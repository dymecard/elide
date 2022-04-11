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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Defines the generic interface supplied for querying via both {@link DatabaseDriver} and {@link DatabaseAdapter}
 * implementations; not meant for end-consumption.
 */
@SuppressWarnings("unused")
interface QueryableBase<Key extends Message, Model extends Message, Query> {
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
    @Nonnull
    ReactiveFuture<Stream<Key>> queryKeysAsync(@Nonnull Query query, @Nullable QueryOptions options);

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

    /**
     * Execute the provided query, producing a lazy stream of decoded and type-checked decoded record results, and
     * applying the specified options.
     *
     * <p>Because this method is synchronous, exceptions are thrown. For safer or more performant options, take a look
     * at the async and safe versions of this method.</p>
     *
     * @param query Query to execute and return key results for.
     * @param options Options to apply to the query execution.
     * @return Stream of results of type {@link Model}.
     * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
     * @throws PersistenceException If an unexpected failure occurs, of any kind, while fetching the requested resource.
     * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
     */
    @Nonnull Stream<Model> query(@Nonnull Query query, @Nullable QueryOptions options) throws PersistenceException;

    /**
     * Execute the provided query, producing a lazy stream of decoded and type-checked keys-only results, and applying
     * the specified options.
     *
     * <p>Because this method is synchronous, exceptions are thrown. For safer or more performant options, take a look
     * at the async and safe versions of this method.</p>
     *
     * @param query Query to execute and return key results for.
     * @param options Options to apply to the query execution.
     * @return Stream of results of type {@link Key}.
     * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
     * @throws PersistenceException If an unexpected failure occurs, of any kind, while fetching the requested resource.
     * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
     */
    @Nonnull Stream<Key> queryKeys(@Nonnull Query query, @Nullable QueryOptions options) throws PersistenceException;

    /**
     * Execute the provided query, producing a lazy stream of keys-only results; this method uses a default set of
     * sensible query options, which can be overridden via other method variants.
     *
     * <p>Because this method is synchronous, exceptions are thrown. For safer or more performant options, take a look
     * at the async and safe versions of this method.</p>
     *
     * @param query Query to execute and return key results for.
     * @return Stream of results of type {@link Key}.
     * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
     * @throws PersistenceException If an unexpected failure occurs, of any kind, while fetching the requested resource.
     * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
     */
    default @Nonnull Stream<Key> queryKeys(@Nonnull Query query) throws PersistenceException {
        return this.queryKeys(query, QueryOptions.DEFAULTS);
    }

    /**
     * Synchronously execute the provided query, producing a lazy stream of keys-only results, and applying the
     * specified options, as applicable.
     *
     * <p>Because this method is synchronous, exceptions are thrown. For safer or more performant options, take a look
     * at the async and safe versions of this method.</p>
     *
     * @param query Query to execute and return key results for.
     * @param options Options to apply to the query execution.
     * @return Stream of results of type {@link Key}.
     * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
     * @throws PersistenceException If an unexpected failure occurs, of any kind, while fetching the requested resource.
     * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
     */
    default @Nonnull
    List<Key> queryKeysSync(
            @Nonnull Query query, @Nullable QueryOptions options) throws PersistenceException {
        return this.queryKeys(query, options).collect(Collectors.toList());
    }

    /**
     * Synchronously execute the provided query, producing a lazy stream of keys-only results; this method uses a
     * default set of sensible query options, which can be overridden via other method variants.
     *
     * <p>Because this method is synchronous, exceptions are thrown. For safer or more performant options, take a look
     * at the async and safe versions of this method.</p>
     *
     * @param query Query to execute and return key results for.
     * @return Stream of results of type {@link Key}.
     * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
     * @throws PersistenceException If an unexpected failure occurs, of any kind, while fetching the requested resource.
     * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
     */
    default @Nonnull List<Key> queryKeysSync(@Nonnull Query query) throws PersistenceException {
        return this.queryKeysSync(query, QueryOptions.DEFAULTS);
    }

    /**
     * Execute the provided query, producing a lazy stream of decoded record results; this method uses a default set of
     * sensible query options, which can be overridden via other method variants.
     *
     * <p>Because this method is synchronous, exceptions are thrown. For safer or more performant options, take a look
     * at the async and safe versions of this method.</p>
     *
     * @param query Query to execute and return key results for.
     * @return Stream of results of type {@link Model}.
     * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
     * @throws PersistenceException If an unexpected failure occurs, of any kind, while fetching the requested resource.
     * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
     */
    default @Nonnull Stream<Model> query(@Nonnull Query query) throws PersistenceException {
        return this.query(query, QueryOptions.DEFAULTS);
    }

    /**
     * Synchronously execute the provided query, producing a lazy stream of decoded record results, and applying the
     * specified options, as applicable.
     *
     * <p>Because this method is synchronous, exceptions are thrown. For safer or more performant options, take a look
     * at the async and safe versions of this method.</p>
     *
     * @param query Query to execute and return key results for.
     * @param options Options to apply to the query execution.
     * @return Stream of results of type {@link Model}.
     * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
     * @throws PersistenceException If an unexpected failure occurs, of any kind, while fetching the requested resource.
     * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
     */
    default @Nonnull List<Model> querySync(
            @Nonnull Query query, @Nullable QueryOptions options) throws PersistenceException {
        return this.query(query, options).collect(Collectors.toList());
    }

    /**
     * Synchronously execute the provided query, producing a lazy stream of decoded record results; this method uses a
     * default set of sensible query options, which can be overridden via other method variants.
     *
     * <p>Because this method is synchronous, exceptions are thrown. For safer or more performant options, take a look
     * at the async and safe versions of this method.</p>
     *
     * @param query Query to execute and return key results for.
     * @return Stream of results of type {@link Model}.
     * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
     * @throws PersistenceException If an unexpected failure occurs, of any kind, while fetching the requested resource.
     * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
     */
    default @Nonnull List<Model> querySync(@Nonnull Query query) throws PersistenceException {
        return this.querySync(query, QueryOptions.DEFAULTS);
    }
}
