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
package tech.pegasys.web3signer.slashingprotection.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import tech.pegasys.web3signer.slashingprotection.DbConnection;
import tech.pegasys.web3signer.slashingprotection.SlashingProtectionFactory;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.testing.JdbiRule;
import org.jdbi.v3.testing.Migration;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class DatabaseVersionDaoTest {

  @Rule
  public JdbiRule postgres =
      JdbiRule.embeddedPostgres()
          .withMigration(Migration.before().withPath("migrations/postgresql"));

  private final DatabaseVersionDao databaseVersionDao = new DatabaseVersionDao();
  private Handle handle;

  @Before
  public void setup() {
    DbConnection.configureJdbi(postgres.getJdbi());
    handle = postgres.getJdbi().open();
  }

  @After
  public void cleanup() {
    handle.close();
  }

  @Test
  public void migratedDatabaseReturnsValue() {
    final int version = databaseVersionDao.findDatabaseVersion(handle);
    assertThat(version).isEqualTo(SlashingProtectionFactory.EXPECTED_DATABASE_VERSION);
  }

  @Test
  public void missingTableThrowsException() {
    handle.execute("DROP TABLE database_version");
    assertThatThrownBy(() -> databaseVersionDao.findDatabaseVersion(handle))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void missingEntryInTableThrowsException() {
    handle.execute("DELETE FROM database_version");
    assertThatThrownBy(() -> databaseVersionDao.findDatabaseVersion(handle))
        .isInstanceOf(IllegalStateException.class);
  }
}
