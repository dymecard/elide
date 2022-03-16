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
package elide.util.proto

import com.google.protobuf.Descriptors
import com.google.protobuf.Message
import com.google.protobuf.util.JsonFormat
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.concurrent.Immutable
import javax.annotation.concurrent.ThreadSafe


/**
 * Generic loader for protocol buffer records expressed in text, binary, or JSON. Supports hot-pluggable dialects which
 * can be installed downstream.
 *
 * Proto codec objects can be re-used for a given [Message] instance. They are thread-safe and immutable. Built-in
 * dialects use no dependencies; only Protobuf's core toolset.
 *
 * Proto codec instances are themselves usable as basic streams, with support for the [Closeable] and [AutoCloseable]
 * interfaces. When dispatched, these interfaces free held resources. Because proto-codec instances are immutable,
 * closing an instance yields it unusable for continued codec operations. In this case, the codec will throw
 * [IllegalStateException] for all operations that mutability (i.e. registration of types or dialects).
 *
 * @param M Message-type instance we intend to decode.
 * @param defaultDialect Default dialect to use. If none is supplied, [Dialect.BINARY] is used.
 * @param defaultInstance Default proto instance we should use to spawn builders, etc.
 * @param defaultRegistry Default type registry to use. If none is provided, uses an empty registry.
 * @param descriptor Descriptor of the instance we intend to decode.
 * @param dialectMap Custom dialect map to use when resolving codec dialects. Empty default is used if none is supplied.
 */
@Immutable
@ThreadSafe
class Protobuf<M: Message>(
        private val defaultDialect: Dialect,
        private val defaultInstance: M,
        private val defaultRegistry: JsonFormat.TypeRegistry.Builder = JsonFormat.TypeRegistry.newBuilder(),
        private val descriptor: Descriptors.Descriptor = defaultInstance.descriptorForType,
        private val dialectMap: MutableMap<DialectInfo, LoaderDialect<M>> =
                ConcurrentSkipListMap<DialectInfo, LoaderDialect<M>>(),
): Closeable, AutoCloseable, Serializable {
    companion object {
        /** Governs the default codec dialect. */
        val defaultDialect = Dialect.BINARY

        /**
         * Spawn a proto codec specialized to the provided [Message] of type [M]. The default instance of the message
         * should be passed (i.e. [Message.getDefaultInstanceForType]).
         *
         * @param model Default model instance from which to specialize the resulting proto loader.
         * @return Proto loader, specialized to type [M].
         */
        @Suppress("UNCHECKED_CAST")
        @JvmStatic fun <M: Message> forProto(model: M): Protobuf<M> =
                Protobuf(defaultDialect, model.defaultInstanceForType as M)

        /**
         * Spawn a proto codec specialized to the provided [Message] of type [M]. The default instance of the message
         * should be passed (i.e. [Message.getDefaultInstanceForType]).
         *
         * @param model Default model instance from which to specialize the resulting proto loader.
         * @param dialect Default dialect to use for the coder.
         * @return Proto loader, specialized to type [M].
         */
        @JvmStatic fun <M: Message> forProto(model: M, dialect: Dialect): Protobuf<M> =
                Protobuf(dialect, model)

        /**
         * Use a one-shot [Protobuf] with the default [Dialect] to inflate an instance of the [Message] type [M] from
         * the supplied set of [bytes]; use the provided [defaultInstance] to spawn builders.
         *
         * Note: The input data is expected to contain no more and no less than one un-wrapped instance.
         *
         * @param M Type of message we are de-serializing.
         * @param defaultInstance Default message instance to use when acquiring a decoder.
         * @param bytes Raw bytes to de-serialize the instance from.
         * @return Instance of type [M], decoded using the default [Dialect] from the provided [bytes].
         */
        @JvmStatic fun <M: Message> deserialize(defaultInstance: M, bytes: ByteArray): M {
            return ByteArrayInputStream(bytes).use { stream ->
                read(defaultInstance, stream)
            }
        }

        /**
         * Use a one-shot [Protobuf] with the specified [dialect] to inflate an instance of the [Message] type [M]
         * from the supplied set of [bytes]; use the provided [defaultInstance] to spawn builders.
         *
         * Note: The input data is expected to contain no more than, and no less than, one un-wrapped instance.
         *
         * @param M Type of message we are de-serializing.
         * @param defaultInstance Default message instance to use when acquiring a decoder.
         * @param bytes Raw bytes to de-serialize the instance from.
         * @param dialect Known dialect to use when de-serializing the record.
         * @return Instance of type [M], decoded using the specified [dialect] from the provided [bytes].
         */
        @JvmStatic fun <M: Message> deserialize(defaultInstance: M, bytes: ByteArray, dialect: Dialect): M {
            return ByteArrayInputStream(bytes).use { stream ->
                read(defaultInstance, stream, dialect)
            }
        }

        /**
         * Use a one-shot [Protobuf] with the default [Dialect] to inflate an instance of the [Message] type [M] from
         * the supplied [resource] on the classpath; use the provided [defaultInstance] to spawn builders.
         *
         * Note: The input resource is expected to contain no more than, and no less than, one un-wrapped instance.
         *
         * @param M Type of message we are de-serializing.
         * @param defaultInstance Default message instance to use when acquiring a decoder.
         * @param resource Path to the resource within the classpath, starting with `/`.
         * @return Instance of type [M], decoded using the default [Dialect] from the provided [resource].
         * @throws IOException If the resource cannot be located.
         */
        @Throws(IOException::class)
        @JvmStatic fun <M: Message> read(defaultInstance: M, resource: String): M {
            return BufferedInputStream(Protobuf::class.java.getResourceAsStream(resource)).use { buf ->
                read(defaultInstance, buf)
            }
        }

        /**
         * Use a one-shot [Protobuf] with the specified [dialect] to inflate an instance of the [Message] type [M]
         * from the supplied [resource] on the classpath; use the provided [defaultInstance] to spawn builders.
         *
         * Note: The input file is expected to contain no more than, and no less than, one un-wrapped instance.
         *
         * @param M Type of message we are de-serializing.
         * @param defaultInstance Default message instance to use when acquiring a decoder.
         * @param resource Path to the resource within the classpath, starting with `/`.
         * @param dialect Known dialect to use when de-serializing the record.
         * @return Instance of type [M], decoded using the specified [dialect] from the provided [resource].
         * @throws IOException If the resource cannot be located.
         */
        @Throws(IOException::class)
        @JvmStatic fun <M: Message> read(defaultInstance: M, resource: String, dialect: Dialect): M {
            return BufferedInputStream(Objects.requireNonNull(
                Protobuf::class.java.getResourceAsStream(resource),
                "ProtoCodec cannot load `null` classpath resource"
            )).use { buf ->
                read(defaultInstance, buf, dialect)
            }
        }

        /**
         * Use a one-shot [Protobuf] with the default [Dialect] to inflate an instance of the [Message] type [M] from
         * the supplied [file]; use the provided [defaultInstance] to spawn builders.
         *
         * Note: The input file is expected to contain no more than, and no less than, one un-wrapped instance.
         *
         * @param M Type of message we are de-serializing.
         * @param defaultInstance Default message instance to use when acquiring a decoder.
         * @param file File to read and deserialize from.
         * @return Instance of type [M], decoded using the default [Dialect] from the provided [file].
         * @throws FileNotFoundException If the file could not be located.
         * @throws IOException In general if the file cannot be read.
         */
        @Throws(IOException::class, FileNotFoundException::class)
        @JvmStatic fun <M: Message> read(defaultInstance: M, file: File): M {
            return BufferedInputStream(FileInputStream(file)).use { buf ->
                read(defaultInstance, buf)
            }
        }

        /**
         * Use a one-shot [Protobuf] with the specified [dialect] to inflate an instance of the [Message] type [M]
         * from the supplied [file]; use the provided [defaultInstance] to spawn builders.
         *
         * Note: The input file is expected to contain no more than, and no less than, one un-wrapped instance.
         *
         * @param M Type of message we are de-serializing.
         * @param defaultInstance Default message instance to use when acquiring a decoder.
         * @param file File to read and deserialize from.
         * @param dialect Known dialect to use when de-serializing the record.
         * @return Instance of type [M], decoded using the specified [dialect] from the provided [file].
         * @throws FileNotFoundException If the file could not be located.
         * @throws IOException In general if the file cannot be read.
         */
        @Throws(IOException::class, FileNotFoundException::class)
        @JvmStatic fun <M: Message> read(defaultInstance: M, file: File, dialect: Dialect): M {
            return BufferedInputStream(FileInputStream(file)).use { buf ->
                read(defaultInstance, buf, dialect)
            }
        }

        /**
         * Use a one-shot [Protobuf] with the default [Dialect] to inflate an instance of the [Message] type [M] from
         * the supplied [stream]; use the provided [defaultInstance] to spawn builders.
         *
         * Note: The input stream is expected to contain no more than, and no less than, one un-wrapped instance.
         *
         * @param M Type of message we are de-serializing.
         * @param defaultInstance Default message instance to use when acquiring a decoder.
         * @param stream Stream of data to read and decode from.
         * @return Instance of type [M], decoded using the default [Dialect] from the provided [stream].
         */
        @JvmStatic fun <M: Message> read(defaultInstance: M, stream: InputStream): M {
            return forProto(defaultInstance).decode(
                stream,
                defaultDialect
            )
        }

        /**
         * Use a one-shot [Protobuf] with the specified [dialect] to inflate an instance of the [Message] type [M]
         * from the supplied [stream]; use the provided [defaultInstance] to spawn builders.
         *
         * Note: The input stream is expected to contain no more than, and no less than, one un-wrapped instance.
         *
         * @param M Type of message we are de-serializing.
         * @param defaultInstance Default message instance to use when acquiring a decoder.
         * @param stream Stream of data to read and decode from.
         * @return Instance of type [M], decoded using the specified [dialect] from the provided [stream].
         */
        @JvmStatic fun <M: Message> read(defaultInstance: M, stream: InputStream, dialect: Dialect): M {
            return forProto(defaultInstance).decode(
                stream,
                dialect
            )
        }

        /**
         * Use a one-shot [Protobuf] with the default [Dialect] to serialize the provided [instance] into a resulting
         * [ByteArray]; the contents of the resulting array (and encoding) depend on the dialect employed.
         *
         * At the time of this writing, the default dialect is [Dialect.BINARY], and can be accessed at
         * [Protobuf.defaultDialect].
         *
         * @param instance Message instance to serialize using the default dialect.
         * @return Raw bytes resulting from the serialization operation.
         */
        @JvmStatic fun <M: Message> serialize(instance: M): ByteArray {
            return forProto(instance).encode(
                instance,
                defaultDialect
            )
        }

        /**
         * Use a one-shot [Protobuf] with the specified [dialect] to serialize the provided [instance] into a
         * resulting [ByteArray]; the contents of the resulting array (and encoding) depend on the dialect employed.
         *
         * @param instance Message instance to serialize using the specified [dialect].
         * @param dialect Dialect to use for encoding the provided [instance].
         * @return Raw bytes resulting from the serialization operation.
         */
        @JvmStatic fun <M: Message> serialize(instance: M, dialect: Dialect): ByteArray {
            return forProto(instance).encode(
                instance,
                dialect
            )
        }

        /**
         * Use a one-shot [Protobuf] with the default [Dialect] to serialize the provided [instance] into the target
         * [file]; the [file] must be writable, otherwise an [IOException] is thrown. At the time of this writing, the
         * default dialect is [Dialect.BINARY] (the active default can be fetched via [Protobuf.defaultDialect]).
         *
         * Note: No delimiters are written and this interface expects the resulting file to contain exactly one proto
         * record, in un-wrapped form.
         *
         * @param instance Proto instance to write to the specified [file].
         * @param file File to serialize and write the proto instance to.
         */
        @Throws(IOException::class)
        @JvmStatic fun <M: Message> write(instance: M, file: File) {
            BufferedOutputStream(FileOutputStream(file)).use { buf ->
                write(instance, buf)
            }
        }

        /**
         * Use a one-shot [Protobuf] with the default [Dialect] to serialize the provided [instance] into the target
         * [file]; the [file] must be writable, otherwise an [IOException] is thrown. At the time of this writing, the
         * default dialect is [Dialect.BINARY] (the active default can be fetched via [Protobuf.defaultDialect]).
         *
         * Note: No delimiters are written and this interface expects the resulting file to contain exactly one proto
         * record, in un-wrapped form.
         *
         * @param instance Proto instance to write to the specified [file].
         * @param file File to serialize and write the proto instance to.
         * @param dialect Dialect to use for encoding the provided [instance].
         */
        @JvmStatic fun <M: Message> write(instance: M, file: File, dialect: Dialect) {
            BufferedOutputStream(FileOutputStream(file)).use { buf ->
                write(instance, buf, dialect)
            }
        }

        /**
         * Use a one-shot [Protobuf] with the default [Dialect] to serialize the provided [instance] into the target
         * [stream]; the [stream] must be writable, otherwise an [IOException] is thrown. At the time of this writing,
         * the default dialect is [Dialect.BINARY] (the active default can be fetched via [Protobuf.defaultDialect]).
         *
         * Note: No delimiters are written and this interface expects the resulting file to contain exactly one proto
         * record, in un-wrapped form.
         *
         * @param instance Proto instance to write to the specified [stream].
         * @param stream Stream to write the serialized instance to.
         */
        @JvmStatic fun <M: Message> write(instance: M, stream: OutputStream) {
            BufferedOutputStream(stream).use { buf ->
                forProto(instance).encode(
                    instance,
                    buf,
                    defaultDialect
                )
            }
        }

        /**
         * Use a one-shot [Protobuf] with the specified [dialect] to serialize the provided [instance] into the target
         * [stream]; the [stream] must be writable, otherwise an [IOException] is thrown. At the time of this writing,
         * the default dialect is [Dialect.BINARY] (the active default can be fetched via [Protobuf.defaultDialect]).
         *
         * Note: No delimiters are written and this interface expects the resulting file to contain exactly one proto
         * record, in un-wrapped form.
         *
         * @param instance Proto instance to write to the specified [stream].
         * @param stream Stream to write the serialized instance to.
         * @param dialect Dialect to use for encoding the provided [instance].
         */
        @JvmStatic fun <M: Message> write(instance: M, stream: OutputStream, dialect: Dialect) {
            BufferedOutputStream(stream).use { buf ->
                forProto(instance).encode(
                    instance,
                    buf,
                    dialect
                )
            }
        }
    }

    /** Whether the proto-codec is closed. */
    private var _closed = AtomicBoolean(false)

    /** Read-only access to the current closed-status for this proto codec. */
    val closed: Boolean get() = _closed.get()

    /** Exception: Thrown when the requested custom dialect could not be found. */
    class DialectNotFound(info: DialectInfo): IllegalArgumentException(
        "Dialect not found ('${info.name}')"
    )

    /** Specifies structured dialect info, affixed to an enumerated instance. */
    data class DialectInfo(
        /** Name of the dialect. */
        val name: String,

        /** Whether the dialect is built-in or custom. */
        val builtin: Boolean
    ) {
        companion object {
            /** Private access to create built-in dialects. */
            @JvmStatic internal fun forBuiltin(name: String): DialectInfo = DialectInfo(
                name = name,
                builtin = true
            )

            /** Create a custom dialect for decoding protocol buffers, at the supplied [name]. */
            @JvmStatic fun forCustomDialect(name: String): DialectInfo = DialectInfo(
                name = name,
                builtin = false
            )
        }
    }

    /**
     * Enumerates available dialects from which proto records may be loaded.
     *
     * @param info Info/metadata regarding this built-in dialect entry.
     */
    enum class Dialect(val info: DialectInfo) {
        /** Length-prefixed binary protocol buffer encoding. */
        BINARY(DialectInfo.forBuiltin(name = "BINARY")),

        /** Text-based protocol buffer encoding. */
        TEXT(DialectInfo.forBuiltin(name = "TEXT")),

        /** JSON-mapped protocol buffer encoding. */
        JSON(DialectInfo.forBuiltin(name = "TEXT"))
    }

    /** Describes the interface for a hot-pluggable proto loader dialect. */
    interface LoaderDialect<M: Message> {
        /**
         * Provide the enumerated dialect bound to this implementation. The resulting serializable info acts as a spec
         * for the attached implementation and must match the requested dialect at runtime byte-for-byte.
         *
         * @return Enumerated dialect corresponding to this implementation.
         */
        fun dialect(): DialectInfo

        /**
         * Decode a single proto from the provided [stream]; it is assumed that no delimiter is defined, so only a
         * single record may yield from this method.
         *
         * The supplied input stream will already be wrapped in a buffered stream, etc., and so we need only read from
         * the stream to consume data efficiently.
         *
         * @param stream Input stream which we should decode from.
         * @param builder Fresh builder for instance [M], so we can fill it out with data from [stream].
         * @return Instance of [M] as a [Message.Builder], filled out with applicable properties from supplied data.
         */
        fun decode(stream: InputStream, builder: Message.Builder): Message.Builder

        /**
         * Encode a single proto supplied via the [message] of type [M]; once encoded, write to the provided [stream]
         * with resulting data. The stream will be closed for us, accounting for any delimiter settings and other proto
         * records which should be included.
         *
         * The stream does not need to be closed in this method, and output will be buffered for us. All we need to do
         * is handle serialization and write.
         *
         * @param message Message instance which we should encode into the target stream.
         * @param stream Output stream where we should put the message.
         */
        fun encode(message: M, stream: OutputStream)

        /**
         * During the process of serializing a proto in the [encode] method, before any encoding actually occurs, this
         * method is dispatched to "prep" an [instance] of [M] for serialization. The method is expected to return a
         * copy of instance [M] if any changes are made before serializtaion.
         *
         * The default implementation of this method hands back the provided [instance] unchanged. Child classes wishing
         * to override this behavior *must* ensure the type of [M] does not change, unless the [Protobuf] itself is
         * typed directly to [Message].
         *
         * @param instance Instance to prepare for serialization, expected to be of type [M].
         * @return Instance that is ready to serialize.
         */
        fun prepare(instance: M): M = instance

        /**
         * During the process of decoding a proto in the [decode] method, perform any final actions on the resulting
         * [builder] (or validations), before building/sealing/casting an instance of [M]. This method may optionally be
         * overridden by child-classes; the default implementation simply builds and casts.
         *
         * Because of the cast that occurs in this method, [decode] *must not* hand back a [Message] instance other than
         * the expected type [M], unless the [Protobuf] is typed directly to [Message]. Due to the cast that is
         * expected to occur in this method, it is safe and allowed to annotate with `@Suppress("UNCHECKED_CAST")`.
         *
         * @param builder Message builder which we should use to inflate an instance of [M].
         * @return Instance of message [M], built from the supplied [builder].
         */
        @Suppress("UNCHECKED_CAST")
        fun finalize(builder: Message.Builder): M = builder.build() as M

        /**
         * Encode a single proto ([message] of type [M]) using this dialect implementation, and then write it to the
         * provided [stream]. This method is responsible for calling [LoaderDialect.prepare] and [LoaderDialect.encode],
         * which in turn implement dialect-specific typed serialization.
         *
         * No wrapping or stream management is performed by the [ProtoLoaderDialect]. All buffering and cleanup actions
         * occur in the main [Protobuf].
         *
         * @param message Message instance to encode into the target stream.
         * @param stream Output stream where we should write the encoded result.
         */
        fun encodeProto(message: M, stream: OutputStream) {
            this.encode(
                this.prepare(message),
                stream
            )
        }

        /**
         * Decode a single merged [Message.Builder] instance from the supplied [stream] of data using this dialect
         * implementation. This method is responsible for calling [LoaderDialect.decode] and [LoaderDialect.finalize],
         * which in turn implement dialect-specific typed de-serialization.
         *
         * No wrapping or stream management is performed by the [ProtoLoaderDialect]. All buffering and cleanup actions
         * occur in the main [Protobuf].
         *
         * @param stream Input stream of data which we should decode our builder from.
         * @param defaultInstance Default instance from which we should spawn a new builder.
         * @return Merged builder, assembled from our input stream of data.
         */
        fun decodeBuilder(stream: InputStream, defaultInstance: M): Message.Builder {
            return this.decode(
                stream,
                defaultInstance.newBuilderForType()
            )
        }

        /**
         * Decode a single inflated proto-record instance [M], of type [Message], from the supplied [stream] of data
         * using this dialect implementation. This method is responsible for calling [LoaderDialect.decode] and
         * [LoaderDialect.finalize], which in turn implement dialect-specific typed de-serialization.
         *
         * No wrapping or stream management is performed by the [ProtoLoaderDialect]. All buffering and cleanup actions
         * occur in the main [Protobuf].
         *
         * @param stream Input stream of data from which we should decode an instance of [M].
         * @param defaultInstance Default instance from which we should spawn a new builder.
         * @return Inflated proto instance of type [M].
         */
        fun decodeProto(stream: InputStream, defaultInstance: M): M {
            return this.finalize(decodeBuilder(
                stream,
                defaultInstance
            ))
        }
    }

    /**
     * Describes base logic for hot-pluggable dialects.
     *
     * @param dialect Enumerated dialect implemented by a given child class.
     */
    private sealed class ProtoLoaderDialect<M: Message>(private val dialect: Dialect): LoaderDialect<M> {
        /**
         * Force child classes to specify the enumerated dialect they implement.
         *
         * @return Implementing dialect.
         */
        override fun dialect(): DialectInfo = dialect.info
    }

    /** Dialect implementation for decoding length-prefixed binary protocol buffer data. */
    private class ProtoBinaryDialect<M: Message>: ProtoLoaderDialect<M>(dialect = Dialect.BINARY) {
        /** @inheritDoc */
        override fun decode(stream: InputStream, builder: Message.Builder): Message.Builder {
            return builder.mergeFrom(stream)
        }

        /** @inheritDoc */
        override fun encode(message: M, stream: OutputStream) {
            return message.writeTo(stream)
        }
    }

    /** Dialect implementation for decoding proto-text data. */
    private class ProtoTextDialect<M: Message>: ProtoLoaderDialect<M>(dialect = Dialect.TEXT) {
        /** @inheritDoc */
        override fun decode(stream: InputStream, builder: Message.Builder): Message.Builder {
            throw NotImplementedError(
                "Text-based decoding is not available on the JVM."
            )
        }

        /** @inheritDoc */
        override fun encode(message: M, stream: OutputStream) {
            stream.write(message.toString().toByteArray(StandardCharsets.UTF_8))
        }
    }

    /** Dialect implementation for decoding JSON-mapped protocol buffer data. */
    private class ProtoJSONDialect<M: Message>(
        /** Type registry for JSON decoding. */
        registry: JsonFormat.TypeRegistry? = null,

        /** Custom JSON reader to use. */
        reader: JsonFormat.Parser? = null,

        /** Custom JSON printer to use. */
        printer: JsonFormat.Printer? = null
    ): ProtoLoaderDialect<M>(dialect = Dialect.JSON) {
        // Registry for custom types registered with the outer `ProtoCodec`.
        private val jsonTypeRegistry = registry ?: JsonFormat.TypeRegistry
                .getEmptyTypeRegistry()

        // JSON printer.
        private val jsonPrinter = printer ?: JsonFormat.printer()
                .includingDefaultValueFields()
                .omittingInsignificantWhitespace()
                .sortingMapKeys()

        // JSON reader.
        private val jsonReader = reader ?: JsonFormat.parser()
                .ignoringUnknownFields()
                .usingTypeRegistry(jsonTypeRegistry)

        /** @inheritDoc */
        override fun decode(stream: InputStream, builder: Message.Builder): Message.Builder {
            BufferedReader(InputStreamReader(stream)).use { buf ->
                jsonReader.merge(buf, builder)
                return builder
            }
        }

        /** @inheritDoc */
        override fun encode(message: M, stream: OutputStream) {
            stream.write(jsonPrinter.print(
                message
            ).toByteArray(StandardCharsets.UTF_8))
        }
    }

    // Make sure the coder isn't closed before performing a mutable operation.
    private fun <R> mutating(mutator: () -> R): R {
        if (!_closed.get()) throw IllegalStateException(
            "The `ProtoCodec` instance has been closed and cannot be used for new operations."
        )
        return mutator.invoke()
    }

    // Resolve a dialect implementation for the supplied parameters, or fail loudly.
    @Throws(DialectNotFound::class)
    private fun resolveDialectImpl(known: Dialect, custom: DialectInfo? = null): LoaderDialect<M> {
        return when {
            // if any custom dialect is specified, use that first.
            custom != null -> {
                val resolved = dialectMap[custom]
                if (resolved != null) {
                    object: LoaderDialect<M> {
                        override fun dialect(): DialectInfo =
                            custom
                        override fun decode(stream: InputStream, builder: Message.Builder): Message.Builder =
                            resolved.decode(stream, builder)
                        override fun encode(message: M, stream: OutputStream) =
                            resolved.encode(message, stream)
                    }
                } else throw DialectNotFound(custom)
            }

            // if a known dialect is specified, use that. the fallback is set to this anyway when a custom dialect is
            // specified at runtime.
            else -> when (known) {
                Dialect.BINARY -> ProtoBinaryDialect()
                Dialect.TEXT -> ProtoTextDialect()
                Dialect.JSON -> ProtoJSONDialect()
            }
        }
    }

    /**
     * Register a custom dialect with the [Protobuf], using the specified [info] (describing the dialect). If a
     * request surfaces to encode or decode using a matching [DialectInfo], the supplied [dialect] is used. Only one
     * dialect implementation may be registered for a given hashable value of [DialectInfo].
     *
     * @see withCustomDialect for builder-friendly registration.
     * @param info Dialect info describing this custom codec dialect.
     * @param dialect Implementation to register for this dialect.
     * @throws IllegalArgumentException If the specified dialect can't be registered due to an existing [DialectInfo]
     *         which describes the same dialect.
     */
    @Throws(IllegalArgumentException::class)
    fun registerCustomDialect(info: DialectInfo, dialect: LoaderDialect<M>) {
        mutating {
            if (dialectMap.containsKey(info)) throw IllegalArgumentException(
                    "Duplicate dialect for ProtoCodec: '${info.name}'"
            )
            dialectMap[info] = dialect
        }
    }

    /**
     * Register a custom dialect with the [Protobuf], using the specified [info] (describing the dialect). If a
     * request surfaces to encode or decode using a matching [DialectInfo], the supplied [dialect] is used. Only one
     * dialect implementation may be registered for a given hashable value of [DialectInfo].
     *
     * This variant returns self ([Protobuf], specialized as-is) for easy method chaining during construction.
     *
     * @param info Dialect info describing this custom codec dialect.
     * @param dialect Implementation to register for this dialect.
     * @return Instance of [Protobuf], with specialization intact.
     * @throws IllegalArgumentException If the specified dialect can't be registered due to an existing [DialectInfo]
     *         which describes the same dialect.
     */
    @Throws(IllegalArgumentException::class)
    fun withCustomDialect(info: DialectInfo, dialect: LoaderDialect<M>): Protobuf<M> {
        registerCustomDialect(info, dialect)
        return this
    }

    /**
     * Register one or more protocol buffer type(s) to be decoded by the [Protobuf], via the internal type registry.
     * In this case, the proto is registered via its [Descriptors.Descriptor].
     *
     * @see registerType which operates on messages.
     * @param types Types to register with the internal [Protobuf] type library.
     */
    fun registerType(vararg types: Descriptors.Descriptor) {
        mutating {
            defaultRegistry.add(types.asList())
        }
    }

    /**
     * Register one or more protocol buffer type(s) to be decoded by the [Protobuf], via the internal type registry.
     * In this case, the proto is registered via its regular [Message] instance type.
     *
     * @see registerType which operates on descriptors.
     * @param types Message types to register with the internal [Protobuf] type library.
     */
    fun registerType(vararg types: Message) {
        mutating {
            defaultRegistry.add(types.asList().map { it.descriptorForType })
        }
    }

    /**
     * Register one or more protocol buffer type(s) to be decoded by the [Protobuf], via the internal type registry.
     * In this case, the proto is registered via its [Descriptors.Descriptor].
     *
     * This variant returns self ([Protobuf], specialized as-is) for easy method chaining during construction.
     *
     * @see withTypes which operates on messages.
     * @see registerType which operates directly without returning `this`.
     * @param types Types to register with the internal [Protobuf] type library.
     */
    fun withTypes(vararg types: Descriptors.Descriptor): Protobuf<M> {
        registerType(*types)
        return this
    }

    /**
     * Register one or more protocol buffer type(s) to be decoded by the [Protobuf], via the internal type registry.
     * In this case, the proto is registered via its regular [Message] instance type.
     *
     * This variant returns self ([Protobuf], specialized as-is) for easy method chaining during construction.
     *
     * @see withTypes which operates on messages.
     * @see registerType which operates directly without returning `this`.
     * @param types Types to register with the internal [Protobuf] type library.
     */
    fun withTypes(vararg types: Message): Protobuf<M> {
        registerType(*types)
        return this
    }

    /**
     * Decode a single proto instance [M] (inheriting from [Message]) from the provided [input] stream using the
     * [defaultDialect] provided at construction time or defaulted to [Dialect.BINARY] (at the time of this writing --
     * the current default is accessible at [Protobuf.defaultDialect]).
     *
     * The provided stream is expected to have exactly 1 unwrapped instance of the provided proto [M]. Delimited streams
     * must use methods which provide delimiter settings.
     *
     * @param input Input stream to read the proto record from.
     * @return Instance of [M] decoded from the input stream, using the resolved dialect.
     */
    fun decode(input: InputStream): M {
        return decode(
            input,
            defaultDialect
        )
    }

    /**
     * Decode a single proto instance [M] (inheriting from [Message]) from the provided [input] stream, using the known
     * specified [dialect], if provided. If no dialect is provided, the [defaultDialect] is used, which is taken at
     * construction time or defaulted to [Dialect.BINARY] (at the time of this writing -- the current default is
     * accessible at [Protobuf.defaultDialect]).
     *
     * The provided stream is expected to have exactly 1 unwrapped instance of the provided proto [M]. Delimited streams
     * must use methods which provide delimiter settings.
     *
     * @param input Input stream to read the proto record from.
     * @param dialect Known dialect to use when reading from the stream. If no dialect is provided, the default is used.
     * @return Instance of [M] decoded from the input stream, using the resolved dialect.
     */
    fun decode(input: InputStream, dialect: Dialect?): M {
        return resolveDialectImpl(dialect ?: defaultDialect).decodeProto(
            input,
            defaultInstance
        )
    }

    /**
     * Decode a single proto instance [M] (inheriting from [Message]) from the provided [input] stream, using the
     * provided [customDialect].
     *
     * The provided stream is expected to have exactly 1 unwrapped instance of the provided proto [M]. Delimited streams
     * must use methods which provide delimiter settings.
     *
     * @param input Input stream to read the proto record from.
     * @param customDialect Custom dialect specification, with which a dialect is resolved and used.
     * @return Instance of [M] decoded from the input stream, using the resolved dialect.
     * @throws DialectNotFound If the specified [customDialect] was not registered.
     */
    @Throws(DialectNotFound::class)
    fun decode(input: InputStream, customDialect: DialectInfo): M {
        return resolveDialectImpl(known = defaultDialect, custom = customDialect).decodeProto(
            input,
            defaultInstance
        )
    }

    /**
     * Encode a single proto [instance] of type [M] into a [ByteArray], using the [defaultDialect] provided at
     * construction time (or defaulted to [Dialect.BINARY] at the time of this writing -- the current default is
     * accessible at [Protobuf.defaultDialect]).
     *
     * The provided stream will have no more than 1 unwrapped instance of the provided proto [M]. Delimited streams
     * must use methods which provide delimiter settings.
     *
     * @param instance Instance to encode into a raw set of bytes, using the default [Dialect].
     * @return Raw set of bytes resulting from the encoding operation.
     */
    fun encode(instance: M): ByteArray {
        return encode(
            instance,
            defaultDialect
        )
    }

    /**
     * Encode a single proto [instance] of type [M] into a [ByteArray], using the specified known [dialect] if provided;
     * otherwise, if no dialect is provided, fallback to the [defaultDialect] provided at construction time (or
     * defaulted to [Dialect.BINARY] at the time of this writing -- the current default is accessible at
     * [Protobuf.defaultDialect]).
     *
     * The provided stream will have no more than 1 unwrapped instance of the provided proto [M]. Delimited streams
     * must use methods which provide delimiter settings.
     *
     * @param instance Instance to encode into a raw set of bytes, using the resolved [Dialect] ([dialect] or default).
     * @param dialect Specifies a known dialect to use for encoding the [instance].
     * @return Raw set of bytes resulting from the encoding operation.
     */
    fun encode(instance: M, dialect: Dialect?): ByteArray {
        val bytes = ByteArrayOutputStream()
        return bytes.use { out ->
            encode(instance, out, dialect)
            bytes.toByteArray()
        }
    }

    /**
     * Encode a single proto [instance] of type [M] into a [ByteArray], using the specified [customDialect].
     *
     * The provided stream will have no more than 1 unwrapped instance of the provided proto [M]. Delimited streams
     * must use methods which provide delimiter settings.
     *
     * @param instance Instance to encode into a raw set of bytes, using the specified [customDialect].
     * @param customDialect Custom dialect implementation to use.
     * @return Raw set of bytes resulting from the encoding operation.
     * @throws DialectNotFound If the specified [customDialect] was not registered.
     */
    @Throws(DialectNotFound::class)
    fun encode(instance: M, customDialect: DialectInfo? = null): ByteArray {
        val bytes = ByteArrayOutputStream()
        return bytes.use { out ->
            encode(instance, out, customDialect)
            bytes.toByteArray()
        }
    }

    /**
     * Encode a single proto [instance] of type [M] into the target [stream], using the specified known [dialect] if
     * provided; otherwise, if no dialect is provided, fallback to the [defaultDialect] provided at construction time
     * (or defaulted to [Dialect.BINARY] at the time of this writing -- the active default dialect is accessible at
     * [Protobuf.defaultDialect]).
     *
     * The provided stream will have no more than 1 unwrapped instance of the provided proto [M]. Delimited streams
     * must use methods which provide delimiter settings.
     *
     * @param instance Instance to encode into a raw set of bytes, using the resolved [Dialect] ([dialect] or default).
     * @param stream Output stream where we should write the resulting record.
     * @param dialect Specifies a known dialect to use for encoding the [instance].
     */
    fun encode(instance: M, stream: OutputStream, dialect: Dialect? = null) {
        resolveDialectImpl(dialect ?: defaultDialect).encodeProto(
            instance,
            stream
        )
    }

    /**
     * Encode a single proto [instance] of type [M] into the target [stream], using the specified [customDialect].
     *
     * The provided stream will have no more than 1 unwrapped instance of the provided proto [M]. Delimited streams
     * must use methods which provide delimiter settings.
     *
     * @param instance Instance to encode into a raw set of bytes, using the specified [customDialect].
     * @param stream Output stream where we should write the resulting record.
     * @param customDialect Custom dialect implementation to use.
     * @throws DialectNotFound If the specified [customDialect] was not registered.
     */
    @Throws(DialectNotFound::class)
    fun encode(instance: M, stream: OutputStream, customDialect: DialectInfo? = null) {
        resolveDialectImpl(known = defaultDialect, custom = customDialect).encodeProto(
            instance,
            stream
        )
    }

    // -- Interface: `Closeable` -- //

    /** @inheritDoc */
    override fun close() {
        if (!_closed.get()) {
            _closed.compareAndSet(false, true)
            dialectMap.clear()
        }
    }
}
