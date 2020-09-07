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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.units.bigints.UInt64;

public class DbBackedSlashingProtection implements SlashingProtection {

  private static final String BLOCK_SLOT_TABLE_NAME = "blockSlots";
  private static final String ATTESTATION_EPOCH_TABLE_NAME = "attestationEpochs";

  private static final Logger LOG = LogManager.getLogger();

  private final PreparedStatement canSignBlockSqlTemplate;
  private final PreparedStatement addBlockSignEntrySqlTemplate;
  private final PreparedStatement canSignAttestationTemplate;
  private final PreparedStatement addAttestationSignEntrySqlTemplate;

  public DbBackedSlashingProtection(
      final PreparedStatement canSignBlockSqlTemplate,
      final PreparedStatement addBlockSignEntrySqlTemplate,
      final PreparedStatement canSignAttestationTemplate,
      final PreparedStatement addAttestationSignEntrySqlTemplate) {
    this.canSignBlockSqlTemplate = canSignBlockSqlTemplate;
    this.addBlockSignEntrySqlTemplate = addBlockSignEntrySqlTemplate;
    this.canSignAttestationTemplate = canSignAttestationTemplate;
    this.addAttestationSignEntrySqlTemplate = addAttestationSignEntrySqlTemplate;
  }

  @Override
  public boolean maySignAttestation(
      final String keyIdentifier, final UInt64 sourceEpoch, UInt64 targetEpoch) {
    try {
      canSignAttestationTemplate.setString(1, keyIdentifier);
      // Use binary blobs for UInt64
      canSignAttestationTemplate.setLong(2, sourceEpoch.toLong());
      canSignAttestationTemplate.setLong(3, targetEpoch.toLong());
      if (canSignAttestationTemplate.executeQuery().getInt("total") > 0) {
        addAttestationSignEntrySqlTemplate.setString(1, keyIdentifier);
        addAttestationSignEntrySqlTemplate.setLong(2, sourceEpoch.toLong());
        addAttestationSignEntrySqlTemplate.setLong(3, targetEpoch.toLong());
        final int rowsUpdated = addAttestationSignEntrySqlTemplate.executeUpdate();
        return (rowsUpdated > 0);
      }
    } catch (SQLException throwables) {
      LOG.error("Failed to read/write attestation signing record to database", throwables);
      throwables.printStackTrace();
    }

    return false;
  }

  @Override
  public boolean maySignBlock(final String keyIdentifier, final UInt64 blockSlot) {
    try {
      canSignBlockSqlTemplate.setString(1, keyIdentifier);
      canSignBlockSqlTemplate.setLong(2, blockSlot.toLong());

      if (canSignBlockSqlTemplate.executeQuery().getInt("total") > 0) {
        addBlockSignEntrySqlTemplate.setString(1, keyIdentifier);
        addBlockSignEntrySqlTemplate.setLong(2, blockSlot.toLong());
        final int rowsUpdated = addBlockSignEntrySqlTemplate.executeUpdate();
        return (rowsUpdated > 0);
      }
    } catch (SQLException throwables) {
      LOG.error("Failed to read/write block signing record to database", throwables);
      throwables.printStackTrace();
    }

    return false;
  }
}
