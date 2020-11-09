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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
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
    final Path migrationPath =
        getProjectPath()
            .toPath()
            .resolve(
                Path.of(
                    "slashing-protection", "src", "main", "resources", "migrations", "postgresql"));

    final Flyway flyway =
        Flyway.configure()
            .locations("filesystem:" + migrationPath.toString())
            .dataSource(dataSource)
            .load();
    flyway.migrate();
  }

  protected static File getProjectPath() {
    // For gatling the pwd is actually the web3signer directory for other tasks this a lower dir
    final String userDir = System.getProperty("user.dir");
    return userDir.toLowerCase().endsWith("web3signer")
        ? new File(userDir)
        : new File(userDir).getParentFile();
  }
}
