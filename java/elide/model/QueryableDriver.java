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
package elide.model;


import com.google.protobuf.Message;
import elide.runtime.jvm.ReactiveFuture;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.stream.Stream;


/**
 *  Extends the built-in {@link DatabaseDriver} interface with additional support for queries of type {@link Query},
 *  specified by the implementing driver.
 */
public interface QueryableDriver<Key extends Message, Model extends Message, ReadRecord, WriteRecord, Query>
   extends DatabaseDriver<Key, Model, ReadRecord, WriteRecord>, QueryableBase<Key, Model, Query> {
    /**
     * Execute the provided query asynchronously, producing a future which resolves to a lazy stream of keys-only
     * results, and applying the specified options.
     *
     * <p>Exceptions are returned as failed/rejected futures.</p>
     *
     * @param query Query to execute and return key results for.
     * @param options Options to apply to the query execution.
     * @return Stream of results of type {@link Key}.
     * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
     * @throws PersistenceException If an unexpected failure occurs, of any kind, while fetching the requested resource.
     * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
     */
    @Nonnull ReactiveFuture<Stream<Key>> queryKeysAsync(@Nonnull Query query, @Nullable QueryOptions options);

    /**
     * Execute the provided query asynchronously, producing a future which resolves to a lazy stream of decoded record
     * results, and applying the specified options.
     *
     * <p>Exceptions are returned as failed/rejected futures.</p>
     *
     * @param query Query to execute and return key results for.
     * @param options Options to apply to the query execution.
     * @return Stream of results of type {@link Model}.
     * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
     * @throws PersistenceException If an unexpected failure occurs, of any kind, while fetching the requested resource.
     * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
     */
    @Nonnull ReactiveFuture<Stream<Model>> queryAsync(@Nonnull Query query, @Nullable QueryOptions options);
}
