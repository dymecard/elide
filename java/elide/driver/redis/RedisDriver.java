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
import elide.runtime.jvm.Logging;
import elide.runtime.jvm.ReactiveFuture;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import org.slf4j.Logger;
import tools.elide.core.DatapointType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

import static elide.driver.redis.RedisInternals.*;
import static elide.model.ModelMetadata.*;
import static java.lang.String.format;


/**
 * Persistence driver for Elide applications which works with Redis, or any database which is wire-compatible with
 * Redis, via Lettuce, a Redis adapter for the JVM; support is used in cooperation with Micronaut's Redis layer.
 *
 * @param <Model> Model/message type which we are storing with this driver.
 */
public final class RedisDriver<Key extends Message, Model extends Message> implements PersistenceDriver<Key, Model> {
    /** Private logging pipe. */
    private static final Logger logging = Logging.logger(RedisDriver.class);

    /** Codec to use for model serialization/de-serialization. */
    private final @Nonnull ModelCodec<Model, EncodedModel, EncodedModel> codec;

    /** Executor service to use for storage calls. */
    private final @Nonnull ListeningScheduledExecutorService executorService;

    /** Connection to use when communicating with Redis. */
    private final @Nonnull StatefulRedisConnection<String, EncodedModel> redis;

    /**
     * Construct a new Redis driver from scratch. This constructor is private to force use of static factory methods
     * also defined on this class.
     *
     * @param redis Redis instance to use with this driver.
     * @param codec Codec to use when serializing and de-serializing models with this driver.
     * @param executorService Executor service to run against.
     */
    private RedisDriver(@Nonnull StatefulRedisConnection<String, EncodedModel> redis,
                        @Nonnull ModelCodec<Model, EncodedModel, EncodedModel> codec,
                        @Nonnull ListeningScheduledExecutorService executorService) {
        this.redis = redis;
        this.codec = codec;
        this.executorService = executorService;
    }

    /**
     * Acquire an instance of the Redis driver for the provided model type and builder.
     *
     * <p>It is generally recommended to acquire an instance of this driver through the adapter instead. This can be
     * accomplished via creation of a {@link RedisAdapter}, followed by {@link RedisAdapter#engine()}.</p>
     *
     * @param <K> Key type to specify for the attached model type.
     * @param <M> Model/message type for which we should return a Redis storage driver.
     * @param redis Redis instance to use with this adapter.
     * @param codec Codec to use when serializing and de-serializing models with this driver.
     * @param executorService Executor service to use for storage calls.
     * @return Redis-backed driver instance created for the specified message type.
     */
    static @Nonnull <K extends Message, M extends Message> RedisDriver<K, M> acquire(
            @Nonnull StatefulRedisConnection<String, EncodedModel> redis,
            @Nonnull ModelCodec<M, EncodedModel, EncodedModel> codec,
            @Nonnull ListeningScheduledExecutorService executorService) {
        return new RedisDriver<>(
            redis,
            codec,
            executorService
        );
    }

    // -- Getters -- //
    /** {@inheritDoc} */
    @Override
    public @Nonnull ModelCodec<Model, EncodedModel, EncodedModel> codec() {
        return this.codec;
    }

    /** {@inheritDoc} */
    @Override
    public @Nonnull ListeningScheduledExecutorService executorService() {
        return this.executorService;
    }

    // -- API: Fetch -- //
    /** {@inheritDoc} */
    @Override
    public @Nonnull ReactiveFuture<Optional<Model>> retrieve(final @Nonnull Key key,
                                                             final @Nonnull FetchOptions options) {
        Objects.requireNonNull(key, "Cannot fetch model with `null` for key.");
        Objects.requireNonNull(options, "Cannot fetch model without `options`.");
        enforceRole(key, DatapointType.OBJECT_KEY);
        final var id = id(key)
            .orElseThrow(() -> new IllegalArgumentException("Cannot fetch model with empty key."));

        if (logging.isDebugEnabled())
            logging.debug(format("Retrieving model at ID '%s' from Redis", id));
        if (logging.isTraceEnabled())
            logging.trace(format("Began async task to retrieve model at ID '%s' from Redis", id));

        String targetKey = encodeKeyHex(id);
        return ReactiveFuture.wrap(this.executorService.submit(() -> {
            // fetch the model from Redis
            var data = Optional.ofNullable(
                redis.sync().get(targetKey)
            );
            if (data.isPresent()) {
                if (logging.isTraceEnabled())
                    logging.trace(format("Model found at ID '%s'. Sending to deserializer...", id));

                // deserialize record
                var deserialized = this.codec.deserialize(data.get());

                if (logging.isDebugEnabled())
                    logging.debug(format(
                        "Found and deserialized model at ID '%s'. Record follows:\n%s", id, deserialized));
                if (logging.isInfoEnabled())
                    logging.info(format("Retrieved record at ID '%s' from Redis", id));

                // we found encoded data at the provided key. inflate it with the codec.
                return Optional.of(spliceKey(
                    applyMask(deserialized, options),
                    Optional.of(key)
                ));
            } else {
                if (logging.isWarnEnabled())
                    logging.warn(format("Model not found at ID '%s'.", id));

                // the model was not found.
                return Optional.empty();
            }
        }), options.executorService().orElse(this.executorService));
    }

    // -- API: Persist -- //
    /** {@inheritDoc} */
    @Override
    public @Nonnull ReactiveFuture<Model> persist(final @Nullable Key key,
                                                  final @Nonnull Model model,
                                                  final @Nonnull WriteOptions options) {
        Objects.requireNonNull(model, "Cannot persist `null` model to Redis.");
        Objects.requireNonNull(options, "Cannot persist model to Redis without `options`.");
        if (key != null) enforceRole(key, DatapointType.OBJECT_KEY);

        // resolve target key, and then write mode
        final @Nonnull Key targetKey = key != null ? key : generateKey(model);

        //noinspection OptionalGetWithoutIsPresent
        final @Nonnull Object targetId = id(targetKey).get();

        if (logging.isDebugEnabled())
            logging.debug(format("Persisting model at ID '%s' using Redis", targetId));

        String target = encodeKeyHex(targetId);
        return ReactiveFuture.wrap(this.executorService.submit(() -> {
            // serialize the model before writing
            var serialized = codec.serialize(model);

            WriteOptions.WriteDisposition writeMode = (
                    key == null ? WriteOptions.WriteDisposition.MUST_NOT_EXIST : options.writeMode()
                            .orElse(WriteOptions.WriteDisposition.BLIND));

            if (logging.isTraceEnabled())
                logging.trace(format(
                    "Began async task to write model at ID '%s' to Redis. Write disposition: '%s'.",
                    targetId,
                    writeMode.name()));

            // enforce write mode
            boolean success = false;
            switch (writeMode) {
                case MUST_NOT_EXIST:
                    success = checkSetResult(redis.sync().set(
                        target,
                        serialized,
                        SetArgs.Builder.nx()
                    ));
                    break;
                case MUST_EXIST:
                    success = checkSetResult(redis.sync().set(
                            target,
                            serialized,
                            SetArgs.Builder.xx()
                    ));
                    break;

                case BLIND:
                    success = checkSetResult(redis.sync().set(
                        target,
                        serialized
                    ));
                    break;
            }
            if (!success) {
                logging.error(format("Redis write failure: key collision or rejection at ID '%s'.", targetId));
                throw new ModelWriteConflict(targetId, model, writeMode);
            }
            if (logging.isTraceEnabled())
                logging.trace(format(
                    "No conflict failure encountered, model was written at ID '%s'.",
                    targetId));

            var rval = ModelMetadata.<Model, Key>spliceKey(model, Optional.of(targetKey));
            if (logging.isInfoEnabled())
                logging.info(format(
                    "Wrote record to Redis at ID '%s'.",
                    targetId));
            if (logging.isDebugEnabled())
                logging.debug(format(
                    "Returning written model at ID '%s' after write to Redis. Record follows:\n%s",
                    targetId,
                    rval));

            return rval;
        }), options.executorService().orElse(this.executorService));
    }

    // -- API: Delete -- //
    /** {@inheritDoc} */
    @Override
    public @Nonnull ReactiveFuture<Key> delete(@Nonnull Key key, @Nonnull DeleteOptions options) {
        Objects.requireNonNull(key, "Cannot delete `null` key.");
        Objects.requireNonNull(options, "Cannot delete model without `options`.");
        ModelMetadata.enforceRole(key, DatapointType.OBJECT_KEY);

        final @Nonnull Object targetId = id(key)
                .orElseThrow(() -> new IllegalStateException("Cannot delete record with empty key/ID."));

        if (logging.isDebugEnabled())
            logging.debug(format("Deleting model at key '%s' from Redis.", targetId));

        String target = encodeKeyHex(targetId);
        return ReactiveFuture.wrap(this.executorService.submit(() -> {
            if (logging.isTraceEnabled())
                logging.trace(format("Began async task to delete model at ID '%s' from Redis.", targetId));

            // perform the delete
            redis.sync().del(
                target
            );
            if (logging.isInfoEnabled())
                logging.info(format("Model at ID '%s' deleted from Redis.", targetId));
            return key;
        }));
    }
}
