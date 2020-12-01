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

import tech.pegasys.web3signer.slashingprotection.DbConnection;

import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.jdbi.v3.testing.JdbiRule;
import org.jdbi.v3.testing.Migration;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class LowWatermarkDaoTest {

  @Rule
  public JdbiRule postgres =
      JdbiRule.embeddedPostgres()
          .withMigration(Migration.before().withPath("migrations/postgresql"));

  private final LowWatermarkDao lowWatermarkDao = new LowWatermarkDao();
  private Handle handle;

  @Before
  public void setup() {
    DbConnection.configureJdbi(postgres.getJdbi());
    handle = postgres.getJdbi().open();
  }

  @After
  public void cleanup() {
    handle.close();
  }

  @Test
  public void emptyTableReturnsEmptyLowWatermark() {
    insertValidator(Bytes.of(100), 1);
    final Optional<SigningWatermark> result =
        lowWatermarkDao.findLowWatermarkForValidator(handle, 1);

    assertThat(result).isEmpty();
  }

  @Test
  public void canCreateANewWatermarkForAttestation() {
    insertValidator(Bytes.of(100), 1);
    lowWatermarkDao.updateEpochWatermarksFor(handle, 1, UInt64.valueOf(2), UInt64.valueOf(3));
    final Optional<SigningWatermark> watermark =
        lowWatermarkDao.findLowWatermarkForValidator(handle, 1);

    assertThat(watermark).isNotEmpty();
    assertThat(watermark.get().getSlot()).isNull();
    assertThat(watermark.get().getSourceEpoch()).isEqualTo(UInt64.valueOf(2));
    assertThat(watermark.get().getTargetEpoch()).isEqualTo(UInt64.valueOf(3));
  }

  @Test
  public void canReplaceAttestationLowWaterMarks() {
    insertValidator(Bytes.of(100), 1);
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
  public void canSetJustSlotInAWaterMark() {
    insertValidator(Bytes.of(100), 1);
    lowWatermarkDao.updateSlotWatermarkFor(handle, 1, UInt64.valueOf(3));
    Optional<SigningWatermark> watermark = lowWatermarkDao.findLowWatermarkForValidator(handle, 1);
    assertThat(watermark).isNotEmpty();
    assertThat(watermark.get().getSlot()).isEqualTo(UInt64.valueOf(3));
    assertThat(watermark.get().getSourceEpoch()).isNull();
    assertThat(watermark.get().getTargetEpoch()).isNull();
  }

  @Test
  public void canSetSlotSeparatelyToEpochs() {
    insertValidator(Bytes.of(100), 1);
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
  public void returnsCorrectValidatorsWatermarkWhenMultipleExist() {
    insertValidator(Bytes.of(100), 1);
    insertValidator(Bytes.of(200), 2);

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
  public void ensureBothWatermarksMustBeNonNull() {
    insertValidator(Bytes.of(100), 1);
    assertThatThrownBy(
            () -> lowWatermarkDao.updateEpochWatermarksFor(handle, 1, null, UInt64.valueOf(3)))
        .isInstanceOf(UnableToExecuteStatementException.class);
    assertThatThrownBy(
            () -> lowWatermarkDao.updateEpochWatermarksFor(handle, 1, UInt64.valueOf(2), null))
        .isInstanceOf(UnableToExecuteStatementException.class);
  }

  @Test
  public void cannotUpdateAttestationWatermarkWithLowerValue() {
    Optional<SigningWatermark> watermark;
    insertValidator(Bytes.of(100), 1);

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
  public void cannotUpdateBlockWatermarkWithALowerValue() {
    Optional<SigningWatermark> watermark;
    insertValidator(Bytes.of(100), 1);

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
  public void canUpdateAttestationWatermarkAfterBlockWatermark() {
    Optional<SigningWatermark> watermark;
    insertValidator(Bytes.of(100), 1);

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

  private void insertValidator(final Bytes publicKey, final int validatorId) {
    handle.execute("INSERT INTO validators (id, public_key) VALUES (?, ?)", validatorId, publicKey);
  }
}
