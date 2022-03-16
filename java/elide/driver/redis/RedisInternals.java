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
package elide.driver.redis;

import elide.util.Hex;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static java.lang.String.format;


/** Utility class with internals used by both the {@link RedisCache} and {@link RedisDriver}. */
final class RedisInternals {
    /** Key hash algorithm to apply. */
    private static final String hashAlgorithm = "SHA-256";

    /** Version of layout of keyed data in Redis. */
    private static final String dataVersion = "v1a";

    /** Static prefix to apply to all keys. */
    private static final String staticTag = "_elide_::model:";

    /** Prefix bytes to apply to all keys in Redis. */
    private static final byte[] prefixBytes = format(
            "%s%s", staticTag, dataVersion
    ).getBytes(StandardCharsets.UTF_8);

    private RedisInternals() { /* disallow construction */ }

    /** @return `true` if the given `result` (from a Redis `SET` call) is considered successful. */
    static boolean checkSetResult(String result) {
        return (
            "OK".equals(result)
        );
    }

    /** @return Hashed bytes of the provided raw key data, with our configured Redis key hash. */
    private static byte[] hashKeyBytes(byte[] data) {
        try {
            return MessageDigest.getInstance(hashAlgorithm).digest(data);
        } catch (NoSuchAlgorithmException nse) {
            throw new IllegalStateException(nse);
        }
    }

    /** @return Raw encoded bytes of a Redis key where `key` (of an integer value) should be locate-able. */
    private static byte[] encodeNumericKey(byte type, long key) {
        var keySubject = ByteBuffer
                .allocate(9)  // (`long`=8)+(`byte`=1)=9
                .putLong(key)
                .array();

        var target = new byte[prefixBytes.length + 1 /* byte `type */ + keySubject.length];
        System.arraycopy(prefixBytes, 0, target, 0, prefixBytes.length);
        target[prefixBytes.length + 1] = type;
        System.arraycopy(keySubject, 0, target, prefixBytes.length, keySubject.length);
        return target;
    }

    /** @return Raw encoded bytes of a Redis key where `key` (of a string value) should be locate-able. */
    private static byte[] encodeStringKey(byte type, String key) {
        var keySubject = key.getBytes(StandardCharsets.UTF_8);
        var target = new byte[prefixBytes.length + 1 /* byte `type` */ + keySubject.length];
        System.arraycopy(prefixBytes, 0, target, 0, prefixBytes.length);
        target[prefixBytes.length + 1] = type;
        System.arraycopy(keySubject, 0, target, prefixBytes.length + 1, keySubject.length);
        return hashKeyBytes(target);
    }

    /** @return Type-checked and encoded bytes for the requested key ID. */
    static byte[] encodeKey(byte type, Object id) {
        if (id.getClass().isAssignableFrom(String.class) || id instanceof String) {
            // it's a string key.
            return encodeStringKey(type, (String)id);
        } else if (
                id.getClass().isAssignableFrom(Integer.class) ||
                        id instanceof Integer ||
                        id.getClass().equals(Integer.TYPE) ||
                        id.getClass().isAssignableFrom(Long.class) ||
                        id instanceof Long ||
                        id.getClass().equals(Long.TYPE)) {
            // it's a numeric key.
            return encodeNumericKey(type, (long)id);
        } else {
            // failed to type-check or resolve compatible type for key.
            throw new IllegalArgumentException(format(
                    "Failed to resolve key type for class %s",
                    id.getClass().getName()
            ));
        }
    }

    /** @return Hex-encoded final key string for use with Redis in persistent contexts. */
    static String encodeKeyHex(Object id) {
        return Hex.bytesToHex(encodeKey((byte)'p', id));
    }

    /** @return Hex-encoded final key string for use with Redis in ephemeral contexts. */
    static String encodeCacheKey(Object id) {
        return Hex.bytesToHex(encodeKey((byte)'c', id));
    }
}
