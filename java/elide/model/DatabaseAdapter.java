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

import javax.annotation.Nonnull;


/**
 * Extends the standard {@link ModelAdapter} interface with rich persistence features.
 *
 * @param <Key> Type of key used to uniquely address models.
 * @param <Model> Message type which this database adapter is handling.
 * @param <ReadRecord> Intermediate read record.
 * @param <WriteRecord> Intermediate write record.
 */
public interface DatabaseAdapter<Key extends Message, Model extends Message, ReadRecord, WriteRecord>
  extends ModelAdapter<Key, Model> {
  /**
   * Return the lower-level {@link DatabaseDriver} powering this adapter. The driver is responsible for communicating
   * with the actual database or storage service, either via local stubs/emulators or a production API.
   *
   * @return Database driver instance currently in use by this model adapter.
   */
  @Nonnull DatabaseDriver<Key, Model, ReadRecord, WriteRecord> engine();

  /** {@inheritDoc} */
  @Override
  default @Nonnull Key generateKey(@Nonnull Message instance) {
    return engine().generateKey(instance);
  }
}
