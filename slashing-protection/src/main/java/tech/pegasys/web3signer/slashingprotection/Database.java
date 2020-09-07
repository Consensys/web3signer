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
import java.sql.Statement;
import javax.sql.DataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Database {

  private static final Logger LOG = LogManager.getLogger();

  public static void createTables(final DataSource datasource) {

    final Connection conn;
    try {
      conn = datasource.getConnection();
    } catch (final SQLException e) {
      LOG.error("Failed to connect to database, slashing unavailable, exiting.", e);
      throw new RuntimeException("Unable to create slashing database using supplied config");
    }

    try {
      final Statement statement = conn.createStatement();
      statement
          .execute("CREATE TABLE validators (id INTEGER PRIMARY KEY, public_key BLOB NOT NULL)");
    } catch (SQLException throwables) {
      LOG.trace("Failed to create validators table, may already exist.");
    }

    try {
      final Statement statement2 = conn.createStatement();
      statement2.execute(
          "CREATE TABLE signed_blocks ( " +
              "validator_id INTEGER NOT NULL, " +
              "slot INTEGER NOT NULL, " +
              "signing_root BLOB NOT NULL, " +
              "FOREIGN KEY(validator_id) REFERENCES validators(id) " +
              "UNIQUE (validator_id, slot))");
    } catch (SQLException throwables) {
      LOG.trace("Failed to create signed_blocks table, may already exist.");
    }

    try {
      final Statement statement3 = conn.createStatement();
      statement3.execute(
          "CREATE TABLE signed_attestations (validator_id INTEGER, source_epoch INTEGER NOT NULL, target_epoch INTEGER NOT NULL, signing_root BLOB NOT NULL, FOREIGN KEY(validator_id) REFERENCES validators(id) UNIQUE (validator_id, target_epoch))");
    } catch (SQLException throwables) {
      LOG.trace("Failed to create signed_attestations table, may already exist.");
    }
  }

  public static SlashingProtection createSlashingProtection(final DataSource datasource)
      throws SQLException {

    final Connection conn = datasource.getConnection();
    final PreparedStatement canSignBlockSqlTemplate =
        conn.prepareStatement(
            "SELECT COUNT(*) AS total FROM signed_blocks " +
                "INNER JOIN validators ON signed_blocks.validator_id=validators.id" +
                " where validators.public_key=? AND blockSlot>?;");
    final PreparedStatement addBlockSignEntrySqlTemplate =
        conn.prepareStatement(
            "INSERT INTO signed_blocks(validator_id, blockSlot, signing_root) " +
                "SELECT validators.id, ?, ?" +
                "FROM validators " +
                "WHERE validators.public_key = ?");
    final PreparedStatement canSignAttestationTemplate =
        conn.prepareStatement(
            "SELECT COUNT(*) as total from signed_attestations " +
                "INNER JOIN validators ON signed_attestations.validator_id=validators.id" +
                " where validators.public_key=? AND source_epoch<? AND target_epoch<?");
    final PreparedStatement addAttestationSignEntrySqlTemplate =
        conn.prepareStatement(
            "INSERT INTO signed_attestations (validator_id, sourceEpoch, targetEpoch, signing_root) " +
                "SELECT validators.id, ?, ?, ?" +
                "FROM validators " +
                "WHERE validators.public_key = ?");

    return new DbBackedSlashingProtection(
        canSignBlockSqlTemplate,
        addBlockSignEntrySqlTemplate,
        canSignAttestationTemplate,
        addAttestationSignEntrySqlTemplate);
  }
}
