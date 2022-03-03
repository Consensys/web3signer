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

import db.DatabaseSetupExtension;
import org.jdbi.v3.core.Handle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DatabaseSetupExtension.class)
public class TestDatabaseInfoVersionDaoTest {

  private final DatabaseVersionDao databaseVersionDao = new DatabaseVersionDao();

  @Test
  public void migratedDatabaseReturnsValue(final Handle handle) {
    final int version = databaseVersionDao.findDatabaseVersion(handle);
    assertThat(version).isEqualTo(DatabaseVersionDao.EXPECTED_DATABASE_VERSION);
  }

  @Test
  public void missingTableThrowsException(final Handle handle) {
    handle.execute("DROP TABLE database_version");
    assertThatThrownBy(() -> databaseVersionDao.findDatabaseVersion(handle))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void missingEntryInTableThrowsException(final Handle handle) {
    handle.execute("DELETE FROM database_version");
    assertThatThrownBy(() -> databaseVersionDao.findDatabaseVersion(handle))
        .isInstanceOf(IllegalStateException.class);
  }
}
