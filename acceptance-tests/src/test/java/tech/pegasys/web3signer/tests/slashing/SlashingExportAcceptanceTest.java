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
import static tech.pegasys.web3signer.dsl.utils.Eth2RequestUtils.GENESIS_VALIDATORS_ROOT;
import static tech.pegasys.web3signer.dsl.utils.Eth2RequestUtils.createAttestationRequest;
import static tech.pegasys.web3signer.dsl.utils.WaitUtils.waitFor;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.web3signer.BLSTestUtil;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.Eth2SigningRequestBody;
import tech.pegasys.web3signer.dsl.signer.Signer;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.web3signer.signing.KeyType;
import tech.pegasys.web3signer.slashingprotection.interchange.InterchangeJsonProvider;
import tech.pegasys.web3signer.slashingprotection.interchange.model.SignedAttestation;
import tech.pegasys.web3signer.tests.AcceptanceTestBase;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import dsl.InterchangeV5Format;
import dsl.SignedArtifacts;
import io.restassured.response.Response;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class SlashingExportAcceptanceTest extends AcceptanceTestBase {

  private static final MetadataFileHelpers METADATA_FILE_HELPERS = new MetadataFileHelpers();
  public static final String DB_USERNAME = "postgres";
  public static final String DB_PASSWORD = "postgres";
  protected final BLSKeyPair keyPair = BLSTestUtil.randomKeyPair(0);

  final List<String> blockSlashingMetrics =
      List.of(
          "eth2_slashingprotection_permitted_signings",
          "eth2_slashingprotection_prevented_signings");

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
  void slashingDataIsExported(@TempDir final Path testDirectory) throws IOException {
    setupSigner(testDirectory, true);

    final Eth2SigningRequestBody request = createAttestationRequest(5, 6, UInt64.ZERO);

    final Response initialResponse = signer.eth2Sign(keyPair.getPublicKey().toString(), request);
    assertThat(initialResponse.getStatusCode()).isEqualTo(200);
    assertThat(signer.getMetricsMatching(blockSlashingMetrics))
        .containsOnly(blockSlashingMetrics.get(0) + " 1.0", blockSlashingMetrics.get(1) + " 0.0");

    final Path exportFile = testDirectory.resolve("dbExport.json");
    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withMode("eth2");
    builder.withSlashingEnabled(true);
    builder.withSlashingProtectionDbUrl(signer.getSlashingDbUrl());
    builder.withSlashingProtectionDbUsername("postgres");
    builder.withSlashingProtectionDbPassword("postgres");
    builder.withKeyStoreDirectory(testDirectory);
    builder.withSlashingExportPath(exportFile);
    builder.withHttpPort(12345); // prevent wait for Ports file in AT

    final Signer exportSigner = new Signer(builder.build(), null);
    exportSigner.start();
    waitFor(() -> assertThat(exportSigner.isRunning()).isFalse());

    final ObjectMapper mapper = new InterchangeJsonProvider().getJsonMapper();

    final InterchangeV5Format mappedData =
        mapper.readValue(exportFile.toFile(), InterchangeV5Format.class);

    assertThat(mappedData.getMetadata().getFormatVersion()).isEqualTo("5");
    assertThat(mappedData.getMetadata().getGenesisValidatorsRoot())
        .isEqualTo(Bytes.fromHexString(GENESIS_VALIDATORS_ROOT));

    assertThat(mappedData.getSignedArtifacts()).hasSize(1);
    final SignedArtifacts artifacts = mappedData.getSignedArtifacts().get(0);

    assertThat(artifacts.getSignedBlocks()).hasSize(0);

    assertThat(artifacts.getSignedAttestations()).hasSize(1);
    final SignedAttestation attestation = artifacts.getSignedAttestations().get(0);
    assertThat(attestation.getSourceEpoch().toLong())
        .isEqualTo(request.attestation().source.epoch.longValue());
    assertThat(attestation.getTargetEpoch().toLong())
        .isEqualTo(request.attestation().target.epoch.longValue());
    assertThat(attestation.getSigningRoot()).isNotNull();
  }
}
