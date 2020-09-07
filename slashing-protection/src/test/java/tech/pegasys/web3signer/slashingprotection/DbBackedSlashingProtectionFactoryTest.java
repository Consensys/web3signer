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
package tech.pegasys.web3signer.slashingprotection;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.assertj.db.type.Table;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DbBackedSlashingProtectionFactoryTest {

  @Test
  void dataBaseIsCreatedWithAppropriateTablesWhenInitialised(@TempDir Path testDir)
      throws SQLException {
    final String jdbcUrl = createSqliteDatabase(testDir);
    final Connection conn = DriverManager.getConnection(jdbcUrl);

    final ResultSet rs =
        conn.createStatement().executeQuery("SELECT name FROM sqlite_master WHERE type='table';");

    final List<String> tableNames = Lists.newArrayList();
    while (rs.next()) {
      tableNames.add(rs.getString(1));
    }

    assertThat(tableNames).containsOnly("flyway_schema_history", "validators", "signed_blocks",
        "signed_attestations");
  }

  @Test
  void migratingADatabaseDoesNotImpactItsContent(@TempDir Path testDir) throws SQLException {
    final String jdbcUrl = createSqliteDatabase(testDir);
    final Connection conn = DriverManager.getConnection(jdbcUrl);
    final ComboPooledDataSource dataSource = new ComboPooledDataSource();
    dataSource.setJdbcUrl(jdbcUrl);

    //insert data
    final int rowsInserted =
        conn.createStatement()
            .executeUpdate("INSERT INTO validators(id, public_key) VALUES (1, \"abcd\")");

    assertThat(rowsInserted).isOne();

    //attempt to create database again
    createSqliteDatabase(testDir);

    final Table validatorTable = new Table(dataSource, "validators");

    org.assertj.db.api.Assertions.assertThat(validatorTable).hasNumberOfRows(1);
    org.assertj.db.api.Assertions.assertThat(validatorTable).column("id").containsValues(1);
    org.assertj.db.api.Assertions.assertThat(validatorTable).column("public_key")
        .containsValues("abcd");
  }

  // Returns the JDBC URL of the database
  private String createSqliteDatabase(final Path rootDir) {
    final Path dbPath = rootDir.toAbsolutePath().resolve("test.db");

    final String sqlLitePath = "jdbc:sqlite:" + dbPath.toString();

    DbBackedSlashingProtectionFactory.createDbBackedSlashingProtection(sqlLitePath, null, null);
    assertThat(dbPath.toFile().exists()).isTrue();

    return sqlLitePath;
  }
}