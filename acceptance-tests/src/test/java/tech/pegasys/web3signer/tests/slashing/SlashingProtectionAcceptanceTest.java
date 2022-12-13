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
import static tech.pegasys.web3signer.dsl.utils.Eth2RequestUtils.createAttestationRequest;
import static tech.pegasys.web3signer.dsl.utils.Eth2RequestUtils.createBlockRequest;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.web3signer.BLSTestUtil;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.Eth2SigningRequestBody;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.web3signer.signing.KeyType;
import tech.pegasys.web3signer.tests.AcceptanceTestBase;

import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.response.Response;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class SlashingProtectionAcceptanceTest extends AcceptanceTestBase {

  private static final MetadataFileHelpers METADATA_FILE_HELPERS = new MetadataFileHelpers();
  public static final String DB_USERNAME = "postgres";
  public static final String DB_PASSWORD = "postgres";
  protected final BLSKeyPair keyPair = BLSTestUtil.randomKeyPair(0);
  static final int SLASHING_PROTECTION_ENFORCED = 412;

  final List<String> attestationSlashingMetrics =
      List.of(
          "eth2_slashingprotection_permitted_signings",
          "eth2_slashingprotection_prevented_signings");

  final List<String> blockSlashingMetrics =
      List.of(
          "eth2_slashingprotection_permitted_signings",
          "eth2_slashingprotection_prevented_signings");

  void setupSigner(final Path testDirectory) {
    setupSigner(testDirectory, true, true);
  }

  void setupSigner(
      final Path testDirectory,
      final boolean enableSlashing,
      final boolean slashingProtectionDbConnectionPoolEnabled) {
    final SignerConfigurationBuilder builder =
        new SignerConfigurationBuilder()
            .withMetricsCategories("ETH2_SLASHING_PROTECTION")
            .withMode("eth2")
            .withSlashingEnabled(enableSlashing)
            .withSlashingProtectionDbUsername(DB_USERNAME)
            .withSlashingProtectionDbPassword(DB_PASSWORD)
            .withSlashingProtectionDbConnectionPoolEnabled(
                slashingProtectionDbConnectionPoolEnabled)
            .withMetricsEnabled(true)
            .withNetwork("minimal")
            .withKeyStoreDirectory(testDirectory);

    final Path keyConfigFile = testDirectory.resolve("keyfile.yaml");
    METADATA_FILE_HELPERS.createUnencryptedYamlFileAt(
        keyConfigFile, keyPair.getSecretKey().toBytes().toHexString(), KeyType.BLS);

    startSigner(builder.build());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void canSignSameAttestationTwiceWhenSlashingIsEnabled(
      final boolean dbConnectionPoolEnabled, @TempDir final Path testDirectory)
      throws JsonProcessingException {

    setupSigner(testDirectory, true, dbConnectionPoolEnabled);

    final Eth2SigningRequestBody request = createAttestationRequest(5, 6, UInt64.ZERO);

    final Response initialResponse = signer.eth2Sign(keyPair.getPublicKey().toString(), request);
    assertThat(initialResponse.getStatusCode()).isEqualTo(200);

    assertThat(signer.getMetricsMatching(attestationSlashingMetrics))
        .containsOnly(
            attestationSlashingMetrics.get(0) + " 1.0", attestationSlashingMetrics.get(1) + " 0.0");

    final Response secondResponse = signer.eth2Sign(keyPair.getPublicKey().toString(), request);
    assertThat(secondResponse.getStatusCode()).isEqualTo(200);

    assertThat(signer.getMetricsMatching(attestationSlashingMetrics))
        .containsOnly(
            attestationSlashingMetrics.get(0) + " 2.0", attestationSlashingMetrics.get(1) + " 0.0");
  }

  @Test
  void cannotSignASecondAttestationForSameSlotWithDifferentSigningRoot(
      @TempDir final Path testDirectory) throws JsonProcessingException {
    setupSigner(testDirectory);

    final Eth2SigningRequestBody initialRequest = createAttestationRequest(5, 6, UInt64.ZERO);

    final Response initialResponse =
        signer.eth2Sign(keyPair.getPublicKey().toString(), initialRequest);
    assertThat(initialResponse.getStatusCode()).isEqualTo(200);

    assertThat(signer.getMetricsMatching(attestationSlashingMetrics))
        .containsOnly(
            attestationSlashingMetrics.get(0) + " 1.0", attestationSlashingMetrics.get(1) + " 0.0");

    final Eth2SigningRequestBody secondRequest = createAttestationRequest(5, 6, UInt64.ONE);

    final Response secondResponse =
        signer.eth2Sign(keyPair.getPublicKey().toString(), secondRequest);
    assertThat(secondResponse.getStatusCode()).isEqualTo(SLASHING_PROTECTION_ENFORCED);

    assertThat(signer.getMetricsMatching(attestationSlashingMetrics))
        .containsOnly(
            attestationSlashingMetrics.get(0) + " 1.0", attestationSlashingMetrics.get(1) + " 1.0");
  }

  @Test
  void cannotSignSurroundedAttestationWhenSlashingEnabled(@TempDir final Path testDirectory)
      throws JsonProcessingException {
    setupSigner(testDirectory);

    final Eth2SigningRequestBody initialRequest = createAttestationRequest(3, 6, UInt64.ONE);
    final Response initialResponse =
        signer.eth2Sign(keyPair.getPublicKey().toString(), initialRequest);
    assertThat(initialResponse.getStatusCode()).isEqualTo(200);

    // attempt a surrounded Request
    final Eth2SigningRequestBody surroundedRequest = createAttestationRequest(4, 5, UInt64.ONE);

    final Response surroundedResponse =
        signer.eth2Sign(keyPair.getPublicKey().toString(), surroundedRequest);
    assertThat(surroundedResponse.getStatusCode()).isEqualTo(SLASHING_PROTECTION_ENFORCED);
  }

  @Test
  void cannotSignASurroundingAttestationWhenSlashingEnabled(@TempDir final Path testDirectory)
      throws JsonProcessingException {
    setupSigner(testDirectory);

    final Eth2SigningRequestBody initialRequest = createAttestationRequest(3, 6, UInt64.ONE);
    final Response initialResponse =
        signer.eth2Sign(keyPair.getPublicKey().toString(), initialRequest);
    assertThat(initialResponse.getStatusCode()).isEqualTo(200);

    // attempt a surrounding Request
    final Eth2SigningRequestBody surroundingRequest = createAttestationRequest(2, 7, UInt64.ONE);

    final Response surroundingResponse =
        signer.eth2Sign(keyPair.getPublicKey().toString(), surroundingRequest);
    assertThat(surroundingResponse.getStatusCode()).isEqualTo(SLASHING_PROTECTION_ENFORCED);
  }

  @Test
  void canSignSameBlockTwiceWhenSlashingIsEnabled(@TempDir final Path testDirectory)
      throws JsonProcessingException {

    setupSigner(testDirectory);

    final Eth2SigningRequestBody request =
        createBlockRequest(
            UInt64.valueOf(3L),
            Bytes32.fromHexString(
                "0x2b530d6262576277f1cc0dbe341fd919f9f8c5c92fc9140dff6db4ef34edea0d"));

    final Response initialResponse = signer.eth2Sign(keyPair.getPublicKey().toString(), request);
    assertThat(initialResponse.getStatusCode()).isEqualTo(200);
    assertThat(signer.getMetricsMatching(blockSlashingMetrics))
        .containsOnly(blockSlashingMetrics.get(0) + " 1.0", blockSlashingMetrics.get(1) + " 0.0");

    final Response secondResponse = signer.eth2Sign(keyPair.getPublicKey().toString(), request);
    assertThat(secondResponse.getStatusCode()).isEqualTo(200);
    assertThat(signer.getMetricsMatching(blockSlashingMetrics))
        .containsOnly(blockSlashingMetrics.get(0) + " 2.0", blockSlashingMetrics.get(1) + " 0.0");
  }

  @Test
  void signingBlockWithDifferentSigningRootForPreviousSlotFailsWith412(
      @TempDir final Path testDirectory) throws JsonProcessingException {
    setupSigner(testDirectory);

    final Eth2SigningRequestBody initialRequest =
        createBlockRequest(
            UInt64.valueOf(3L),
            Bytes32.fromHexString(
                "0x2b530d6262576277f1cc0dbe341fd919f9f8c5c92fc9140dff6db4ef34edea0d"));

    final Response initialResponse =
        signer.eth2Sign(keyPair.getPublicKey().toString(), initialRequest);
    assertThat(initialResponse.getStatusCode()).isEqualTo(200);
    assertThat(signer.getMetricsMatching(blockSlashingMetrics))
        .containsOnly(blockSlashingMetrics.get(0) + " 1.0", blockSlashingMetrics.get(1) + " 0.0");

    final Eth2SigningRequestBody secondRequest =
        createBlockRequest(
            UInt64.valueOf(3L),
            Bytes32.fromHexString(
                "0xb2eedb01adbd02c828d5eec09b4c70cbba12ffffba525ebf48aca33028e8ad89"));

    final Response secondResponse =
        signer.eth2Sign(keyPair.getPublicKey().toString(), secondRequest);
    assertThat(secondResponse.getStatusCode()).isEqualTo(SLASHING_PROTECTION_ENFORCED);
    assertThat(signer.getMetricsMatching(blockSlashingMetrics))
        .containsOnly(blockSlashingMetrics.get(0) + " 1.0", blockSlashingMetrics.get(1) + " 1.0");
  }
}
