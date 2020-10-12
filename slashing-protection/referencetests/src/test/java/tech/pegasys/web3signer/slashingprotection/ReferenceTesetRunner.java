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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.apache.tuweni.bytes.Bytes;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;
import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestationsDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlock;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlocksDao;
import tech.pegasys.web3signer.slashingprotection.dao.Validator;
import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;
import tech.pegasys.web3signer.slashingprotection.model.TestFileModel;

public class ReferenceTesetRunner {

  final ObjectMapper objectMapper = new ObjectMapper();
  private final SignedBlocksDao signedBlocks = new SignedBlocksDao();
  private final SignedAttestationsDao signedAttestations = new SignedAttestationsDao();
  private final ValidatorsDao validators = new ValidatorsDao();


  private EmbeddedPostgres setup() throws IOException, URISyntaxException {
    final EmbeddedPostgres slashingDatabase = EmbeddedPostgres.start();

    final String migrationsFile = Path.of("migrations", "postgresql", "V1__initial.sql").toString();

    final Path schemaPath = Paths.get(Resources.getResource(migrationsFile).toURI());

    final Path migrationPath = schemaPath.getParent();

    final Flyway flyway =
        Flyway.configure()
            .locations("filesystem:" + migrationPath.toString())
            .dataSource(slashingDatabase.getPostgresDatabase())
            .load();
    flyway.migrate();

    return slashingDatabase;
  }


  @Test
  void testExportMatchesInterchangeContent() throws IOException {
    final URL refTestPath = Resources.getResource(
        Path.of("slashing-protection-interchange-tests", "tests", "generated").toString());

    final Path testFilesPath = Path.of(refTestPath.getPath());

    try (final Stream<Path> fileStream = Files.list(testFilesPath)) {
      fileStream.forEach(path -> {
        System.out.println("File = " + path.toString());
        try {
          final TestFileModel model = objectMapper.readValue(path.toFile(), TestFileModel.class);
          final String interchangeContent =
              objectMapper.writeValueAsString(model.getInterchangeContent());

          final EmbeddedPostgres db = setup();
          final String databaseUrl =
              String.format("jdbc:postgresql://localhost:%d/postgres", db.getPort());
          final SlashingProtection slashingProtection =
              SlashingProtectionFactory
                  .createSlashingProtection(databaseUrl, "postgres", "postgres");

          final Jdbi jdbi = DbConnection.createConnection(databaseUrl, "postgres", "postgres");
          model.getBlocks().forEach(block -> {
            jdbi.useTransaction(h -> {
              final Validator v =
                  validators.insertIfNotExist(h, Bytes.fromHexString(block.getPublickKey()));
              signedBlocksDao.insertBlockProposal(h, new SignedBlock(v.getId(), block.getSlot(), block.get));
            });

          })


        } catch (IOException | URISyntaxException e) {
          e.printStackTrace();
          throw new RuntimeException("setup failed for test");
        }

      });


    }
  }

}
