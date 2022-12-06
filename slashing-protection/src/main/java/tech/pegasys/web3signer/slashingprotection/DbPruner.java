/*
 * Copyright 2021 ConsenSys AG.
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
package tech.pegasys.web3signer.slashingprotection;

import static com.google.common.base.Preconditions.checkArgument;
import static org.jdbi.v3.core.transaction.TransactionIsolationLevel.READ_UNCOMMITTED;
import static tech.pegasys.web3signer.slashingprotection.DbLocker.lockForValidator;

import tech.pegasys.web3signer.slashingprotection.DbLocker.LockType;
import tech.pegasys.web3signer.slashingprotection.dao.LowWatermarkDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestation;
import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestationsDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlock;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlocksDao;
import tech.pegasys.web3signer.slashingprotection.dao.SigningWatermark;

import java.util.Optional;

import org.apache.tuweni.units.bigints.UInt64;
import org.apache.tuweni.units.bigints.UInt64s;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

public class DbPruner {
  private final Jdbi jdbi;
  private final SignedBlocksDao signedBlocksDao;
  private final SignedAttestationsDao signedAttestationsDao;
  private final LowWatermarkDao lowWatermarkDao;

  public DbPruner(
      final Jdbi jdbi,
      final SignedBlocksDao signedBlocksDao,
      final SignedAttestationsDao signedAttestationsDao,
      final LowWatermarkDao lowWatermarkDao) {
    this.jdbi = jdbi;
    this.signedBlocksDao = signedBlocksDao;
    this.signedAttestationsDao = signedAttestationsDao;
    this.lowWatermarkDao = lowWatermarkDao;
  }

  public void pruneForValidator(
      final int validatorId, final long epochsToKeep, final long slotsPerEpoch) {
    checkArgument(
        epochsToKeep > 0, "epochsToKeep must be a positive value, but was %s", epochsToKeep);
    checkArgument(
        slotsPerEpoch > 0, "slotsPerEpoch must be a positive value, but was %s", slotsPerEpoch);
    final long slotsToKeep = epochsToKeep * slotsPerEpoch;
    pruneBlocks(validatorId, slotsToKeep);
    pruneAttestations(validatorId, epochsToKeep);
  }

  private void pruneBlocks(final int validatorId, final long slotsToKeep) {
    final boolean hasWatermark =
        jdbi.inTransaction(
            READ_UNCOMMITTED,
            h -> {
              lockForValidator(h, LockType.BLOCK, validatorId);
              return moveWatermarkForBlock(validatorId, slotsToKeep, h);
            });

    if (hasWatermark) {
      jdbi.useTransaction(
          READ_UNCOMMITTED, h -> signedBlocksDao.deleteBlocksBelowWatermark(h, validatorId));
    }
  }

  private boolean moveWatermarkForBlock(
      final int validatorId, final long slotsToKeep, final Handle handle) {
    final Optional<UInt64> watermarkSlot =
        lowWatermarkDao
            .findLowWatermarkForValidator(handle, validatorId)
            .map(SigningWatermark::getSlot);
    final Optional<UInt64> mostRecentSlot = signedBlocksDao.findMaxSlot(handle, validatorId);
    final Optional<UInt64> slotWatermark =
        calculateWatermark(slotsToKeep, mostRecentSlot, watermarkSlot);
    final Optional<SignedBlock> watermark =
        slotWatermark.flatMap(
            w -> signedBlocksDao.findNearestBlockWithSlot(handle, validatorId, w));
    watermark.ifPresent(
        s -> lowWatermarkDao.updateSlotWatermarkFor(handle, validatorId, s.getSlot()));
    return slotWatermark.isPresent();
  }

  private void pruneAttestations(final int validatorId, final long epochsToKeep) {
    final boolean hasWatermark =
        jdbi.inTransaction(
            READ_UNCOMMITTED,
            h -> {
              lockForValidator(h, LockType.ATTESTATION, validatorId);
              return moveWatermarkForAttestation(validatorId, epochsToKeep, h);
            });

    if (hasWatermark) {
      jdbi.useTransaction(
          READ_UNCOMMITTED,
          h -> signedAttestationsDao.deleteAttestationsBelowWatermark(h, validatorId));
    }
  }

  private boolean moveWatermarkForAttestation(
      final int validatorId, final long epochsToKeep, final Handle h) {
    final Optional<UInt64> watermarkEpoch =
        lowWatermarkDao
            .findLowWatermarkForValidator(h, validatorId)
            .map(SigningWatermark::getTargetEpoch);
    final Optional<UInt64> mostRecentEpoch =
        signedAttestationsDao.findMaxTargetEpoch(h, validatorId);
    final Optional<UInt64> targetEpochWatermark =
        calculateWatermark(epochsToKeep, mostRecentEpoch, watermarkEpoch);
    final Optional<SignedAttestation> watermark =
        targetEpochWatermark.flatMap(
            w -> signedAttestationsDao.findNearestAttestationWithTargetEpoch(h, validatorId, w));
    targetEpochWatermark.ifPresent(
        e ->
            lowWatermarkDao.updateEpochWatermarksFor(
                h,
                validatorId,
                watermark.get().getSourceEpoch(),
                watermark.get().getTargetEpoch()));
    return targetEpochWatermark.isPresent();
  }

  private Optional<UInt64> calculateWatermark(
      final long amountToKeep, final Optional<UInt64> highpoint, final Optional<UInt64> watermark) {
    return highpoint.flatMap(
        h ->
            watermark.map(
                w -> {
                  final UInt64 pruningPoint =
                      h.compareTo(UInt64.valueOf(amountToKeep)) < 0
                          ? UInt64.ZERO
                          // add one as we remove below the watermark
                          : h.subtract(amountToKeep).add(1);
                  final UInt64 newWatermark = UInt64s.max(UInt64.ZERO, pruningPoint);
                  return UInt64s.max(newWatermark, w);
                }));
  }
}
