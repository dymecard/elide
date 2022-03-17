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
import io.lettuce.core.api.StatefulRedisConnection;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.concurrent.ExecutorService;


/**
 * Implementation of an Elide {@link ModelAdapter} which persists records in Redis, or any database which is wire-
 * compatible with Redis.
 *
 * <p>This adapter can use any model codec, and any cache driver, in front of its storage operations. The
 * {@link RedisCache} doesn't win much over Redis-based persistence, in this case, see the in-memory adapter for
 * caching. Queries are not supported by this engine.</p>
 */
public final class RedisAdapter<Key extends Message, Model extends Message> implements ModelAdapter<Key, Model> {
    /** Specifies the format to use. One of `BINARY`, `JSON`, or `TEXT`. */
    private static final EncodingMode FORMAT = EncodingMode.BINARY;

    /** Driver for this Redis adapter. */
    private final @Nonnull RedisDriver<Key, Model> driver;

    /** Codec in use for model serialization/de-serialization activities. */
    private final @Nonnull ModelCodec<Model, EncodedModel, EncodedModel> codec;

    /** Cache to use for model interactions through this adapter (optional). */
    private final @Nonnull Optional<CacheDriver<Key, Model>> cache;

    /** Connection to use when communicating with Redis. */
    private final @Nonnull StatefulRedisConnection<String, EncodedModel> redis;

    /**
     * Private constructor - create a Redis adapter from scratch.
     *
     * @param redis Redis instance to use with this adapter.
     * @param keyInstance Empty instance of the attached model's key.
     * @param codec Model codec to use with this adapter (when serializing/de-serializing instances).
     * @param cache Caching driver to use with this adapter (optional).
     * @param executorService Executor service to use for storage operations.
     */
    @SuppressWarnings("unused")
    private RedisAdapter(@Nonnull StatefulRedisConnection<String, EncodedModel> redis,
                         @Nonnull Key keyInstance,
                         @Nonnull ModelCodec<Model, EncodedModel, EncodedModel> codec,
                         @Nonnull Optional<CacheDriver<Key, Model>> cache,
                         @Nonnull ListeningScheduledExecutorService executorService) {
        this.redis = redis;
        this.cache = cache;
        this.codec = codec;
        this.driver = RedisDriver.acquire(this.redis, codec, executorService);
    }

    /**
     * Acquire an instance of the {@link RedisAdapter}, specialized for the provided empty model instance.
     *
     * <p>An empty instance can easily be acquired for any given model, via {@link Message#getDefaultInstanceForType()}.
     * The instance is used only for builder-spawning and type information. The provided {@link ExecutorService} is used
     * for model codec activities and callback dispatch.</p>
     *
     * @param redis Connection to use when communicating with Redis.
     * @param keyInstance Empty instance of the key type for <pre>instance</pre>.
     * @param instance Empty model instance with which to spawn new builders, and resolve type information.
     * @param executorService Executor to use for callbacks and model codec activities.
     * @param <M> Type of model for which an {@link RedisAdapter} is being requested.
     * @return Instance of a Redis data adapter for the provided model.
     * @throws InvalidModelType If the specified model is not meant to be used for storage.
     */
    public static @Nonnull <K extends Message, M extends Message> RedisAdapter<K, M> acquire(
            @Nonnull StatefulRedisConnection<String, EncodedModel> redis,
            @Nonnull K keyInstance,
            @Nonnull M instance,
            @Nonnull ListeningScheduledExecutorService executorService) throws InvalidModelType {
        return acquire(redis, keyInstance, instance, Optional.empty(), executorService);
    }

    /**
     * Acquire an instance of the {@link RedisAdapter}, specialized for the provided empty model instance, optionally
     * specifying a {@link CacheDriver} to use.
     *
     * <p>An empty instance can easily be acquired for any given model, via {@link Message#getDefaultInstanceForType()}.
     * The instance is used only for builder-spawning and type information. The provided {@link ExecutorService} is used
     * for model codec activities and callback dispatch.</p>
     *
     * <p>If {@link Optional#empty()}</p> is passed as the {@code cache}, no caching will take place. If a valid
     * {@link CacheDriver} instance is provided, it will be used only if {@code options} on a request allow for it
     * (caching defaults to being active).</p>
     *
     * @param redis Connection to use when communicating with Redis.
     * @param keyInstance Empty instance of the key type for <pre>instance</pre>.
     * @param instance Empty model instance with which to spawn new builders, and resolve type information.
     * @param cache Cache driver to use for read-path code in the adapter.
     * @param executorService Executor to use for callbacks and model codec activities.
     * @param <M> Type of model for which an {@link RedisAdapter} is being requested.
     * @return Instance of a Redis data adapter for the provided model.
     * @throws InvalidModelType If the specified model is not meant to be used for storage.
     */
    public static @Nonnull <K extends Message, M extends Message> RedisAdapter<K, M> acquire(
            @Nonnull StatefulRedisConnection<String, EncodedModel> redis,
            @Nonnull K keyInstance,
            @Nonnull M instance,
            @Nonnull Optional<CacheDriver<K, M>> cache,
            @Nonnull ListeningScheduledExecutorService executorService) throws InvalidModelType {
        return new RedisAdapter<>(
            redis,
            keyInstance,
            ProtoModelCodec.forModel(instance, FORMAT),
            cache,
            executorService
        );
    }

    // -- Components -- //
    /** {@inheritDoc} */
    @Override
    public @Nonnull ModelCodec<Model, EncodedModel, EncodedModel> codec() {
        return this.codec;
    }

    /** {@inheritDoc} */
    @Override
    public @Nonnull Optional<CacheDriver<Key, Model>> cache() {
        return this.cache;
    }

    /** {@inheritDoc} */
    @Override
    public @Nonnull RedisDriver<Key, Model> engine() {
        return this.driver;
    }
}