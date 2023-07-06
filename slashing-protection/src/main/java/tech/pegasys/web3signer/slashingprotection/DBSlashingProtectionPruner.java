/*
 * Copyright 2023 ConsenSys AG.
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

import tech.pegasys.web3signer.slashingprotection.dao.LowWatermarkDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestationsDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlocksDao;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Jdbi;

public class DBSlashingProtectionPruner implements SlashingProtectionPruner {
  private static final Logger LOG = LogManager.getLogger();
  private final DbPruner dbPruner;

  private final long pruningEpochsToKeep;
  private final long pruningSlotsPerEpoch;
  private final RegisteredValidators registeredValidators;

  public DBSlashingProtectionPruner(
      final Jdbi pruningJdbi,
      final long pruningEpochsToKeep,
      final long pruningSlotsPerEpoch,
      final RegisteredValidators registeredValidators,
      final SignedBlocksDao signedBlocksDao,
      final SignedAttestationsDao signedAttestationsDao,
      final LowWatermarkDao lowWatermarkDao) {
    this.dbPruner =
        new DbPruner(pruningJdbi, signedBlocksDao, signedAttestationsDao, lowWatermarkDao);
    this.pruningEpochsToKeep = pruningEpochsToKeep;
    this.pruningSlotsPerEpoch = pruningSlotsPerEpoch;
    this.registeredValidators = registeredValidators;
  }

  @Override
  public void prune() {
    final Set<Integer> validatorKeys = registeredValidators.validatorIds();
    LOG.info("Pruning slashing protection database for {} validators", validatorKeys.size());
    final AtomicInteger pruningCount = new AtomicInteger();
    validatorKeys.forEach(
        v -> {
          LOG.trace(
              "Pruning {} of {} validator {}",
              pruningCount::incrementAndGet,
              validatorKeys::size,
              () -> registeredValidators.getPublicKeyForValidatorId(v));
          dbPruner.pruneForValidator(v, pruningEpochsToKeep, pruningSlotsPerEpoch);
        });
    LOG.info("Pruning slashing protection database complete");
  }
}
