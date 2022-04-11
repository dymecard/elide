/*
 * Copyright © 2022, The Elide Framework Authors. All rights reserved.
 *
 * The Gust/Elide framework and tools, and all associated source or object computer code, except where otherwise noted,
 * are licensed under the Zero Prosperity license, which is enclosed in this repository, in the file LICENSE.txt. Use of
 * this code in object or source form requires and implies consent and agreement to that license in principle and
 * practice. Source or object code not listing this header, or unless specified otherwise, remain the property of
 * Elide LLC and its suppliers, if any. The intellectual and technical concepts contained herein are proprietary to
 * Elide LLC and its suppliers and may be covered by U.S. and Foreign Patents, or patents in process, and are protected
 * by trade secret and copyright law. Dissemination of this information, or reproduction of this material, in any form,
 * is strictly forbidden except in adherence with assigned license requirements.
 */
package elide.model

import com.google.cloud.firestore.DocumentReference
import com.google.protobuf.ByteString
import com.google.protobuf.Descriptors
import com.google.protobuf.Descriptors.FieldDescriptor.Type
import com.google.protobuf.Message
import com.google.protobuf.Timestamp
import elide.model.ModelDeserializer.DeserializationError
import elide.runtime.jvm.Logging
import tools.elide.core.Datamodel
import tools.elide.core.FieldPersistenceOptions
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.function.Function
import javax.annotation.Nonnull
import tools.elide.core.FieldType as CoreFieldType


/**
 * Specifies a deserializer which is capable of converting generic Java [Map] objects (expected to have
 * [String] keys) into arbitrary [Message] types.
 *
 * @param <Model> Model record type which this serializer is responsible for converting.
 */
open class ObjectModelDeserializer<Model: Message> protected constructor(
  /** Default model instance to spawn builders from. */
  private val defaultInstance: Model,

  /** Prefix to use when de-serializing database references. */
  private val referencePrefix: String): ModelDeserializer<Map<String, *>, Model> {
  companion object {
    /** Private logging pipe. */
    private val logging = Logging.logger(ObjectModelDeserializer::class.java)

    /**
     * Return an object model deserializer tailored to the parameterized model specified with `M`, with the specified
     * deserialization settings.
     *
     * @param <M> Model type to acquire an object model serializer for.
     * @param instance Default instance to generate the de-serializer from.
     * @param referencePrefix Prefix to apply to all de-serialized database references.
     * @return Deserializer, customized to the specified type.
     */
    @JvmStatic
    @Suppress("MemberVisibilityCanBePrivate")
    fun <M: Message> withSettings(instance: M, referencePrefix: String = ""): ObjectModelDeserializer<M> {
      return ObjectModelDeserializer(instance, referencePrefix)
    }

    /**
     * Return an object model de-serializer tailored to the parameterized model specified with `M`, with default
     * selections for deserialization settings.
     *
     * @param <M> Model type to acquire an object model deserializer for.
     * @param instance Default instance to generate the de-serializer from.
     * @param referencePrefix Prefix to apply to all de-serialized database references.
     * @return Deserializer, customized to the specified type.
     */
    @JvmStatic
    fun <M: Message> defaultInstance(instance: M, referencePrefix: String = ""): ObjectModelDeserializer<M> {
      return withSettings(instance, referencePrefix)
    }
  }

  /**
   * Utility function to convert Google Cloud's specific timestamp type into a temporal instant from OCP. This function
   * must live in this module, and not in common with the `InstantFactory`, to avoid adding a dependency on common for
   * Google's Cloud commons for Java.
   *
   * @param ts
   * @returns
   */
  private fun instantFromCloudTimestamp(ts: com.google.cloud.Timestamp): Timestamp {
    return Timestamp.newBuilder()
      .setSeconds(ts.seconds)
      .setNanos(ts.nanos)
      .build()
  }

  /**
   * Set a field value on a given builder, depending on the field's declared type (provided by Protobuf via field
   * descriptors). "Simple" values are defined as items that are un-repeated and composed solely of native scalar types
   * defined in the Protobuf `proto3` spec.
   *
   * @param type
   * @param field
   * @param builder
   * @param dataValue
   * @throws DeserializationError
   */
  @Throws(DeserializationError::class)
  private fun <B: Message.Builder> setSimpleField(type: Type,
                                                  field: Descriptors.FieldDescriptor,
                                                  builder: B,
                                                  dataValue: Any) {
    // this is only for un-repeated simple values
    if (field.isRepeated)
      throw DeserializationError("Cannot set repeated fields as simple values.")

    // only operate on fields with a value
    when (type) {
      // for many types, we can just splice directly
      Type.BOOL,
      Type.INT32, Type.INT64,
      Type.SFIXED32, Type.SFIXED64,
      Type.SINT32, Type.SINT64,
      Type.FIXED64, Type.FIXED32 ->
        builder.setField(field, dataValue)

      // strings may need to deal with special types
      Type.STRING -> when (dataValue) {
        is DocumentReference -> builder.setField(field, "$referencePrefix${dataValue.path}")
        is String -> builder.setField(field, dataValue)
        else -> throw IllegalArgumentException(
            "Unrecognized `string` data value: '$dataValue' for field '${field.fullName}'")
      }

      // serialize a precision number type
      Type.DOUBLE, Type.FLOAT -> when (dataValue) {
        is Int -> builder.setField(field, dataValue.toFloat())
        is Long -> builder.setField(field, dataValue.toFloat())
        is String -> builder.setField(field, dataValue.toFloat())
        is Double -> builder.setField(field, dataValue.toFloat())
        else -> {
          logging.warn("Unable to serialize number precise numeric field: '${field.name}'.")
          builder.setField(field, dataValue)
        }
      }

      // serialize with care taken for longs
      Type.UINT32, Type.UINT64 -> when (dataValue) {
        is Long -> builder.setField(field, dataValue.toInt())
        is Double -> builder.setField(field, dataValue.toInt())
        is Float -> builder.setField(field, dataValue.toInt())
        else -> builder.setField(field, dataValue)
      }

      // decode from base64 if it's not already raw
      Type.BYTES -> if (dataValue is String) {
        // it's probably base64 encoded
        val bytes: ByteArray = Base64.getDecoder().decode(dataValue.toByteArray(StandardCharsets.UTF_8))
        val bytestring: ByteString = ByteString.copyFrom(bytes)
        builder.setField(field, bytestring)
      } else {
        builder.setField(field, dataValue)
      }

      Type.ENUM -> {
        val enumType = field.enumType ?:
        throw DeserializationError("Unable to resolve enum without attached type, for field '${field.name}' " +
          "on entity '${builder.descriptorForType.name}'.")

        // resolve enum type for this field
        // decode from either string or numeric reference
        val enumValue = when (dataValue) {
          // resolve by name?
          is String -> enumType.findValueByName(dataValue)

          // resolve by ID number?
          is Int, is Long, is Double -> enumType.findValueByNumber(dataValue as Int)

          else ->
            // unable to resolve enum type
            throw DeserializationError("Unable to resolve enum type from raw value for field '${field.name}' " +
              "on entity '${builder.descriptorForType.name}'.")
        } ?: throw DeserializationError("Unable to resolve enum value for field '${field.name}' " +
          "on entity '${builder.descriptorForType.name}'.")

        // we should have an enum value now
        builder.setField(field, enumValue)
      }

      Type.GROUP, Type.MESSAGE ->
        throw DeserializationError("Cannot set sub-message types as simple values.")
    }
  }

  /**
   * Repeated enums are a special case, because they may be specially-encoded as either a list of strings (or numbers),
   * or a map of strings to boolean or integer values.
   *
   * @param descriptor
   * @param field
   * @param builder
   * @param dataList
   * @throws DeserializationError
   */
  private fun <B: Message.Builder> setRepeatedEnum(descriptor: Descriptors.Descriptor,
                                                   field: Descriptors.FieldDescriptor,
                                                   builder: B,
                                                   dataList: Any) {
    val enumType = field.enumType ?: throw DeserializationError("Unable to deserialize repeated enum with missing " +
      "enum type, at field '${field.name}' on entity '${descriptor.name}'.")
    val enumValues: ArrayList<Descriptors.EnumValueDescriptor>

    when (dataList) {
      // a list of enum values can be strings or numbers
      is List<*> -> {
        enumValues = ArrayList(dataList.size)

        var pos = 0
        for (rawEnumValue in dataList) {
          pos += 1

          when (rawEnumValue) {
            // handle as the enum name if it's a string
            is String -> enumValues.add(enumType.findValueByName(rawEnumValue.uppercase()))

            // handle as a numeric ID of the enum value
            is Int, is Double, is Long -> enumValues.add(enumType.findValueByNumber(rawEnumValue as Int))

            // reject unrecognized types
            else ->
              throw DeserializationError("Unable to decode repeated enum value in position '$pos' on field " +
                "'${field.name}' on entity '${descriptor.name}'.")
          }
        }
      }

      // a list of map values should be strings mapped to boolean or integer values
      is Map<*, *> -> {
        enumValues = ArrayList(dataList.size)
        for (rawEnumKey in dataList.keys) {
          if (rawEnumKey !is String)
            throw DeserializationError("Repeated enum values expressed as a map must have string keys, at field " +
              "'${field.name}' on entity '${descriptor.name}'.")

          // string value should be the enum type
          enumValues.add(enumType.findValueByName(rawEnumKey.uppercase()))
        }
      }

      // unrecognized types should fail
      else -> throw DeserializationError("Failed to identify type of repeated enum data at field '${field.name}' " +
        "on entity '${descriptor.name}'.")
    }

    // fill it in on the builder
    builder.setField(field, enumValues)
  }

  /**
   * Fill in a repeated field on a message. This involves iterating over the list of values, preparing a list of our own
   * filtered and decoded values, and then setting it via the builder.
   *
   * @param type
   * @param field
   * @param builder
   * @param dataList
   * @throws DeserializationError
   */
  private fun <B: Message.Builder> setRepeatedField(type: Type,
                                                    field: Descriptors.FieldDescriptor,
                                                    builder: B,
                                                    dataList: List<*>) {
    val targetValues = ArrayList<Any>(dataList.size)

    if (type == Type.ENUM)
      // should never get here
      throw DeserializationError("Repeated enums cannot be decoded as regular fields.")

    // we have a list of raw values
    for (item in dataList) {
      // skip nulls
      item ?: continue

      // decode a simple type
      targetValues.add(item)
    }
    builder.setField(field, targetValues)
  }

  /**
   * Translate the provided database [ref] into a recursively-constructed key structure matching the provided
   * [defaultInstance]. If the specified instance has a parent, it is recursively decoded into the resulting instance
   * [K].
   *
   * @param K Concrete key type we are inflating.
   * @param ref Reference to turn into a key.
   * @param defaultInstance Default instance of the key we are inflating.
   * @return Constructed key builder.
   */
  @Suppress("UNCHECKED_CAST")
  private fun <K: Message> refToKey(ref: DocumentReference, defaultInstance: K): K {
    // references look like this:
    // `projects/<project-id>/databases/(default)/documents/*[collection/doc]`
    //
    // so, for example:
    // `projects/<project-id>/databases/(default)/documents/users/abc123`
    //
    // and, with a parent:
    // `projects/<project-id>/databases/(default)/documents/users/abc123/things/abc123`
    //
    val path = ref.path
    val segments = path.split("/")
    var i = 0
    val segmentPairs: ArrayList<Pair<String, String>> = ArrayList()
    if ((segments.size - 5) % 2 > 0) throw DeserializationError(
        "Invalid segment count in path reference: '$path'"
    )

    keySegments@while (i < segments.size) {
      when (segments[i]) {
        // skip the next segment, it's the project name.
        "projects" -> {
          i += 2
          continue@keySegments
        }

        // skip the next segment, it's the database name.
        "databases" -> {
          i += 2
          continue@keySegments
        }

        // it's the last segment of the database qualifier, so begin handling on the next step.
        "documents" -> {
          i += 1
          continue@keySegments
        }

        else -> {
          // quick sanity check
          if (i < 5 && path.startsWith("projects/")) throw DeserializationError(
              "Failed to deserialize unqualified reference '$path'"
          )

          // add the next pair
          segmentPairs.add(
            segments[i] to segments[i + 1]
          )
          i += 2  // advance to the next pair
        }
      }
    }

    // resolve the base key builder
    val parentStack: ArrayList<Message.Builder> = ArrayList(segmentPairs.size)
    val leafDescriptor = defaultInstance.descriptorForType
    var baseDescriptor = leafDescriptor
    var contextBuilder = defaultInstance.newBuilderForType()
    val segmentCount = segmentPairs.size
    var stackI = 1

    // process each non-leaf to create a key
    while ((segmentCount - (1 + stackI)) > -1) {
      val pair = segmentPairs[segmentCount - (1 + stackI)]

      // look for a parent field on the base.
      val parentField = ModelMetadata.annotatedField(
          baseDescriptor,
          Datamodel.field,
          false,
          Optional.of(Function { field: FieldPersistenceOptions -> field.type == tools.elide.core.FieldType.PARENT })
      )

      // if we don't find a parent, it's an error
      if (!parentField.isPresent) throw DeserializationError(
        "Failed to locate expected parent for key segment '${pair.first}/${pair.second}' for key path '$path'"
      )

      val currentParent = parentField.get()
      val subBuilder = contextBuilder.newBuilderForField(parentField.get().field)
      contextBuilder = subBuilder
      parentStack.add(subBuilder)
      baseDescriptor = currentParent.field.messageType
      stackI += 1
    }

    // begin building the key, starting at the root
    var pairI = 0
    var builderI = parentStack.size - 1
    var baseBuilder: Message.Builder = parentStack[builderI]
    var baseParent: Message? = null

    while (pairI < (segmentPairs.size - 1)) {
      // pull the segment pair and corresponding field
      val (collection, documentId) = segmentPairs[pairI]

      // resolve the ID field on the base, splice it in
      val idField = ModelMetadata.annotatedField(
            baseDescriptor,
            Datamodel.field,
            false,
            Optional.of(Function { field: FieldPersistenceOptions -> field.type == tools.elide.core.FieldType.ID })
      )
      if (idField.isEmpty) throw DeserializationError(
        "Failed to locate expected ID field for key segment '$collection/$documentId' for key path '$path'"
      )

      // splice it into the builder
      ModelMetadata.spliceBuilder<Message.Builder, String>(
          baseBuilder,
          idField.get(),
          Optional.of(documentId)
      )

      if (builderI == 0) {
        // we're done building
        pairI += 1
        baseParent = baseBuilder.build()
      } else {
        pairI += 1
        builderI -= 1
        val nextBuilder = parentStack[builderI]
        val nextDescriptor = nextBuilder.descriptorForType

        // resolve the parent field on the next base, splice it in
        val nextParent = ModelMetadata.annotatedField(
            nextDescriptor,
            Datamodel.field,
            false,
            Optional.of(Function { field: FieldPersistenceOptions -> field.type == tools.elide.core.FieldType.PARENT })
        )
        if (nextParent.isEmpty) throw DeserializationError(
          "Failed to locate expected parent field for key segment '$collection/$documentId' for key path '$path'"
        )

        // on the next builder, make sure to add the `PARENT` first, if applicable, before proceeding to build-in the ID
        // of the next key.
        ModelMetadata.spliceBuilder<Message.Builder, Message>(
            nextBuilder,
            nextParent.get(),
            Optional.of(baseBuilder.build())
        )
        baseDescriptor = nextDescriptor
        baseBuilder = nextBuilder
      }
    }

    // it's time to start building the final concrete key
    val leafBuilder = defaultInstance.newBuilderForType()
    val (_, leafId) = segmentPairs.last()

    // locate the ID field and splice it in
    ModelMetadata.spliceIdBuilder<Message.Builder, String>(
        leafBuilder,
        Optional.of(leafId)
    )

    // locate the leaf parent field
    if (baseParent != null) {
      val leafParent = ModelMetadata.annotatedField(
          leafDescriptor,
          Datamodel.field,
          false,
          Optional.of(Function { field: FieldPersistenceOptions -> field.type == tools.elide.core.FieldType.PARENT })
      )
      if (leafParent.isEmpty) throw DeserializationError(
          "Failed to locate expected leaf parent field for path '$path'"
      )
      ModelMetadata.spliceBuilder<Message.Builder, Message>(
          leafBuilder,
          leafParent.get(),
          Optional.of(baseParent)
      )
    }

    // return the built key
    return leafBuilder.build() as K
  }

  /**
   * Load a raw set of mapped data, from underlying storage, into a message builder so that it may be constructed into
   * a concrete Protobuf representation.
   *
   * @param builder
   * @param data
   * @returns
   * @throws DeserializationError
   */
  @Throws(DeserializationError::class)
  @Suppress("UNCHECKED_CAST", "DuplicatedCode")
  fun <B: Message.Builder> build(builder: B, data: Map<String, *>): B {
    // setup a new builder
    if (data.isEmpty()) return builder  // it's empty, return a default proto

    // otherwise parse the fields
    val descriptor = builder.descriptorForType
    fields@for (field in descriptor.fields) {
      // every field must have a type
      val type = field.type ?: throw DeserializationError("Cannot inflate a field without a type.")
      val fieldProto = field.toProto()
      val fieldOptions = if (fieldProto.hasOptions() && fieldProto.options.hasExtension(Datamodel.opts)) {
        fieldProto.options.getExtension(Datamodel.opts)
      } else {
        null
      }
      val persistenceOptions = if (fieldProto.hasOptions() && fieldProto.options.hasExtension(Datamodel.field)) {
        fieldProto.options.getExtension(Datamodel.field)
      } else {
        null
      }

      // skip ephemeral fields
      if (fieldOptions?.ephemeral == true) continue@fields

      if (!field.isRepeated) {
        if (data.containsKey(field.name)) {
          // extract value, make sure it's not null
          val dataValue = data[field.name] ?: continue@fields
          when (type) {
            Type.GROUP, Type.MESSAGE -> {
              // it's a singular sub-message field. recurse.
              val subBuilder = builder.newBuilderForField(field)
                ?: throw DeserializationError("Unable to resolve message type for property '${field.name}' " +
                  "on entity '${descriptor.name}'")
              if (dataValue is Map<*, *>) {
                // prepare the sub-builder, then attach to the top-level field
                this.build(subBuilder, dataValue as Map<String, Any>)
                builder.setField(field, subBuilder.build())
              } else {
                // special case: consider timestamps
                if (field.messageType.fullName == "google.protobuf.Timestamp") {
                  when (dataValue.javaClass.name) {
                    // it's a Google Cloud well-known-value (`Timestamp`), for which we have a converter
                    "com.google.cloud.Timestamp" ->
                      builder.setField(field, instantFromCloudTimestamp(
                        dataValue as com.google.cloud.Timestamp))


                    // it's a Protobuf well-known-value (`Timestamp`), for which we need no conversion
                    "google.protobuf.Timestamp" ->
                      builder.setField(field, dataValue as Timestamp)


                    // if it's numeric, it should be a millisecond-resolution Unix epoch timestamp
                    else -> builder.setField(field, when (dataValue) {
                      is Int, is Double, is Long -> Timestamp.newBuilder()
                        .setSeconds(dataValue as Long)

                      // all other types should fail
                      else ->
                        throw DeserializationError("Failed to decode timestamp/instant type. Could not determine " +
                          "native type at field '${field.name}' on entity '${descriptor.name}'.")
                    }.build())
                  }
                } else {
                  // see if it is annotated as a parent, or a reference, which would explain this state
                  if (persistenceOptions != null && (
                      persistenceOptions.type == CoreFieldType.REFERENCE ||
                        persistenceOptions.type == CoreFieldType.PARENT)) {
                    // it should be a reference type
                    if (dataValue.javaClass.name != DocumentReference::class.java.name)
                      throw DeserializationError("Found non-reference value for reference property.")
                    val ref = dataValue as DocumentReference
                    if (ref.parent.parent == null) {
                      // has no parent, so it's easy. set up a new instance of the key.
                      val keyInstance = builder.newBuilderForField(field) ?:
                        throw DeserializationError("Unable to resolve builder for key reference instance.")

                      // find the field to inflate the ID into
                      var idField: Descriptors.FieldDescriptor? = null
                      keyFields@for (keyField in keyInstance.descriptorForType.fields) {
                        if (keyField.options.hasExtension(Datamodel.field)) {
                          val persistenceOptsForSubfield = keyField.options.getExtension(Datamodel.field)
                          if (persistenceOptsForSubfield.type == CoreFieldType.ID) {
                            // found it
                            idField = keyField
                            break@keyFields
                          }
                        }
                      }
                      if (idField == null)
                        throw DeserializationError("Could not resolve key structure ID field for reference inflate.")

                      // fill the builder at that field with the trimmed value
                      keyInstance.setField(idField, ref.id)
                      builder.setField(field, keyInstance.build())

                    } else {
                      val key = refToKey(ref, builder.newBuilderForField(field).defaultInstanceForType)
                      builder.setField(field, key)
                    }
                  } else {
                    // dunno why it's not an object
                    throw DeserializationError("Found non-map value where sub-message value was expected, " +
                      "in field '${field.name}' on entity '${descriptor.name}'.")
                  }
                }
              }
            }

            else -> setSimpleField(type, field, builder, dataValue)
          }
        } else if (fieldOptions?.concrete == true && (type == Type.MESSAGE || type == Type.GROUP)) {
          // if it's a concrete record, examine the field name, against the containing one-of name. if the containing
          // one-of name (concrete synthesized name) and the property name match here, it's supposed to be a concrete
          // type, flattened into the map we're currently de-serializing.
          val concreteType = data[ObjectModelSerializer.concreteTypeProperty] as? String
          if (concreteType != null && concreteType.lowercase().trim() == field.jsonName.lowercase().trim()) {
            // we found the concrete type expressed by this generic entity. now we need to decode it as if it's the
            // underlying concrete type specified.
            val subBuilder = builder.newBuilderForField(field)
            subBuilder ?: throw DeserializationError("Unable to resolve message type for concrete property " +
              "'${field.name}' on entity '${descriptor.name}'")
            this.build(subBuilder, data)
            builder.setField(field, subBuilder.build())
          }
        } else if (fieldOptions?.required == true) {
          throw DeserializationError("Unable to resolve required field '${field.name}' on message " +
            "'${field.containingType.fullName}'.")
        }
      } else {
        // field is repeated: try to grab a list of values, decode for each one
        val dataList = data[field.name] ?: continue@fields
        if (type == Type.ENUM) {
          // handle special case: repeated enums
          setRepeatedEnum(descriptor, field, builder, dataList)
        } else {
          // only operate on lists with values
          if (dataList is List<*> && dataList.isNotEmpty()) {
            when (type) {
              Type.GROUP, Type.MESSAGE -> {
                val submessageList: ArrayList<Message> = ArrayList(dataList.size)

                // make a new list of decoded messages
                var pos = 0
                for (subObj in dataList) {
                  pos += 1

                  if (subObj is Map<*, *>) {
                    // reset field for next round
                    val subBuilder = builder.newBuilderForField(field) ?:
                    throw DeserializationError("Unable to resolve builder for field '${field.name}' on " +
                      "entity '${descriptor.name}'.")
                    this.build(subBuilder, subObj as Map<String, Any>)
                    submessageList.add(subBuilder.build())

                  } else {
                    throw DeserializationError("Cannot identify type for message in repeated field " +
                      "'${field.name}' at position '$pos' on entity " +
                      "'${descriptor.name}'.")
                  }
                }

                if (submessageList.isNotEmpty())
                  builder.setField(field, submessageList)
              }
              else ->
                // set as regular repeated field
                setRepeatedField(type, field, builder, dataList)
            }
          }
        }
      }
    }
    return builder
  }

  /** @inheritDoc */
  @Nonnull
  @Suppress("UNCHECKED_CAST")
  @Throws(ModelInflateException::class)
  override fun inflate(@Nonnull input: Map<String, *>): Model {
    val builder = defaultInstance.newBuilderForType()
    build(builder, input)
    return builder.build() as Model
  }
}
