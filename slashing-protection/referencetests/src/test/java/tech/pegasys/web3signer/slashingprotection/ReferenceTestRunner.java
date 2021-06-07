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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import tech.pegasys.web3signer.slashingprotection.dao.MetadataDao;
import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;
import tech.pegasys.web3signer.slashingprotection.interchange.InterchangeModule;
import tech.pegasys.web3signer.slashingprotection.model.AttestationTestModel;
import tech.pegasys.web3signer.slashingprotection.model.BlockTestModel;
import tech.pegasys.web3signer.slashingprotection.model.Step;
import tech.pegasys.web3signer.slashingprotection.model.TestFileModel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import dsl.SignedArtifacts;
import dsl.TestSlashingProtectionParameters;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class ReferenceTestRunner {

  private static final Logger LOG = LogManager.getLogger();

  private static final String USERNAME = "postgres";
  private static final String PASSWORD = "postgres";

  private static final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new InterchangeModule())
          .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
  private final ValidatorsDao validators = new ValidatorsDao();

  private EmbeddedPostgres slashingDatabase;
  private String databaseUrl;
  private SlashingProtection slashingProtection;
  private Jdbi jdbi;

  public void setup() throws IOException {
    slashingDatabase = EmbeddedPostgres.start();
    final Flyway flyway =
        Flyway.configure()
            .locations("/migrations/postgresql/")
            .dataSource(slashingDatabase.getPostgresDatabase())
            .load();
    flyway.migrate();
    databaseUrl =
        String.format("jdbc:postgresql://localhost:%d/postgres", slashingDatabase.getPort());
    final SlashingProtectionParameters slashingProtectionParameters =
        new TestSlashingProtectionParameters(databaseUrl, USERNAME, PASSWORD);
    slashingProtection =
        SlashingProtectionFactory.createSlashingProtection(slashingProtectionParameters);
    jdbi =
        DbConnection.createConnection(
            slashingProtectionParameters.getDbUrl(),
            slashingProtectionParameters.getDbUsername(),
            slashingProtectionParameters.getDbPassword(),
            slashingProtectionParameters.getDbPoolConfigurationFile());
  }

  public void cleanup() {
    try {
      slashingDatabase.close();
    } catch (final IOException e) {
      LOG.error("Failed to close database", e);
    }
  }

  @TestFactory
  public Collection<DynamicTest> executeEachReferenceTestFile() {
    final URL refTestPath =
        Resources.getResource(
            Path.of("slashing-protection-interchange-tests", "tests", "generated").toString());
    final Path testFilesPath = Path.of(refTestPath.getPath());

    try {
      try (final Stream<Path> files = Files.list(testFilesPath)) {
        return files
            .map(tf -> DynamicTest.dynamicTest(tf.getFileName().toString(), () -> executeFile(tf)))
            .collect(Collectors.toList());
      }
    } catch (final IOException e) {
      throw new RuntimeException("Failed to create dynamic tests", e);
    }
  }

  private void executeFile(final Path inputFile) throws IOException {
    setup();
    try {
      final TestFileModel model = objectMapper.readValue(inputFile.toFile(), TestFileModel.class);

      for (final Step step : model.getSteps()) {
        final String interchangeContent =
            objectMapper.writeValueAsString(step.getInterchangeContent());

        final Bytes32 gvr = Bytes32.fromHexString(model.getGenesis_validators_root());

        jdbi.useHandle(
            h -> {
              final MetadataDao metadataDao = new MetadataDao();
              if (metadataDao.findGenesisValidatorsRoot(h).isEmpty()) {
                metadataDao.insertGenesisValidatorsRoot(h, gvr);
              }
            });

        // web3signer doesn't allow for partial imports, so - if it is expected, then
        // expect import to throw.
        if (step.isShouldSucceed()) {
          slashingProtection.importData(
              new ByteArrayInputStream(interchangeContent.getBytes(UTF_8)));
          verifyImport(step, gvr);
        } else {
          assertThatThrownBy(
                  () ->
                      slashingProtection.importData(
                          new ByteArrayInputStream(interchangeContent.getBytes(UTF_8))))
              .isInstanceOf(RuntimeException.class);
        }
      }
    } finally {
      cleanup();
    }
  }

  private void verifyImport(final Step step, final Bytes32 gvr) {
    final List<String> validatorsInModel =
        step.getInterchangeContent().getSignedArtifacts().stream()
            .map(SignedArtifacts::getPublicKey)
            .distinct()
            .collect(Collectors.toList());

    final List<String> publicKeysInDb = getValidatorPublicKeysFromDb();

    assertThat(validatorsInModel).containsExactlyInAnyOrderElementsOf(publicKeysInDb);

    // need to register the validators with slashingProtection before testing blocks/attestations
    slashingProtection.registerValidators(
        validatorsInModel.stream().map(Bytes::fromHexString).collect(Collectors.toList()));

    validateAttestations(step.getAttestations(), gvr);
    validateBlocks(step.getBlocks(), gvr);
  }

  private List<String> getValidatorPublicKeysFromDb() {

    return jdbi.withHandle(
        h ->
            validators
                .findAllValidators(h)
                .map(v -> v.getPublicKey().toHexString())
                .collect(Collectors.toList()));
  }

  private void validateAttestations(
      final List<AttestationTestModel> attestations, final Bytes32 gvr) {
    for (int i = 0; i < attestations.size(); i++) {
      final AttestationTestModel attestation = attestations.get(i);
      final boolean result =
          slashingProtection.maySignAttestation(
              attestation.getPublickKey(),
              attestation.getSigningRoot(),
              attestation.getSourceEpoch(),
              attestation.getTargetEpoch(),
              gvr);
      if (!attestation.isShouldSucceed()) {
        assertThat(result).isFalse();
      } else if (!result) {
        LOG.warn(
            "Detected a valid signing condition which was rejected by slashing-protection (attestation {} for validator {})",
            i,
            attestation.getPublickKey());
      }
    }
  }

  private void validateBlocks(final List<BlockTestModel> blocks, final Bytes32 gvr) {

    for (int i = 0; i < blocks.size(); i++) {
      final BlockTestModel block = blocks.get(i);
      final boolean result =
          slashingProtection.maySignBlock(
              block.getPublickKey(), block.getSigningRoot(), block.getSlot(), gvr);
      // NOTE: Ref tests are only fail if you sign something which is flagged as "!should_Succeed".
      // (i.e. a "reject" response is always correct)
      if (!block.isShouldSucceed()) {
        assertThat(result).isFalse();
      } else if (!result) {
        LOG.warn(
            "Detected a valid signing condition which was rejected by slashing-protection (block {} for validator {})",
            i,
            block.getPublickKey());
      }
    }
  }
}
