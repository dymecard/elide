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
package elide.driver.spanner;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;


/** Specifies settings for the Spanner driver and data storage implementation. */
@Immutable
@ThreadSafe
public interface SpannerDriverSettings {
    /** Concrete hard-coded driver defaults. */
    final class DefaultSettings {
        private DefaultSettings() { /* disallow construction */ }

        /** Default value: Whether to preserve proto field names (`true`) or use JSON names (`false`, default). */
        public static final Boolean DEFAULT_PRESERVE_FIELD_NAMES = false;

        /** Default value: Whether to generate Spanner style names with initial capitals. */
        public static final Boolean DEFAULT_CAPITALIZED_NAMES = true;

        /** Default value: Whether to treat enumeration instances as numbers (`true`) or strings (`false`, default). */
        public static final Boolean DEFAULT_ENUMS_AS_NUMBERS = false;

        /** Default value: Whether to perform runtime deserialization checks (`true`, default) or not (`false`). */
        public static final Boolean DEFAULT_CHECK_EXPECTED_TYPES = true;

        /** Default value: Whether to write seemingly empty Boolean values as `false` (`true`, by default). */
        public static final Boolean DEFAULT_WRITE_EMPTY_BOOLS_AS_FALSE = true;

        /** Default size for `STRING` or `BYTES` column fields with no explicit setting. */
        private static final int DEFAULT_COLUMN_SIZE = 2048;

        /** Default native JSON field type enablement state. */
        private static final boolean NATIVE_JSON_TYPE = false;
    }

    /** Default set of configured settings for the Spanner driver. */
    SpannerDriverSettings DEFAULTS = new SpannerDriverSettings() {};

    /** @return Whether to preserve proto field names (`true`) or use JSON names (defaults to `false`). */
    default @Nonnull Boolean preserveFieldNames() {
        return DefaultSettings.DEFAULT_PRESERVE_FIELD_NAMES;
    }

    /** @return Whether to generate Spanner style names with initial capitals (i.e. `Name` instead of `name`). */
    default @Nonnull Boolean defaultCapitalizedNames() {
        return DefaultSettings.DEFAULT_CAPITALIZED_NAMES;
    }

    /** @return Whether to treat enumeration instances as numbers (`true`) or strings (defaults to `false`). */
    default @Nonnull Boolean enumsAsNumbers() {
        return DefaultSettings.DEFAULT_ENUMS_AS_NUMBERS;
    }

    /** @return Whether to perform runtime deserialization checks (defaults to `true`). */
    default @Nonnull Boolean checkExpectedTypes() {
        return DefaultSettings.DEFAULT_CHECK_EXPECTED_TYPES;
    }

    /** @return Whether to write seemingly empty Boolean values as `false` (defaults to `true`). */
    default @Nonnull Boolean writeEmptyBoolsAsFalse() {
        return DefaultSettings.DEFAULT_WRITE_EMPTY_BOOLS_AS_FALSE;
    }

    /** @return Default size to use for `STRING` or `BYTES` columns that don't otherwise specify a size. */
    default int defaultColumnSize() {
        return DefaultSettings.DEFAULT_COLUMN_SIZE;
    }

    /** @return Whether to enable experimental support for native JSON columns (defaults to `false`). */
    default @Nonnull Boolean experimentalNativeJsonType() {
        return DefaultSettings.NATIVE_JSON_TYPE;
    }
}
