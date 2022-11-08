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
import java.io.UncheckedIOException;
import java.util.Optional;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;

public class DatabaseUtil {
  public static final String USERNAME = "postgres";
  public static final String PASSWORD = "postgres";
  public static final String MIGRATIONS_LOCATION = "/migrations/postgresql/";
  public static final boolean DB_CONNECTION_POOL_ENABLED = true;

  public static TestDatabaseInfo create() {
    final TestDatabaseInfo testDatabaseInfo = createWithoutMigration();
    final Flyway flyway =
        Flyway.configure()
            .locations(MIGRATIONS_LOCATION)
            .dataSource(testDatabaseInfo.getDb().getPostgresDatabase())
            .load();
    flyway.migrate();
    return new TestDatabaseInfo(
        testDatabaseInfo.getDb(), testDatabaseInfo.getJdbi(), Optional.of(flyway));
  }

  public static TestDatabaseInfo createWithoutMigration() {
    try {
      final EmbeddedPostgres db = EmbeddedPostgres.start();
      final String databaseUrl =
          String.format("jdbc:postgresql://localhost:%d/postgres", db.getPort());
      final Jdbi jdbi =
          DbConnection.createConnection(
              databaseUrl,
              DatabaseUtil.USERNAME,
              DatabaseUtil.PASSWORD,
              null,
              DB_CONNECTION_POOL_ENABLED);
      return new TestDatabaseInfo(db, jdbi, Optional.empty());
    } catch (IOException e) {
      throw new UncheckedIOException("Unable to create embedded postgres database", e);
    }
  }

  public static class TestDatabaseInfo {

    private final EmbeddedPostgres db;
    private final Jdbi jdbi;
    private final Optional<Flyway> flyway;

    private TestDatabaseInfo(
        final EmbeddedPostgres db, final Jdbi jdbi, final Optional<Flyway> flyway) {
      this.db = db;
      this.jdbi = jdbi;
      this.flyway = flyway;
    }

    public EmbeddedPostgres getDb() {
      return db;
    }

    public Jdbi getJdbi() {
      return jdbi;
    }

    public Optional<Flyway> getFlyway() {
      return flyway;
    }

    public String databaseUrl() {
      return String.format("jdbc:postgresql://localhost:%d/postgres", db.getPort());
    }
  }
}
