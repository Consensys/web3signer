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
package tech.pegasys.web3signer.slashingprotection;

import tech.pegasys.web3signer.slashingprotection.dao.DatabaseVersionDao;
import tech.pegasys.web3signer.slashingprotection.dao.LowWatermarkDao;
import tech.pegasys.web3signer.slashingprotection.dao.MetadataDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestationsDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlocksDao;
import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Jdbi;

public class SlashingProtectionFactory {

  public static final int EXPECTED_DATABASE_VERSION = 7;
  private static final Logger LOG = LogManager.getLogger();

  public static SlashingProtection createSlashingProtection(
      final SlashingProtectionParameters slashingProtectionParameters) {
    final Jdbi jdbi =
        DbConnection.createConnection(
            slashingProtectionParameters.getDbUrl(),
            slashingProtectionParameters.getDbUsername(),
            slashingProtectionParameters.getDbPassword());

    verifyVersion(jdbi);

    final SlashingProtection slashingProtection = createSlashingProtection(jdbi);

    if (slashingProtectionParameters.isPruningEnabled()) {
      schedulePruning(slashingProtectionParameters, slashingProtection);
    }

    return slashingProtection;
  }

  private static void schedulePruning(
      final SlashingProtectionParameters slashingProtectionParameters,
      final SlashingProtection slashingProtection) {
    final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    executorService.scheduleAtFixedRate(
        () -> {
          try {
            slashingProtection.prune(
                slashingProtectionParameters.getPruningEpochsToKeep(),
                slashingProtectionParameters.getPruningSlotsPerEpoch());
          } catch (Exception e) {
            // We only log the error as retrying on the scheduled prune might fix the error
            LOG.info("Pruning slashing protection database failed with error", e);
          }
        },
        0,
        slashingProtectionParameters.getPruningInterval(),
        TimeUnit.HOURS);
  }

  private static void verifyVersion(final Jdbi jdbi) {
    final DatabaseVersionDao databaseVersionDao = new DatabaseVersionDao();

    final int version;
    try {
      version = jdbi.withHandle(databaseVersionDao::findDatabaseVersion);
    } catch (final IllegalStateException e) {
      final String errorMsg =
          String.format("Failed to read database version, expected %s", EXPECTED_DATABASE_VERSION);
      throw new IllegalStateException(errorMsg, e);
    }

    if (version != EXPECTED_DATABASE_VERSION) {
      final String errorMsg =
          String.format(
              "Database version (%s) does not match expected version (%s), please run migrations and try again.",
              version, EXPECTED_DATABASE_VERSION);
      throw new IllegalStateException(errorMsg);
    }
  }

  private static SlashingProtection createSlashingProtection(final Jdbi jdbi) {
    return new DbSlashingProtection(
        jdbi,
        new ValidatorsDao(),
        new SignedBlocksDao(),
        new SignedAttestationsDao(),
        new MetadataDao(),
        new LowWatermarkDao());
  }
}
