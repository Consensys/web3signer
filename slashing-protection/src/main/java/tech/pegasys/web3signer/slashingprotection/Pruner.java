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

import static org.jdbi.v3.core.transaction.TransactionIsolationLevel.READ_UNCOMMITTED;
import static tech.pegasys.web3signer.slashingprotection.DbLocker.lockForValidator;

import tech.pegasys.web3signer.slashingprotection.DbLocker.LockType;
import tech.pegasys.web3signer.slashingprotection.dao.LowWatermarkDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestationsDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlocksDao;

import java.util.Optional;

import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.core.Jdbi;

public class Pruner {
  private final Jdbi jdbi;
  private final SignedBlocksDao signedBlocksDao;
  private final SignedAttestationsDao signedAttestationsDao;
  private final LowWatermarkDao lowWatermarkDao;

  public Pruner(
      final Jdbi jdbi,
      final SignedBlocksDao signedBlocksDao,
      final SignedAttestationsDao signedAttestationsDao,
      final LowWatermarkDao lowWatermarkDao) {
    this.jdbi = jdbi;
    this.signedBlocksDao = signedBlocksDao;
    this.signedAttestationsDao = signedAttestationsDao;
    this.lowWatermarkDao = lowWatermarkDao;
  }

  public void pruneValidator(
      final int validatorId, final long epochsToPrune, final long slotsPerEpoch) {
    final long slotsToPrune = Math.max(epochsToPrune / slotsPerEpoch, 1);
    pruneBlocks(validatorId, slotsToPrune);
    pruneAttestations(validatorId, epochsToPrune);
  }

  private void pruneBlocks(final int validatorId, final long slotsToPrune) {
    final Optional<UInt64> pruningSlot =
        jdbi.inTransaction(
            READ_UNCOMMITTED,
            h -> {
              lockForValidator(h, LockType.BLOCK, validatorId);
              final Optional<UInt64> slot =
                  signedBlocksDao.findMaxSlot(h, validatorId).map(s -> s.subtract(slotsToPrune));
              slot.ifPresent(s -> lowWatermarkDao.updateSlotWatermarkFor(h, validatorId, s));
              return slot;
            });

    pruningSlot.ifPresent(
        s ->
            jdbi.useTransaction(
                READ_UNCOMMITTED, h -> signedBlocksDao.deleteBlocksBelowSlot(h, validatorId, s)));
  }

  private void pruneAttestations(final int validatorId, final long epochsToPrune) {
    final Optional<UInt64> pruningEpoch =
        jdbi.inTransaction(
            READ_UNCOMMITTED,
            h -> {
              lockForValidator(h, LockType.ATTESTATION, validatorId);
              final Optional<UInt64> epoch =
                  signedAttestationsDao
                      .findMaxTargetEpoch(h, validatorId)
                      .map(s -> s.subtract(epochsToPrune));
              epoch.ifPresent(e -> lowWatermarkDao.updateEpochWatermarksFor(h, validatorId, e, e));
              return epoch;
            });

    pruningEpoch.ifPresent(
        e ->
            jdbi.useTransaction(
                READ_UNCOMMITTED,
                h -> signedAttestationsDao.deleteAttestationsBelowEpoch(h, validatorId, e)));
  }
}
