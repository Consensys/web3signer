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

import org.jdbi.v3.core.Jdbi;

public class SlashingProtectionFactory {

  public static final int EXPECTED_DATABASE_VERSION = 6;
  private static final String errorMsg =
      String.format(
          "Database does not have expected version (%s), please run migrations and try again.",
          EXPECTED_DATABASE_VERSION);

  public static SlashingProtection createSlashingProtection(
      final String slashingProtectionDbUrl,
      final String slashingProtectionDbUser,
      final String slashingProtectionDbPassword) {
    final Jdbi jdbi =
        DbConnection.createConnection(
            slashingProtectionDbUrl, slashingProtectionDbUser, slashingProtectionDbPassword);

    verifyVersion(jdbi);

    return createSlashingProtection(jdbi);
  }

  private static void verifyVersion(final Jdbi jdbi) {
    final DatabaseVersionDao databaseVersionDao = new DatabaseVersionDao();

    try {
      final Integer version = jdbi.withHandle(databaseVersionDao::findDatabaseVersion);
      if (version.compareTo(EXPECTED_DATABASE_VERSION) != 0) {
        throw new IllegalStateException(errorMsg);
      }
    } catch (final IllegalStateException e) {
      throw new IllegalStateException(errorMsg, e);
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
