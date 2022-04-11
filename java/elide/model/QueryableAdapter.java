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
 * Extends the base {@link DatabaseAdapter} interface with model query features, based on the concrete `Query` type
 * defined/provided by a given adapter.
 *
 * @param <Key> Type of key used to uniquely address models.
 * @param <Model> Message type which this database adapter is handling.
 * @param <ReadRecord> Intermediate read record.
 * @param <WriteRecord> Intermediate write record.
 */
public interface QueryableAdapter<Key extends Message, Model extends Message, ReadRecord, WriteRecord, Query>
    extends DatabaseAdapter<Key, Model, ReadRecord, WriteRecord>, QueryableBase<Key, Model, Query> {
    /** {@inheritDoc} */
    @Nonnull QueryableDriver<Key, Model, ReadRecord, WriteRecord, Query> engine();

    /** {@inheritDoc} */
    default @Nonnull Stream<Model> query(@Nonnull Query query, @Nullable QueryOptions options)
            throws PersistenceException {
        return this.engine().query(query, options);
    }

    /** {@inheritDoc} */
    default @Nonnull Stream<Key> queryKeys(@Nonnull Query query, @Nullable QueryOptions options)
            throws PersistenceException {
        return this.engine().queryKeys(query, options);
    }

    /** {@inheritDoc} */
    default @Nonnull ReactiveFuture<Stream<Key>> queryKeysAsync(@Nonnull Query query, @Nullable QueryOptions options) {
        return this.engine().queryKeysAsync(query, options);
    }

    /** {@inheritDoc} */
    default @Nonnull ReactiveFuture<Stream<Model>> queryAsync(@Nonnull Query query, @Nullable QueryOptions options) {
        return this.engine().queryAsync(query, options);
    }
}
