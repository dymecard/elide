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

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.protobuf.Message;
import elide.model.*;
import elide.runtime.jvm.ReactiveFuture;
import io.lettuce.core.api.StatefulRedisConnection;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Optional;

import static elide.driver.redis.RedisInternals.*;
import static elide.model.ModelMetadata.*;


/**
 * Defines a {@link CacheDriver} backed by Redis, or any database which is wire-compatible with Redis.
 *
 * <p>Cache options may be adjusted based on the operation being memoized, using the {@link CacheOptions} interface,
 * which is supported by various other higher-order options interfaces (i.e. {@link FetchOptions}).</p>
 *
 * @param <K> Type of key used with the cache and model.
 * @param <M> Type of model supported by this cache facade.
 */
@ThreadSafe
public final class RedisCache<K extends Message, M extends Message> implements CacheDriver<K, M> {
    // Connection to use with Redis.
    private final StatefulRedisConnection<String, EncodedModel> redis;

    // Default instance of the model, from which we should spawn builders.
    private final M defaultInstance;

    /**
     * Construct a Redis model object cache from scratch.
     *
     * @param redis Connection to use when communicating with Redis.
     */
    private RedisCache(@Nonnull StatefulRedisConnection<String, EncodedModel> redis,
                       M defaultInstance) {
        this.redis = redis;
        this.defaultInstance = defaultInstance;
    }

    /**
     * Acquire an instance of the Redis-backed caching driver, generalized to support the provided key type {@code K}
     * and model instance type {@code M}.
     *
     * @param <K> Generic type for the key associated with model type {@code M}.
     * @param <M> Generic model type managed by this cache.
     * @param redis Redis connection to use for caching traffic.
     * @param defaultInstance Default model instance, from which we should spawn builders.
     * @return Instance of the acquired cache engine.
     */
    static @Nonnull <K extends Message, M extends Message> RedisCache<K, M> acquire(
            StatefulRedisConnection<String, EncodedModel> redis,
            M defaultInstance) {
        return new RedisCache<>(
            redis,
            defaultInstance
        );
    }

    /** {@inheritDoc} */
    @Override
    public @Nonnull ReactiveFuture put(@Nonnull Message key,
                                       @Nonnull Message model,
                                       @Nonnull ListeningScheduledExecutorService executor) {
        final Object id = (id(key)
            .orElseThrow(() -> new IllegalArgumentException("Cannot add to cache with empty key.")));
        return ReactiveFuture.wrap(executor.submit(() -> redis.sync().set(
            encodeCacheKey(id),
            EncodedModel.from(model)
        ), executor));
    }

    /** {@inheritDoc} */
    @Override
    public @Nonnull ReactiveFuture<Optional<M>> fetch(@Nonnull K key,
                                                      @Nonnull FetchOptions options,
                                                      @Nonnull ListeningScheduledExecutorService executor) {
        final Object id = (id(key).orElseThrow(() -> new IllegalArgumentException("Cannot fetch empty key.")));
        return ReactiveFuture.wrap(options.executorService().orElse(executor).submit(() -> {
            EncodedModel cached = redis.sync().get(encodeCacheKey(id));
            return cached == null ? Optional.empty() : Optional.of(
                cached.inflate(defaultInstance)
            );
        }), executor);
    }

    /** {@inheritDoc} */
    @Override
    public @Nonnull ReactiveFuture evict(@Nonnull K key, @Nonnull ListeningScheduledExecutorService executor) {
        final Object id = (id(key).orElseThrow(() -> new IllegalArgumentException("Cannot expire with empty key.")));
        return ReactiveFuture.wrap(executor.submit(() -> redis.sync().del(encodeCacheKey(id))), executor);
    }

    /** {@inheritDoc} */
    @Override
    public @Nonnull ReactiveFuture flush(@Nonnull ListeningScheduledExecutorService executor) {
        return ReactiveFuture.wrap(executor.submit(() ->
            redis.sync().flushall()
        ), executor);
    }
}
