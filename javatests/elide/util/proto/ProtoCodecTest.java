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
package elide.util.proto;

import com.google.protobuf.Message;
import elide.model.PersonRecord.Person;
import elide.model.PersonRecord.PersonKey;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;


/** Tests for the {@link Protobuf} codec tools. */
@MicronautTest
public class ProtoCodecTest {
    // sample proto record
    static Person sampleProfile = Person.newBuilder()
        .setKey(PersonKey.newBuilder().setId("abc123"))
        .setName("John Doe")
        .build();

    /** Make sure we can construct/acquire a coder in various ways. */
    @Test void testConstruct() {
        // should be able to get a vanilla coder
        assertNotNull(
            Protobuf.codecFor(Person.getDefaultInstance()),
            "should be able to acquire basic proto codec with default dialect"
        );
        assertNotNull(
            Protobuf.codecFor(Person.getDefaultInstance(), Protobuf.Dialect.BINARY),
            "should be able to acquire basic proto codec with explicit BINARY dialect"
        );
        assertNotNull(
            Protobuf.codecFor(Person.getDefaultInstance(), Protobuf.Dialect.TEXT),
            "should be able to acquire basic proto codec with explicit TEXT dialect"
        );
        assertNotNull(
            Protobuf.codecFor(Person.getDefaultInstance(), Protobuf.Dialect.JSON),
            "should be able to acquire basic proto codec with explicit JSON dialect"
        );
    }

    private <M extends Message> void sampleInstanceIntegrityTest(
            M message, Protobuf.Dialect dialect, boolean supportsDecoding) {
        var codec = Protobuf.codecFor(message, dialect);
        assertNotNull(codec, format("should not get `null` for codec with dialect `%s`", dialect.name()));

        // try encoding a model.
        var encoded = codec.encode(message);
        assertNotNull(encoded, format("should not get `null` for encoded proto of dialect `%s`", dialect.name()));
        assertNotEquals(0, encoded.length, format(
                "should not get zero-length result for encoded proto of dialect `%s`", dialect.name()));

        // try encoding a second time. should be deterministic.
        var encoded2 = codec.encode(message);
        assertNotNull(encoded2, format(
                "should not get `null` for 2nd run of encoded proto of dialect `%s`", dialect.name()));
        assertNotEquals(0, encoded2.length, format(
                "should not get zero-length result for 2nd run of encoded proto of dialect `%s`", dialect.name()));
        assertArrayEquals(
            encoded,
            encoded2,
            format("serialization for dialect %s should be deterministic", dialect.name())
        );

        if (supportsDecoding) {
            // try decoding the model.
            var decoded = codec.decode(new ByteArrayInputStream(encoded));
            assertNotNull(decoded, format(
                    "should not get `null` for `decode` result with dialect `%s`", dialect.name()));

            // try decoding the model again.
            var decoded2 = codec.decode(new ByteArrayInputStream(encoded));
            assertNotNull(decoded2, format(
                    "should not get `null` for 2nd run of `decode` result with dialect `%s`", dialect.name()));

            var encoded3 = codec.encode(decoded);
            assertNotNull(encoded3, format(
                    "should not get `null` for re-encode of `decode` result with dialect `%s`", dialect.name()));
            assertNotEquals(0, encoded3.length, format(
                    "should not get zero-length result for re-encode of decoded dialect `%s`", dialect.name()));

            var encoded4 = codec.encode(decoded2);
            assertNotNull(encoded4, format(
                    "should not get zero-length result for 2nd re-encode of decoded dialect `%s`", dialect.name()));
            assertNotEquals(0, encoded4.length, format(
                    "should not get zero-length result for re-encode of 2nd decoded dialect `%s`", dialect.name()));

            assertArrayEquals(
                encoded,
                encoded3,
                format("re-serialization for dialect %s should be deterministic", dialect.name())
            );
            assertArrayEquals(
                encoded3,
                encoded4,
                format("2nd re-serialization for dialect %s should be deterministic", dialect.name())
            );

            assertThat(decoded).isEqualTo(message);
            assertThat(decoded2).isEqualTo(message);
        }
    }

    private <M extends Message> void sampleInstanceIntegrityTest(M message, Protobuf.Dialect dialect) {
        sampleInstanceIntegrityTest(message, dialect, true);
    }

    /** Conformance test for mode=`BINARY`. */
    @Test void testProtoCodecBinary() {
        sampleInstanceIntegrityTest(
            sampleProfile,
            Protobuf.Dialect.BINARY
        );
    }

    /** Conformance test for mode=`TEXT`. */
    @Test void testProtoCodecText() {
        sampleInstanceIntegrityTest(
            sampleProfile,
            Protobuf.Dialect.TEXT,
            false
        );
    }

    /** Conformance test for mode=`JSON`. */
    @Test void testProtoCodecJSON() {
        sampleInstanceIntegrityTest(
            sampleProfile,
            Protobuf.Dialect.JSON
        );
    }

    /** Data sample from profiles service */
    @Test void testProtoCodecReadResource() throws IOException {
        var loaded = Protobuf.read(
            Person.getDefaultInstance(),
            "/elide/util/proto/mock-proto.json",
            Protobuf.Dialect.JSON
        );
        assertNotNull(
            loaded,
            "should not get `null` result loading from JSON proto resource on classpath"
        );
    }
}
