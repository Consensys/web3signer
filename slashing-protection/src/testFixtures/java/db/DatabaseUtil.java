/*
 * Copyright 2022 ConsenSys AG.
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
package db;

import tech.pegasys.web3signer.slashingprotection.DbConnection;

import java.io.IOException;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;

public class DatabaseUtil {
  public static final String USERNAME = "postgres";
  public static final String PASSWORD = "postgres";

  public static TestDatabaseInfo create() {
    try {
      final EmbeddedPostgres db = EmbeddedPostgres.start();
      final Flyway flyway =
          Flyway.configure()
              .locations("/migrations/postgresql/")
              .dataSource(db.getPostgresDatabase())
              .load();
      flyway.migrate();

      final String databaseUrl =
          String.format("jdbc:postgresql://localhost:%d/postgres", db.getPort());
      final Jdbi jdbi =
          DbConnection.createConnection(
              databaseUrl, DatabaseUtil.USERNAME, DatabaseUtil.PASSWORD, null);
      return new TestDatabaseInfo(db, jdbi);
    } catch (IOException e) {
      throw new RuntimeException("Unable to create embedded postgres database", e);
    }
  }

  public static class TestDatabaseInfo {

    public final EmbeddedPostgres db;
    public final Jdbi jdbi;

    private TestDatabaseInfo(final EmbeddedPostgres db, final Jdbi jdbi) {
      this.db = db;
      this.jdbi = jdbi;
    }
  }
}
