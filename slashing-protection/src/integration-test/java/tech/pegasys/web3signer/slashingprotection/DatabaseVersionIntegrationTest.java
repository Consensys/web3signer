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

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.web3signer.slashingprotection.dao.DatabaseVersionDao;

import java.io.IOException;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

public class DatabaseVersionIntegrationTest {

  public static final String DB_USERNAME = "postgres";
  public static final String DB_PASSWORD = "postgres";

  @Test
  void ensureDatabaseVersionMatchesNumberOfMigrations() throws IOException {
    final EmbeddedPostgres slashingDatabase = EmbeddedPostgres.start();
    final String dbUrl =
        String.format("jdbc:postgresql://localhost:%s/postgres", slashingDatabase.getPort());

    final Flyway flyway =
        Flyway.configure()
            .locations("/migrations/postgresql/")
            .dataSource(slashingDatabase.getPostgresDatabase())
            .load();

    final int countMigrations = flyway.info().pending().length;

    flyway.migrate();

    final DatabaseVersionDao databaseVersionDao = new DatabaseVersionDao();
    final Jdbi jdbi = Jdbi.create(dbUrl, DB_USERNAME, DB_PASSWORD);

    final int reportedVersion = jdbi.withHandle(databaseVersionDao::findDatabaseVersion);

    assertThat(reportedVersion).isEqualTo(countMigrations);
  }
}
