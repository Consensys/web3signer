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
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DbBackedSlashingProtectionFactoryTest {

  @Test
  void dataBaseIsCreatedWithAppropriateTablesWhenInitialised(@TempDir Path testDir)
      throws SQLException {
    final Path dbPath = testDir.toAbsolutePath().resolve("test.db");

    assertThat(dbPath.toFile().exists()).isFalse();
    final String sqlLitePath = "jdbc:sqlite:" + dbPath.toString();

    DbBackedSlashingProtectionFactory.createDbBackedSlashingProtection(sqlLitePath, null, null);

    assertThat(dbPath.toFile().exists()).isTrue();

    final Connection conn = DriverManager.getConnection(sqlLitePath);

    final ResultSet rs =
        conn.createStatement().executeQuery("SELECT name FROM sqlite_master WHERE type='table';");

    final List<String> tableNames = Lists.newArrayList();
    while (rs.next()) {
      tableNames.add(rs.getString(1));

    }

    assertThat(tableNames).containsOnly("flyway_schema_history", "validators", "signed_blocks",
        "signed_attestations");
  }

}