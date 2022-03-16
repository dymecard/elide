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
package elide.util.json

import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.blackbird.BlackbirdModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import com.google.protobuf.Message


/** System-level Jackson configuration, and ObjectMapper util. */
@Suppress("MemberVisibilityCanBePrivate")
object JSON {
    /** @return Jackson module which can handle Protocol Buffer serialization. */
    @JvmStatic internal fun protoModule(): SimpleModule {
        val proto = SimpleModule(
            "ProtobufMessageModule",
            Version(
                1, 0, 0,
                null, null, null
            )
        )
        proto.addSerializer(
            Message::class.java,
            JSONProtoSerializer()
        )
        return proto
    }

    /** @return Shorthand alias for [objectMapper]. */
    @JvmStatic fun mapper(): ObjectMapper = objectMapper()

    /** @return Pre-fab object mapper for JSON operations. */
    @JvmStatic fun objectMapper(): ObjectMapper {
        return objectMapper(ObjectMapper())
    }

    /** @return Configured [mapper] with modules and settings. */
    @Suppress("DEPRECATION")
    @JvmStatic fun objectMapper(mapper: ObjectMapper): ObjectMapper {
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
        mapper.configure(MapperFeature.IGNORE_DUPLICATE_MODULE_REGISTRATIONS, true)
        mapper.registerModule(ParameterNamesModule())
        mapper.registerModule(Jdk8Module())
        mapper.registerModule(JavaTimeModule())
        mapper.registerModule(protoModule())
        if (System.getProperty("elide.engine", System.getenv("elide.engine")) != "native") {
            mapper.registerModule(BlackbirdModule())
        }
        return mapper
    }
}
