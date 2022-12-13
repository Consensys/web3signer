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
package tech.pegasys.web3signer.tests.slashing;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.web3signer.dsl.utils.WaitUtils.waitFor;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.web3signer.BLSTestUtil;
import tech.pegasys.web3signer.dsl.signer.Signer;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.web3signer.signing.KeyType;
import tech.pegasys.web3signer.slashingprotection.interchange.InterchangeJsonProvider;
import tech.pegasys.web3signer.slashingprotection.interchange.model.SignedAttestation;
import tech.pegasys.web3signer.slashingprotection.interchange.model.SignedBlock;
import tech.pegasys.web3signer.tests.AcceptanceTestBase;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.google.common.io.Resources;
import dsl.InterchangeV5Format;
import dsl.SignedArtifacts;
import org.apache.tuweni.bytes.Bytes;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class SlashingImportAcceptanceTest extends AcceptanceTestBase {

  private static final MetadataFileHelpers METADATA_FILE_HELPERS = new MetadataFileHelpers();
  public static final String DB_USERNAME = "postgres";
  public static final String DB_PASSWORD = "postgres";

  private static final com.fasterxml.jackson.databind.ObjectMapper OBJECT_MAPPER =
      new InterchangeJsonProvider().getJsonMapper();

  protected final BLSKeyPair keyPair = BLSTestUtil.randomKeyPair(0);

  void setupSigner(final Path testDirectory, final boolean enableSlashing) {
    final SignerConfigurationBuilder builder =
        new SignerConfigurationBuilder()
            .withMode("eth2")
            .withSlashingEnabled(enableSlashing)
            .withSlashingProtectionDbUsername(DB_USERNAME)
            .withSlashingProtectionDbPassword(DB_PASSWORD)
            .withMetricsEnabled(true)
            .withKeyStoreDirectory(testDirectory);

    final Path keyConfigFile = testDirectory.resolve("keyfile.yaml");
    METADATA_FILE_HELPERS.createUnencryptedYamlFileAt(
        keyConfigFile, keyPair.getSecretKey().toBytes().toHexString(), KeyType.BLS);

    startSigner(builder.build());
  }

  @Test
  void slashingDataIsImported(@TempDir final Path testDirectory)
      throws URISyntaxException, IOException {
    setupSigner(testDirectory, true);

    final Path importFile =
        new File(Resources.getResource("slashing/slashingImport.json").toURI()).toPath();

    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withMode("eth2");
    builder.withSlashingEnabled(true);
    builder.withSlashingProtectionDbUrl(signer.getSlashingDbUrl());
    builder.withSlashingProtectionDbUsername("postgres");
    builder.withSlashingProtectionDbPassword("postgres");
    builder.withKeyStoreDirectory(testDirectory);
    builder.withSlashingImportPath(importFile);
    builder.withHttpPort(12345); // prevent wait for Ports file in AT

    final Signer importSigner = new Signer(builder.build(), null);
    importSigner.start();
    waitFor(() -> assertThat(importSigner.isRunning()).isFalse());

    final InterchangeV5Format interchangeData =
        OBJECT_MAPPER.readValue(importFile.toFile(), InterchangeV5Format.class);

    final Jdbi jdbi = Jdbi.create(signer.getSlashingDbUrl(), DB_USERNAME, DB_PASSWORD);
    final int validatorId = 1;

    final Map<String, Object> metadata =
        jdbi.withHandle(h -> h.select("SELECT * from metadata").mapToMap().one());
    assertThat(metadata.get("id")).isEqualTo(validatorId);
    assertThat(metadata.get("genesis_validators_root"))
        .isEqualTo(interchangeData.getMetadata().getGenesisValidatorsRoot().toArray());

    final SignedArtifacts artifacts = interchangeData.getSignedArtifacts().get(0);

    final List<Map<String, Object>> validators =
        jdbi.withHandle(h -> h.select("SELECT * from validators").mapToMap().list());
    assertThat(validators).hasSize(1);
    assertThat(validators.get(0).get("id")).isEqualTo(validatorId);
    assertThat(validators.get(0).get("public_key"))
        .isEqualTo(Bytes.fromHexString(artifacts.getPublicKey()).toArray());

    final List<Map<String, Object>> signedAttestations =
        jdbi.withHandle(h -> h.select("SELECT * from signed_attestations").mapToMap().list());
    final SignedAttestation attestation = artifacts.getSignedAttestations().get(0);
    assertThat(signedAttestations).hasSize(1);
    assertThat(signedAttestations.get(0).get("validator_id")).isEqualTo(validatorId);
    assertThat(signedAttestations.get(0).get("source_epoch"))
        .isEqualTo(BigDecimal.valueOf(attestation.getSourceEpoch().toLong()));
    assertThat(signedAttestations.get(0).get("target_epoch"))
        .isEqualTo(BigDecimal.valueOf(attestation.getTargetEpoch().toLong()));
    assertThat(signedAttestations.get(0).get("signing_root"))
        .isEqualTo(attestation.getSigningRoot().toArray());

    final List<Map<String, Object>> signedBlocks =
        jdbi.withHandle(h -> h.select("SELECT * from signed_blocks").mapToMap().list());
    final SignedBlock block = artifacts.getSignedBlocks().get(0);
    assertThat(signedBlocks).hasSize(1);
    assertThat(signedBlocks.get(0).get("validator_id")).isEqualTo(validatorId);
    assertThat(signedBlocks.get(0).get("slot"))
        .isEqualTo(BigDecimal.valueOf(block.getSlot().toLong()));
    assertThat(signedBlocks.get(0).get("signing_root")).isEqualTo(block.getSigningRoot().toArray());
  }
}
