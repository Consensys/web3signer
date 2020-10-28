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

import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestation;
import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestationsDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlock;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlocksDao;
import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;
import tech.pegasys.web3signer.slashingprotection.interchange.InterchangeModule;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import dsl.InterchangeV5Format;
import dsl.SignedArtifacts;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

public class InterchangeExportIntegrationTest {

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
  void canCreateDatabaseWithEntries() throws IOException, URISyntaxException {
    final EmbeddedPostgres db = setup();

    final String databaseUrl =
        String.format("jdbc:postgresql://localhost:%d/postgres", db.getPort());

    final Jdbi jdbi = DbConnection.createConnection(databaseUrl, "postgres", "postgres");

    final int VALIDATOR_COUNT = 2;
    final int TOTAL_BLOCKS_SIGNED = 6;
    final int TOTAL_ATTESTATIONS_SIGNED = 8;

    for (int i = 0; i < VALIDATOR_COUNT; i++) {
      final int validatorId = i + 1;
      final Bytes validatorPublicKey = Bytes.of(validatorId);
      jdbi.useTransaction(h -> validators.registerValidators(h, List.of(validatorPublicKey)));
      jdbi.useTransaction(
          h -> {
            for (int b = 0; b < TOTAL_BLOCKS_SIGNED; b++) {
              signedBlocks.insertBlockProposal(
                  h, new SignedBlock(validatorId, UInt64.valueOf(b), Bytes.fromHexString("0x01")));
            }
          });
      jdbi.useTransaction(
          h -> {
            for (int a = 0; a < TOTAL_ATTESTATIONS_SIGNED; a++) {
              signedAttestations.insertAttestation(
                  h,
                  new SignedAttestation(
                      validatorId,
                      UInt64.valueOf(a),
                      UInt64.valueOf(a),
                      Bytes.fromHexString("0x01")));
            }
          });
    }

    final OutputStream exportOutput = new ByteArrayOutputStream();
    final SlashingProtection slashingProtection =
        SlashingProtectionFactory.createSlashingProtection(databaseUrl, "postgres", "postgres");
    slashingProtection.export(exportOutput);
    exportOutput.close();

    final ObjectMapper mapper = new ObjectMapper().registerModule(new InterchangeModule());

    final InterchangeV5Format outputObject =
        mapper.readValue(exportOutput.toString(), InterchangeV5Format.class);

    assertThat(outputObject.getMetadata().getFormatVersionAsString()).isEqualTo("5");
    assertThat(outputObject.getMetadata().getGenesisValidatorsRoot())
        .isEqualTo(Bytes.fromHexString("FFFFFFFF"));

    final List<SignedArtifacts> signedArtifacts = outputObject.getSignedArtifacts();
    assertThat(signedArtifacts).hasSize(2);
    for (int i = 0; i < VALIDATOR_COUNT; i++) {
      final int validatorId = i + 1;
      final SignedArtifacts signedArtifact = signedArtifacts.get(i);
      assertThat(signedArtifact.getPublicKey()).isEqualTo(String.format("0x0%x", validatorId));
      assertThat(signedArtifact.getSignedBlocks()).hasSize(TOTAL_BLOCKS_SIGNED);
      for (int b = 0; b < TOTAL_BLOCKS_SIGNED; b++) {
        final tech.pegasys.web3signer.slashingprotection.interchange.model.SignedBlock block =
            signedArtifact.getSignedBlocks().get(b);
        assertThat(block.getSigningRoot()).isEqualTo(Bytes.fromHexString("0x01"));
        assertThat(block.getSlot()).isEqualTo(UInt64.valueOf(b));
      }

      assertThat(signedArtifact.getSignedAttestations()).hasSize(TOTAL_ATTESTATIONS_SIGNED);
      for (int a = 0; a < TOTAL_ATTESTATIONS_SIGNED; a++) {
        final tech.pegasys.web3signer.slashingprotection.interchange.model.SignedAttestation
            attestation = signedArtifact.getSignedAttestations().get(a);
        assertThat(attestation.getSigningRoot()).isEqualTo(Bytes.fromHexString("0x01"));
        assertThat(attestation.getSourceEpoch()).isEqualTo(UInt64.valueOf(a));
        assertThat(attestation.getTargetEpoch()).isEqualTo(UInt64.valueOf(a));
      }
    }
  }
}
