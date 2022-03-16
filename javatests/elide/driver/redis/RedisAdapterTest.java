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
import com.google.common.util.concurrent.MoreExecutors;
import elide.model.EncodedModel;
import elide.model.GenericPersistenceAdapterTest;
import elide.model.PersonRecord.Person;
import elide.model.PersonRecord.PersonKey;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.DynamicTest;
import redis.embedded.RedisServer;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


/** Tests for the {@link RedisAdapter}. */
@MicronautTest
public final class RedisAdapterTest extends GenericPersistenceAdapterTest<RedisAdapter<PersonKey, Person>> {
    private static final ListeningScheduledExecutorService executorService;
    private static StatefulRedisConnection<String, EncodedModel> redisConnection;
    private static final boolean redisAvailable;

    static {
        //noinspection UnstableApiUsage
        executorService = MoreExecutors.listeningDecorator(MoreExecutors.getExitingScheduledExecutorService(
            new ScheduledThreadPoolExecutor(1)
        ));

        boolean localRedisAvailable;
        boolean embeddedRedisAvailable;

        try {
            RedisServer redisServer = new RedisServer(-1);
            //noinspection OptionalGetWithoutIsPresent
            var clientEmbedded = (
               RedisClient.create(String.format("redis://localhost:%s", redisServer.ports().stream().findFirst().get()))
            );
            redisConnection = clientEmbedded.connect(RedisEncodedModelCodec.acquire());
            embeddedRedisAvailable = true;
        } catch (Throwable err) {
            embeddedRedisAvailable = false;
        }

        if (!embeddedRedisAvailable) {
            // try local redis instead
            try {
                var clientLocal = (
                    RedisClient.create("redis://localhost:6379")
                );
                redisConnection = clientLocal.connect(RedisEncodedModelCodec.acquire());
                localRedisAvailable = true;
            } catch (Throwable err) {
                localRedisAvailable = false;
            }
        } else {
            localRedisAvailable = false;
        }

        // we can test against embedded or local, whatever is available
        redisAvailable = embeddedRedisAvailable || localRedisAvailable;
    }

    /** {@inheritDoc} */
    @Override
    protected @Nonnull RedisAdapter<PersonKey, Person> adapter() {
        return RedisAdapter.acquire(
            redisConnection,
            PersonKey.getDefaultInstance(),
            Person.getDefaultInstance(),
            executorService
        );
    }

    /** {@inheritDoc} */
    @Override
    protected @Nonnull List<DynamicTest> supportedDriverTests() {
        if (redisAvailable) {
            return super.supportedDriverTests();
        } else {
            return new ArrayList<>();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void acquireDriver() {
        assumeTrue(redisAvailable, "can only acquire driver if embedded redis is available");
        assertNotNull(adapter(), "should not get `null` for adapter acquire");
    }
}
