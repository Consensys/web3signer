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
package tech.pegasys.web3signer.dsl.utils;

import java.io.IOException;
import javax.sql.DataSource;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;

public class EmbeddedDatabaseUtils {

  public static String createEmbeddedDatabase() {
    try {
      final EmbeddedPostgres slashingDatabase = EmbeddedPostgres.start();
      createSchemaInDataSource(slashingDatabase.getPostgresDatabase());
      return String.format("jdbc:postgresql://localhost:%s/postgres", slashingDatabase.getPort());
    } catch (final IOException e) {
      throw new RuntimeException("Unable to start embedded postgres db", e);
    }
  }

  private static void createSchemaInDataSource(final DataSource dataSource) {
    final Flyway flyway =
        Flyway.configure().locations("/migrations/postgresql/").dataSource(dataSource).load();
    flyway.migrate();
  }
}
