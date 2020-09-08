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

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.flywaydb.core.Flyway;

public class DbBackedSlashingProtectionFactory {

  private static final Logger LOG = LogManager.getLogger();

  public static SlashingProtection createDbBackedSlashingProtection(
      final String url, final String username, final String password) {
    final DataSource dataSource = createDataSource(url, username, password);

    migrateToLatestSchema(dataSource);

    try {
      return createSlashingProtection(dataSource);
    } catch (final SQLException e) {
      throw new InitializationException(
          "Failed to generate PreparedStatements - potential schema mismatch.", e);
    }
  }

  private static DataSource createDataSource(
      final String url, final String username, final String password) {
    final ComboPooledDataSource dataSource = new ComboPooledDataSource();
    dataSource.setUser(username);
    dataSource.setPassword(password);
    dataSource.setJdbcUrl(url);
    dataSource.setAcquireRetryAttempts(3);

    return dataSource;
  }

  private static void migrateToLatestSchema(final DataSource datasource) {
    try {
      final Flyway flyway = Flyway.configure().dataSource(datasource).load();
      flyway.migrate();
    } catch (final Exception e) {
      LOG.error("Failed to migrate the database");
      throw new InitializationException("Failed to migrate the database");
    }
  }

  private static SlashingProtection createSlashingProtection(final DataSource datasource)
      throws SQLException {

    final Connection conn = datasource.getConnection();
    final PreparedStatement canSignBlockSqlTemplate =
        conn.prepareStatement(
            "SELECT slot, signing_root "
                + "FROM signed_blocks "
                + "INNER JOIN validators ON signed_blocks.validator_id=validators.id"
                + " where validators.public_key=? AND slot=?;");
    final PreparedStatement addBlockSignEntrySqlTemplate =
        conn.prepareStatement(
            "INSERT INTO signed_blocks(validator_id, slot, signing_root) "
                + "SELECT validators.id, ?, ?"
                + "FROM validators "
                + "WHERE validators.public_key = ?");
    final PreparedStatement canSignAttestationTemplate =
        conn.prepareStatement(
            "SELECT source_epoch, target_epoch, signing_root "
                + "FROM signed_attestations "
                + "INNER JOIN validators ON signed_attestations.validator_id=validators.id "
                + "where validators.public_key=? AND target_epoch=?");
    final PreparedStatement addAttestationSignEntrySqlTemplate =
        conn.prepareStatement(
            "INSERT INTO signed_attestations (validator_id, source_epoch, target_epoch, signing_root) "
                + "SELECT validators.id, ?, ?, ?"
                + "FROM validators "
                + "WHERE validators.public_key = ?");

    return new DbBackedSlashingProtection(
        canSignBlockSqlTemplate,
        addBlockSignEntrySqlTemplate,
        canSignAttestationTemplate,
        addAttestationSignEntrySqlTemplate);
  }
}
