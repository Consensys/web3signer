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

import java.util.Optional;

import db.DatabaseSetupExtension;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DatabaseSetupExtension.class)
public class LowWatermarkDaoTest {

  private final LowWatermarkDao lowWatermarkDao = new LowWatermarkDao();

  @Test
  public void emptyTableReturnsEmptyLowWatermark(final Handle handle) {
    insertValidator(handle, Bytes.of(100), 1);
    final Optional<SigningWatermark> result =
        lowWatermarkDao.findLowWatermarkForValidator(handle, 1);

    assertThat(result).isEmpty();
  }

  @Test
  public void canCreateANewWatermarkForAttestation(final Handle handle) {
    insertValidator(handle, Bytes.of(100), 1);
    lowWatermarkDao.updateEpochWatermarksFor(handle, 1, UInt64.valueOf(2), UInt64.valueOf(3));
    final Optional<SigningWatermark> watermark =
        lowWatermarkDao.findLowWatermarkForValidator(handle, 1);

    assertThat(watermark).isNotEmpty();
    assertThat(watermark.get().getSlot()).isNull();
    assertThat(watermark.get().getSourceEpoch()).isEqualTo(UInt64.valueOf(2));
    assertThat(watermark.get().getTargetEpoch()).isEqualTo(UInt64.valueOf(3));
  }

  @Test
  public void canReplaceAttestationLowWaterMarks(final Handle handle) {
    insertValidator(handle, Bytes.of(100), 1);
    lowWatermarkDao.updateEpochWatermarksFor(handle, 1, UInt64.valueOf(2), UInt64.valueOf(3));
    Optional<SigningWatermark> watermark = lowWatermarkDao.findLowWatermarkForValidator(handle, 1);
    assertThat(watermark).isNotEmpty();
    assertThat(watermark.get().getSlot()).isNull();
    assertThat(watermark.get().getSourceEpoch()).isEqualTo(UInt64.valueOf(2));
    assertThat(watermark.get().getTargetEpoch()).isEqualTo(UInt64.valueOf(3));

    lowWatermarkDao.updateEpochWatermarksFor(handle, 1, UInt64.valueOf(4), UInt64.valueOf(5));
    watermark = lowWatermarkDao.findLowWatermarkForValidator(handle, 1);
    assertThat(watermark).isNotEmpty();
    assertThat(watermark.get().getSlot()).isNull();
    assertThat(watermark.get().getSourceEpoch()).isEqualTo(UInt64.valueOf(4));
    assertThat(watermark.get().getTargetEpoch()).isEqualTo(UInt64.valueOf(5));
  }

  @Test
  public void canSetJustSlotInAWaterMark(final Handle handle) {
    insertValidator(handle, Bytes.of(100), 1);
    lowWatermarkDao.updateSlotWatermarkFor(handle, 1, UInt64.valueOf(3));
    Optional<SigningWatermark> watermark = lowWatermarkDao.findLowWatermarkForValidator(handle, 1);
    assertThat(watermark).isNotEmpty();
    assertThat(watermark.get().getSlot()).isEqualTo(UInt64.valueOf(3));
    assertThat(watermark.get().getSourceEpoch()).isNull();
    assertThat(watermark.get().getTargetEpoch()).isNull();
  }

  @Test
  public void canSetSlotSeparatelyToEpochs(final Handle handle) {
    insertValidator(handle, Bytes.of(100), 1);
    lowWatermarkDao.updateEpochWatermarksFor(handle, 1, UInt64.valueOf(2), UInt64.valueOf(3));
    Optional<SigningWatermark> watermark = lowWatermarkDao.findLowWatermarkForValidator(handle, 1);
    assertThat(watermark).isNotEmpty();
    assertThat(watermark.get().getSlot()).isNull();
    assertThat(watermark.get().getSourceEpoch()).isEqualTo(UInt64.valueOf(2));
    assertThat(watermark.get().getTargetEpoch()).isEqualTo(UInt64.valueOf(3));

    lowWatermarkDao.updateSlotWatermarkFor(handle, 1, UInt64.valueOf(5));
    watermark = lowWatermarkDao.findLowWatermarkForValidator(handle, 1);
    assertThat(watermark).isNotEmpty();
    assertThat(watermark.get().getSlot()).isEqualTo(UInt64.valueOf(5));
    assertThat(watermark.get().getSourceEpoch()).isEqualTo(UInt64.valueOf(2));
    assertThat(watermark.get().getTargetEpoch()).isEqualTo(UInt64.valueOf(3));
  }

  @Test
  public void returnsCorrectValidatorsWatermarkWhenMultipleExist(final Handle handle) {
    insertValidator(handle, Bytes.of(100), 1);
    insertValidator(handle, Bytes.of(200), 2);

    lowWatermarkDao.updateEpochWatermarksFor(handle, 1, UInt64.valueOf(2), UInt64.valueOf(3));
    lowWatermarkDao.updateSlotWatermarkFor(handle, 1, UInt64.valueOf(4));

    lowWatermarkDao.updateEpochWatermarksFor(handle, 2, UInt64.valueOf(5), UInt64.valueOf(6));
    lowWatermarkDao.updateSlotWatermarkFor(handle, 2, UInt64.valueOf(7));

    Optional<SigningWatermark> firstValidator =
        lowWatermarkDao.findLowWatermarkForValidator(handle, 1);
    Optional<SigningWatermark> secondValidator =
        lowWatermarkDao.findLowWatermarkForValidator(handle, 2);

    assertThat(firstValidator).isNotEmpty();
    assertThat(firstValidator.get().getSlot()).isEqualTo(UInt64.valueOf(4));
    assertThat(firstValidator.get().getSourceEpoch()).isEqualTo(UInt64.valueOf(2));
    assertThat(firstValidator.get().getTargetEpoch()).isEqualTo(UInt64.valueOf(3));

    assertThat(secondValidator).isNotEmpty();
    assertThat(secondValidator.get().getSlot()).isEqualTo(UInt64.valueOf(7));
    assertThat(secondValidator.get().getSourceEpoch()).isEqualTo(UInt64.valueOf(5));
    assertThat(secondValidator.get().getTargetEpoch()).isEqualTo(UInt64.valueOf(6));
  }

  @Test
  public void ensureBothWatermarksMustBeNonNull(final Handle handle) {
    insertValidator(handle, Bytes.of(100), 1);
    assertThatThrownBy(
            () -> lowWatermarkDao.updateEpochWatermarksFor(handle, 1, null, UInt64.valueOf(3)))
        .isInstanceOf(UnableToExecuteStatementException.class);
    assertThatThrownBy(
            () -> lowWatermarkDao.updateEpochWatermarksFor(handle, 1, UInt64.valueOf(2), null))
        .isInstanceOf(UnableToExecuteStatementException.class);
  }

  @Test
  public void cannotUpdateAttestationWatermarkWithLowerValue(final Handle handle) {
    Optional<SigningWatermark> watermark;
    insertValidator(handle, Bytes.of(100), 1);

    lowWatermarkDao.updateEpochWatermarksFor(handle, 1, UInt64.valueOf(2), UInt64.valueOf(3));
    watermark = lowWatermarkDao.findLowWatermarkForValidator(handle, 1);
    assertThat(watermark).isNotEmpty();
    assertThat(watermark.get().getSourceEpoch()).isEqualTo(UInt64.valueOf(2));
    assertThat(watermark.get().getTargetEpoch()).isEqualTo(UInt64.valueOf(3));

    lowWatermarkDao.updateEpochWatermarksFor(handle, 1, UInt64.valueOf(1), UInt64.valueOf(2));
    watermark = lowWatermarkDao.findLowWatermarkForValidator(handle, 1);
    assertThat(watermark).isNotEmpty();
    assertThat(watermark.get().getSourceEpoch()).isEqualTo(UInt64.valueOf(2));
    assertThat(watermark.get().getTargetEpoch()).isEqualTo(UInt64.valueOf(3));

    lowWatermarkDao.updateEpochWatermarksFor(handle, 1, UInt64.valueOf(3), UInt64.valueOf(4));
    watermark = lowWatermarkDao.findLowWatermarkForValidator(handle, 1);
    assertThat(watermark).isNotEmpty();
    assertThat(watermark.get().getSourceEpoch()).isEqualTo(UInt64.valueOf(3));
    assertThat(watermark.get().getTargetEpoch()).isEqualTo(UInt64.valueOf(4));
  }

  @Test
  public void cannotUpdateBlockWatermarkWithALowerValue(final Handle handle) {
    Optional<SigningWatermark> watermark;
    insertValidator(handle, Bytes.of(100), 1);

    lowWatermarkDao.updateSlotWatermarkFor(handle, 1, UInt64.valueOf(3));
    watermark = lowWatermarkDao.findLowWatermarkForValidator(handle, 1);
    assertThat(watermark).isNotEmpty();
    assertThat(watermark.get().getSlot()).isEqualTo(UInt64.valueOf(3));

    lowWatermarkDao.updateSlotWatermarkFor(handle, 1, UInt64.valueOf(1));
    watermark = lowWatermarkDao.findLowWatermarkForValidator(handle, 1);
    assertThat(watermark).isNotEmpty();
    assertThat(watermark.get().getSlot()).isEqualTo(UInt64.valueOf(3));

    lowWatermarkDao.updateSlotWatermarkFor(handle, 1, UInt64.valueOf(4));
    watermark = lowWatermarkDao.findLowWatermarkForValidator(handle, 1);
    assertThat(watermark).isNotEmpty();
    assertThat(watermark.get().getSlot()).isEqualTo(UInt64.valueOf(4));
  }

  @Test
  public void canUpdateAttestationWatermarkAfterBlockWatermark(final Handle handle) {
    Optional<SigningWatermark> watermark;
    insertValidator(handle, Bytes.of(100), 1);

    lowWatermarkDao.updateSlotWatermarkFor(handle, 1, UInt64.valueOf(3));
    watermark = lowWatermarkDao.findLowWatermarkForValidator(handle, 1);
    assertThat(watermark).isNotEmpty();
    assertThat(watermark.get().getSlot()).isEqualTo(UInt64.valueOf(3));

    lowWatermarkDao.updateEpochWatermarksFor(handle, 1, UInt64.valueOf(4), UInt64.valueOf(5));
    watermark = lowWatermarkDao.findLowWatermarkForValidator(handle, 1);
    assertThat(watermark).isNotEmpty();
    assertThat(watermark.get().getSlot()).isEqualTo(UInt64.valueOf(3));
    assertThat(watermark.get().getSourceEpoch()).isEqualTo(UInt64.valueOf(4));
    assertThat(watermark.get().getTargetEpoch()).isEqualTo(UInt64.valueOf(5));
  }

  private void insertValidator(final Handle handle, final Bytes publicKey, final int validatorId) {
    handle.execute("INSERT INTO validators (id, public_key) VALUES (?, ?)", validatorId, publicKey);
  }
}
