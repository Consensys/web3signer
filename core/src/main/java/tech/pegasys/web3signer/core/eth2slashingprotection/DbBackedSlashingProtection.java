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
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package tech.pegasys.web3signer.core.eth2slashingprotection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.units.bigints.UInt64;

public class DbBackedSlashingProtection extends SlashingProtection {

  private static final String BLOCK_SLOT_TABLE_NAME = "blockSlots";
  private static final String ATTESTATION_EPOCH_TABLE_NAME = "attestationEpochs";

  private static final Logger LOG = LogManager.getLogger();

  final Connection connection;
  private final String canSignBlockSqlTemplate;
  private final String addBlockSignEntrySqlTemplate;
  private final String canSignAttestationTemplate;
  private final String addAttestationSignEntrySqlTemplate;

  public DbBackedSlashingProtection(final Connection connection) {
    this.connection = connection;
    canSignBlockSqlTemplate = String.format("SELECT COUNT(*) AS total FROM %s where identifier=%%s AND blockSlot>%%d;", BLOCK_SLOT_TABLE_NAME);
    addBlockSignEntrySqlTemplate = String.format("INSERT INTO %s (identifier, blockSlot) VALUES (%%s, %%d)", BLOCK_SLOT_TABLE_NAME);
    canSignAttestationTemplate = String.format("SELECT COUNT(*) as total from %s where identifier=%%s AND sourceEpoch<%%d AND targetEpoch<%%d", ATTESTATION_EPOCH_TABLE_NAME);
    addAttestationSignEntrySqlTemplate =  String.format("INSERT INTO %s (identifier, sourceEpoch, targetEpoch) VALUES (%%s, %%d, %%d)", ATTESTATION_EPOCH_TABLE_NAME);
  }

  @Override
  public boolean shouldSignAttestation(final String keyIdentifier, final UInt64 sourceEpoch, UInt64 targetEpoch) {
    boolean result = false;
    final Statement statement;
    try {
      statement = connection.createStatement();
      final String canSignSql = String.format(canSignAttestationTemplate, keyIdentifier, sourceEpoch, targetEpoch);
      if(statement.executeQuery(canSignSql).getInt("total") > 0) {
        final String addEntrySql = String.format(addAttestationSignEntrySqlTemplate, keyIdentifier, sourceEpoch, targetEpoch);
        final int rowsUpdated = statement.executeUpdate(addEntrySql);
        return (rowsUpdated > 0);
      }
    } catch (SQLException throwables) {
      LOG.error("Failed to read/write attestation signing record to database", throwables);
      throwables.printStackTrace();
    }

    return result;
  }

  @Override
  public boolean maySignBlock(final String keyIdentifier, final UInt64 blockSlot) {

    boolean result = false;
    try {
      final Statement statement = connection.createStatement();
      final String canSignSql = String.format(canSignBlockSqlTemplate, keyIdentifier, blockSlot);
      if(statement.executeQuery(canSignSql).getInt("total") > 0) {
        final String addEntrySql = String.format(addBlockSignEntrySqlTemplate, keyIdentifier, blockSlot);
        final int rowsUpdated = statement.executeUpdate(addEntrySql);
        return (rowsUpdated > 0);
      }
    } catch (SQLException throwables) {
      LOG.error("Failed to read/write block signing record to database", throwables);
      throwables.printStackTrace();
    }

    return result;
  }
}
