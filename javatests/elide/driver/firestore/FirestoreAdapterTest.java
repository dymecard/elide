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
package elide.driver.firestore;


import com.google.api.gax.core.NoCredentialsProvider;
import com.google.cloud.NoCredentials;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.v1.stub.FirestoreStubSettings;
import com.google.cloud.grpc.GrpcTransportOptions;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import elide.model.GenericPersistenceAdapterTest;
import elide.model.PersonRecord.Person;
import elide.model.PersonRecord.PersonKey;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.FirestoreEmulatorContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;


/** Tests for the {@link FirestoreAdapter}. */
@Testcontainers
public final class FirestoreAdapterTest extends GenericPersistenceAdapterTest<FirestoreAdapter<PersonKey, Person>> {
    private static ListeningScheduledExecutorService executorService;
    private static FirestoreAdapter<PersonKey, Person> personAdapter;
    private static final String firestoreVersion = System.getProperty(
        "e2e.firestoreVersion", "349.0.0-emulators"
    );

    @Container
    public FirestoreEmulatorContainer firestore = new FirestoreEmulatorContainer(
        DockerImageName.parse("gcr.io/google.com/cloudsdktool/cloud-sdk:" + firestoreVersion)
    ).withNetworkAliases("firestore");

    @BeforeEach
    void initExecutor() {
        executorService = MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(3));
        var creds = NoCredentialsProvider.create();

        var stubSettings = FirestoreStubSettings.newBuilder()
                .setEndpoint(firestore.getEmulatorEndpoint())
                .setCredentialsProvider(creds);

        personAdapter = FirestoreAdapter.acquire(
                FirestoreOptions.newBuilder()
                        .setProjectId("test-project")
                        .setHost(firestore.getEmulatorEndpoint())
                        .setEmulatorHost(firestore.getEmulatorEndpoint())
                        .setCredentials(NoCredentials.getInstance())
                        .setCredentialsProvider(NoCredentialsProvider.create()),
                stubSettings.getTransportChannelProvider()
                        .withEndpoint(firestore.getEmulatorEndpoint())
                        .withCredentials(NoCredentials.getInstance()),
                stubSettings.getCredentialsProvider(),
                GrpcTransportOptions.newBuilder().build(),
                executorService,
                PersonKey.getDefaultInstance(),
                Person.getDefaultInstance()
        );
    }

    @AfterAll
    static void shutdownExecutor() throws InterruptedException {
        executorService.shutdownNow();
        executorService.awaitTermination(5, TimeUnit.SECONDS);
        executorService = null;
        personAdapter = null;
    }

    /** {@inheritDoc} */
    @Override
    protected @Nonnull Optional<List<String>> unsupportedDriverTests() {
        return Optional.of(Stream.concat(
            super.unsupportedDriverTests().orElse(Collections.emptyList()).stream(), Stream.of(
                "storeEntityUpdateNotFound",
                "storeEntityCollision"
            )
        ).collect(Collectors.toList()));
    }

    /** {@inheritDoc} */
    @Override
    protected @Nonnull FirestoreAdapter<PersonKey, Person> adapter() {
        return personAdapter;
    }

    /** {@inheritDoc} */
    @Override
    protected void acquireDriver() {
        assertNotNull(personAdapter, "should not get `null` for adapter acquire");
    }
}