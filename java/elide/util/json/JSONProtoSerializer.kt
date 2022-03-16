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

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.google.protobuf.Message
import com.google.protobuf.util.JsonFormat
import java.io.IOException


/**
 * Serializer which is capable of equipping Jackson with the ability to seamlessly JSON-encode protocol buffer [Message]
 * instances.
 *
 * The developer typically does not need to interact with this class; it is installed automatically at runtime by the
 * Jackson tooling built into Elide, and employed for any [Message]-based object during serialization.
 */
internal class JSONProtoSerializer: JsonSerializer<Message>() {
    private val printer = JsonFormat.printer()
            .sortingMapKeys()
            .omittingInsignificantWhitespace()

    /** @inheritDoc */
    @Throws(IOException::class)
    override fun serialize(message: Message, gen: JsonGenerator, serializers: SerializerProvider) =
        gen.writeRawValue(printer.print(message))
}
