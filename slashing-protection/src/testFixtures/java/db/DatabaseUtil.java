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
  private static final String USERNAME = "postgres";
  private static final String PASSWORD = "postgres";

  public static Jdbi setup() throws IOException {
    final EmbeddedPostgres db = EmbeddedPostgres.start();
    final Flyway flyway =
        Flyway.configure()
            .locations("/migrations/postgresql/")
            .dataSource(db.getPostgresDatabase())
            .load();
    flyway.migrate();

    final String databaseUrl =
        String.format("jdbc:postgresql://localhost:%d/postgres", db.getPort());
    return DbConnection.createConnection(databaseUrl, USERNAME, PASSWORD, null);
  }
}
