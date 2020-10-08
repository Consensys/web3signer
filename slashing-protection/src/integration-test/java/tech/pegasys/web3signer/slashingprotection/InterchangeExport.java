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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestationsDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlock;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlocksDao;
import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;
import tech.pegasys.web3signer.slashingprotection.interchange.model.InterchangeV4Format;

public class InterchangeExport {

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
  void canCreateDatabaseWithEntries(@TempDir Path testDir) throws IOException, URISyntaxException {
    final EmbeddedPostgres db = setup();

    final String databaseUrl =
        String.format("jdbc:postgresql://localhost:%d/postgres", db.getPort());

    final Jdbi jdbi = DbConnection.createConnection(databaseUrl, "postgres", "postgres");

    jdbi.useTransaction(
        h -> validators.registerValidators(h, List.of(Bytes.fromHexString("0x01"))));

    jdbi.useTransaction(h -> {
      for (int i = 0; i < 5; i++) {
        signedBlocks.insertBlockProposal(h,
            new SignedBlock(1, UInt64.valueOf(i), Bytes.fromHexString("0x01")));
      }
    });

    final OutputStream exportOutput = new ByteArrayOutputStream();
    final SlashingProtection slashingProtection = SlashingProtectionFactory
        .createSlashingProtection(databaseUrl, "postgres", "postgres");
    slashingProtection.exportTo(exportOutput);
    exportOutput.close();

    final ObjectMapper mapper = new ObjectMapper();

    final InterchangeV4Format outputObject =
        mapper.readValue(exportOutput.toString(), InterchangeV4Format.class);
  }
//
//  @Test
//  public void testSlashing() {
//    final EmbeddedPostgres db = setup();
//    final String databaseUrl =
//        String.format("jdbc:postgresql://localhost:%d/postgres", db.getPort());
//    final SlashingProtection slashingProtection = SlashingProtectionFactory
//        .createSlashingProtection(databaseUrl, "postgres", "postgres");
//    final Jdbi jdbi = DbConnection.createConnection(
//        slashingProtectionDbUrl, slashingProtectionDbUser, slashingProtectionDbPassword);
//
//    jdbi.insertcrap();
//
//    prot.maySignAttestation()
//  }
}
