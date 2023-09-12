/*
 * Copyright 2020 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.web3signer.slashingprotection.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

import db.DatabaseSetupExtension;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.core.Handle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DatabaseSetupExtension.class)
public class MetadataDaoTest {
  private final MetadataDao metadataDao = new MetadataDao();
  private static final UInt64 MAX_LOW_WATERMARK_SLOT = UInt64.valueOf(13);
  private static final UInt64 MAX_LOW_WATERMARK_SOURCE_EPOCH = UInt64.valueOf(11);
  private static final UInt64 MAX_LOW_WATERMARK_TARGET_EPOCH = UInt64.valueOf(12);

  @Test
  public void findsExistingGvrInDb(final Handle handle) {
    insertGvr(handle, Bytes32.leftPad(Bytes.of(3)));

    final Optional<Bytes32> existingGvr = metadataDao.findGenesisValidatorsRoot(handle);
    assertThat(existingGvr).isNotEmpty();
    assertThat(existingGvr).contains(Bytes32.leftPad(Bytes.of(3)));
  }

  @Test
  public void findsExistingGvrAfterHighWatermarkIsSet(final Handle handle) {
    insertGvr(handle, Bytes32.leftPad(Bytes.of(3)));
    updateHighWatermark(handle, 1, 2);

    final Optional<Bytes32> existingGvr = metadataDao.findGenesisValidatorsRoot(handle);
    assertThat(existingGvr).isNotEmpty();
    assertThat(existingGvr).contains(Bytes32.leftPad(Bytes.of(3)));
  }

  @Test
  public void returnsEmptyForNonExistingGvrInDb(final Handle handle) {
    assertThat(metadataDao.findGenesisValidatorsRoot(handle)).isEmpty();
  }

  @Test
  public void insertsGvrIntoDb(final Handle handle) {
    final Bytes32 genesisValidatorsRoot = Bytes32.leftPad(Bytes.of(4));
    metadataDao.insertGenesisValidatorsRoot(handle, genesisValidatorsRoot);

    final List<Bytes32> gvrs =
        handle
            .createQuery("SELECT genesis_validators_root FROM metadata")
            .mapTo(Bytes32.class)
            .list();
    assertThat(gvrs.size()).isEqualTo(1);
    assertThat(gvrs.get(0)).isEqualTo(genesisValidatorsRoot);
  }

  @Test
  public void failsInsertingMultipleGvrIntoDb(final Handle handle) {
    final Bytes32 genesisValidatorsRoot = Bytes32.leftPad(Bytes.of(4));
    metadataDao.insertGenesisValidatorsRoot(handle, genesisValidatorsRoot);

    assertThatThrownBy(() -> metadataDao.insertGenesisValidatorsRoot(handle, genesisValidatorsRoot))
        .hasMessageContaining("duplicate key value violates unique constraint");
  }

  @Test
  public void findsExistingHighWatermark(final Handle handle) {
    insertGvr(handle, Bytes32.leftPad(Bytes.of(3)));
    updateHighWatermark(handle, 1, 2);

    final Optional<HighWatermark> existingHighWatermark = metadataDao.findHighWatermark(handle);

    assertThat(existingHighWatermark).isNotEmpty();
    assertThat(existingHighWatermark)
        .contains(new HighWatermark(UInt64.valueOf(2), UInt64.valueOf(1)));
  }

  @Test
  public void findsExistingHighWatermarkWithOnlyEpoch(final Handle handle) {
    insertGvr(handle, Bytes32.leftPad(Bytes.of(3)));
    updateHighWatermark(handle, 1, null);

    final Optional<HighWatermark> existingHighWatermark = metadataDao.findHighWatermark(handle);

    assertThat(existingHighWatermark).contains(new HighWatermark(null, UInt64.valueOf(1)));
  }

  @Test
  public void findsExistingHighWatermarkWithOnlySlot(final Handle handle) {
    insertGvr(handle, Bytes32.leftPad(Bytes.of(3)));
    updateHighWatermark(handle, null, 2);

    final Optional<HighWatermark> existingHighWatermark = metadataDao.findHighWatermark(handle);

    assertThat(existingHighWatermark).contains(new HighWatermark(UInt64.valueOf(2), null));
  }

  @Test
  public void returnsEmptyForNonExistingHighWatermark(final Handle handle) {
    assertThat(metadataDao.findHighWatermark(handle)).isEmpty();
  }

  @Test
  public void returnsEmptyForNonExistingHighWatermarkWhenGvrSet(final Handle handle) {
    insertGvr(handle, Bytes32.leftPad(Bytes.of(3)));
    assertThat(metadataDao.findHighWatermark(handle)).isEmpty();
  }

  @Test
  public void insertsHighWatermark(final Handle handle) {
    insertGvr(handle, Bytes32.leftPad(Bytes.of(3)));
    HighWatermark highWatermark = new HighWatermark(UInt64.valueOf(2), UInt64.valueOf(1));

    int updateCount = metadataDao.updateHighWatermark(handle, highWatermark);

    assertHighWatermarkUpdatedSuccessfully(handle, updateCount, highWatermark);
  }

  @Test
  public void insertsOnlyEpochHighWatermark(final Handle handle) {
    insertGvr(handle, Bytes32.leftPad(Bytes.of(3)));
    HighWatermark highWatermark = new HighWatermark(null, UInt64.valueOf(1));

    int updateCount = metadataDao.updateHighWatermark(handle, highWatermark);

    assertHighWatermarkUpdatedSuccessfully(handle, updateCount, highWatermark);
  }

  @Test
  public void insertsOnlySlotHighWatermark(final Handle handle) {
    insertGvr(handle, Bytes32.leftPad(Bytes.of(3)));
    HighWatermark highWatermark = new HighWatermark(UInt64.valueOf(1), null);

    int updateCount = metadataDao.updateHighWatermark(handle, highWatermark);

    assertHighWatermarkUpdatedSuccessfully(handle, updateCount, highWatermark);
  }

  @Test
  public void updatesHighWatermark(final Handle handle) {
    insertGvr(handle, Bytes32.leftPad(Bytes.of(3)));
    updateHighWatermark(handle, 1, 2);
    HighWatermark highWatermark = createHighWatermark(3, 3);

    int updateCount = metadataDao.updateHighWatermark(handle, highWatermark);

    assertHighWatermarkUpdatedSuccessfully(handle, updateCount, highWatermark);
  }

  private void assertHighWatermarkUpdatedSuccessfully(
      Handle handle, int updateCount, HighWatermark highWatermark) {
    assertThat(updateCount).isEqualTo(1);
    final List<HighWatermark> highWatermarks =
        handle
            .createQuery(
                "SELECT high_watermark_epoch as epoch, high_watermark_slot as slot FROM metadata")
            .mapToBean(HighWatermark.class)
            .list();
    assertThat(highWatermarks.size()).isEqualTo(1);
    assertThat(highWatermarks.get(0)).isEqualTo(highWatermark);
  }

  @Test
  public void updateHighWatermarkWhenNoGvrHasNoEffect(final Handle handle) {
    int updateCount = metadataDao.updateHighWatermark(handle, createHighWatermark(1, 1));
    assertThat(updateCount).isEqualTo(0);
  }

  @Test
  public void updateHighWatermarkFailsWhenNotGreaterThanMaxLowWatermarkSlot(final Handle handle) {
    insertGvr(handle, Bytes32.leftPad(Bytes.of(3)));
    insertLowWatermarks(handle);
    // high watermark == max low watermark
    HighWatermark highWatermark = createHighWatermark(MAX_LOW_WATERMARK_SLOT, UInt64.MAX_VALUE);
    assertThatThrownBy(() -> metadataDao.updateHighWatermark(handle, highWatermark))
        .hasMessageContaining(
            "high_watermark_slot must be greater than max slot in low_watermarks table");
  }

  @Test
  public void updateHighWatermarkFailsWhenNotGreaterThanMaxLowWatermarkTargetEpoch(
      final Handle handle) {
    insertGvr(handle, Bytes32.leftPad(Bytes.of(3)));
    insertLowWatermarks(handle);
    // high watermark == max low watermark
    HighWatermark highWatermark =
        createHighWatermark(UInt64.MAX_VALUE, MAX_LOW_WATERMARK_TARGET_EPOCH);

    assertThatThrownBy(() -> metadataDao.updateHighWatermark(handle, highWatermark))
        .hasMessageContaining(
            "high_watermark_epoch must be greater than max epoch in low_watermarks table");
  }

  @Test
  public void updateHighWatermarkFailsWhenNotGreaterThanMaxLowWatermarkSourceEpoch(
      final Handle handle) {
    insertGvr(handle, Bytes32.leftPad(Bytes.of(3)));
    insertLowWatermarks(handle);
    // high watermark == max low watermark
    HighWatermark highWatermark =
        createHighWatermark(UInt64.MAX_VALUE, MAX_LOW_WATERMARK_SOURCE_EPOCH);

    assertThatThrownBy(() -> metadataDao.updateHighWatermark(handle, highWatermark))
        .hasMessageContaining(
            "high_watermark_epoch must be greater than max epoch in low_watermarks table");
  }

  @Test
  public void deletesHighWatermark(final Handle handle) {
    insertGvr(handle, Bytes32.leftPad(Bytes.of(3)));
    updateHighWatermark(handle, 2, 2);
    assertThat(metadataDao.findHighWatermark(handle)).isNotEmpty();

    metadataDao.deleteHighWatermark(handle);

    assertThat(metadataDao.findHighWatermark(handle)).isEmpty();
  }

  private void insertGvr(final Handle handle, final Bytes genesisValidatorsRoot) {
    handle.execute(
        "INSERT INTO metadata (id, genesis_validators_root) VALUES (?, ?)",
        1,
        genesisValidatorsRoot);
  }

  private void insertLowWatermarks(Handle handle) {
    handle.execute("INSERT INTO validators (public_key, enabled) VALUES (?, ?)", Bytes.of(1), true);
    handle.execute("INSERT INTO validators (public_key, enabled) VALUES (?, ?)", Bytes.of(2), true);
    handle.execute(
        "INSERT INTO low_watermarks (validator_id, slot, target_epoch, source_epoch) VALUES (?, ?, ?, ?)",
        1,
        3,
        2,
        1);
    handle.execute(
        "INSERT INTO low_watermarks (validator_id, slot, target_epoch, source_epoch) VALUES (?, ?, ?, ?)",
        2,
        MAX_LOW_WATERMARK_SLOT,
        MAX_LOW_WATERMARK_TARGET_EPOCH,
        MAX_LOW_WATERMARK_SOURCE_EPOCH);
  }

  private void updateHighWatermark(final Handle handle, final Integer epoch, final Integer slot) {
    handle
        .createUpdate("UPDATE metadata set high_watermark_epoch=:epoch, high_watermark_slot=:slot")
        .bind("epoch", epoch)
        .bind("slot", slot)
        .execute();
  }

  private HighWatermark createHighWatermark(final int slot, final int epoch) {
    return new HighWatermark(UInt64.valueOf(slot), UInt64.valueOf(epoch));
  }

  private HighWatermark createHighWatermark(final UInt64 slot, final UInt64 epoch) {
    return new HighWatermark(slot, epoch);
  }
}
