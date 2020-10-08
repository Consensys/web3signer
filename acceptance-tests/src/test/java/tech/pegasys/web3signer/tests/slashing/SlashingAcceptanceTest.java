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

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.teku.api.schema.AttestationData;
import tech.pegasys.teku.api.schema.BLSSignature;
import tech.pegasys.teku.api.schema.BeaconBlock;
import tech.pegasys.teku.api.schema.BeaconBlockBody;
import tech.pegasys.teku.api.schema.Checkpoint;
import tech.pegasys.teku.api.schema.Eth1Data;
import tech.pegasys.teku.api.schema.Fork;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.ssz.SSZTypes.Bytes4;
import tech.pegasys.web3signer.core.service.http.ArtifactType;
import tech.pegasys.web3signer.core.service.http.Eth2SigningRequestBody;
import tech.pegasys.web3signer.core.signing.KeyType;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.web3signer.tests.AcceptanceTestBase;

import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.response.Response;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class SlashingAcceptanceTest extends AcceptanceTestBase {

  private static final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();
  private final BLSKeyPair keyPair = BLSKeyPair.random(0);

  final List<String> attestationSlashingMetrics =
      List.of(
          "eth2_slashingprotection_permitted_signings{artifactType=\"ATTESTATION\",}",
          "eth2_slashingprotection_prevented_signings{artifactType=\"ATTESTATION\",}");

  final List<String> blockSlashingMetrics =
      List.of(
          "eth2_slashingprotection_permitted_signings{artifactType=\"BLOCK\",}",
          "eth2_slashingprotection_prevented_signings{artifactType=\"BLOCK\",}");

  void setupSigner(final Path testDirectory, final boolean enableSlashing) {
    final SignerConfigurationBuilder builder =
        new SignerConfigurationBuilder()
            .withMode("eth2")
            .withSlashingEnabled(enableSlashing)
            .withSlashingProtectionDbUsername("postgres")
            .withSlashingProtectionDbPassword("postgres")
            .withMetricsEnabled(true)
            .withKeyStoreDirectory(testDirectory);

    final Path keyConfigFile = testDirectory.resolve("keyfile.yaml");
    metadataFileHelpers.createUnencryptedYamlFileAt(
        keyConfigFile, keyPair.getSecretKey().toBytes().toHexString(), KeyType.BLS);

    startSigner(builder.build());
  }

  @Test
  void canSignSameAttestationTwiceWhenSlashingIsEnabled(@TempDir Path testDirectory)
      throws JsonProcessingException {

    setupSigner(testDirectory, true);

    final Eth2SigningRequestBody request = createAttestationRequest(5, 6, UInt64.ZERO);

    final Response initialResponse = signer.eth2Sign(keyPair.getPublicKey().toString(), request);
    assertThat(initialResponse.getStatusCode()).isEqualTo(200);

    assertThat(signer.getMetricsMatching(attestationSlashingMetrics))
        .containsOnly(attestationSlashingMetrics.get(0) + " 1.0");

    final Response secondResponse = signer.eth2Sign(keyPair.getPublicKey().toString(), request);
    assertThat(secondResponse.getStatusCode()).isEqualTo(200);

    assertThat(signer.getMetricsMatching(attestationSlashingMetrics))
        .containsOnly(attestationSlashingMetrics.get(0) + " 2.0");
  }

  @Test
  void cannotSignASecondAttestationForSameSlotWithDifferentSigningRoot(@TempDir Path testDirectory)
      throws JsonProcessingException {
    setupSigner(testDirectory, true);

    final Eth2SigningRequestBody initialRequest = createAttestationRequest(5, 6, UInt64.ZERO);

    final Response initialResponse =
        signer.eth2Sign(keyPair.getPublicKey().toString(), initialRequest);
    assertThat(initialResponse.getStatusCode()).isEqualTo(200);

    assertThat(signer.getMetricsMatching(attestationSlashingMetrics))
        .containsOnly(attestationSlashingMetrics.get(0) + " 1.0");

    final Eth2SigningRequestBody secondRequest = createAttestationRequest(5, 6, UInt64.ONE);

    final Response secondResponse =
        signer.eth2Sign(keyPair.getPublicKey().toString(), secondRequest);
    assertThat(secondResponse.getStatusCode()).isEqualTo(403);

    assertThat(signer.getMetricsMatching(attestationSlashingMetrics))
        .containsOnly(
            attestationSlashingMetrics.get(0) + " 1.0", attestationSlashingMetrics.get(1) + " 1.0");
  }

  @Test
  void cannotSignSurroundedAttestationWhenSlashingEnabled(@TempDir Path testDirectory)
      throws JsonProcessingException {
    setupSigner(testDirectory, true);

    final Eth2SigningRequestBody initialRequest = createAttestationRequest(3, 6, UInt64.ONE);
    final Response initialResponse =
        signer.eth2Sign(keyPair.getPublicKey().toString(), initialRequest);
    assertThat(initialResponse.getStatusCode()).isEqualTo(200);

    // attempt a surrounded Request
    final Eth2SigningRequestBody surroundedRequest = createAttestationRequest(4, 5, UInt64.ONE);

    final Response surroundedResponse =
        signer.eth2Sign(keyPair.getPublicKey().toString(), surroundedRequest);
    assertThat(surroundedResponse.getStatusCode()).isEqualTo(403);
  }

  @Test
  void cannotSignASurroundingAttestationWhenSlashingEnabled(@TempDir Path testDirectory)
      throws JsonProcessingException {
    setupSigner(testDirectory, true);

    final Eth2SigningRequestBody initialRequest = createAttestationRequest(3, 6, UInt64.ONE);
    final Response initialResponse =
        signer.eth2Sign(keyPair.getPublicKey().toString(), initialRequest);
    assertThat(initialResponse.getStatusCode()).isEqualTo(200);

    // attempt a surrounding Request
    final Eth2SigningRequestBody surroundingRequest = createAttestationRequest(2, 7, UInt64.ONE);

    final Response surroundingResponse =
        signer.eth2Sign(keyPair.getPublicKey().toString(), surroundingRequest);
    assertThat(surroundingResponse.getStatusCode()).isEqualTo(403);
  }

  @Test
  void canSignSameBlockTwiceWhenSlashingIsEnabled(@TempDir Path testDirectory)
      throws JsonProcessingException {

    setupSigner(testDirectory, true);

    final Eth2SigningRequestBody request =
        createBlockRequest(
            UInt64.valueOf(3L),
            Bytes32.fromHexString(
                "0x2b530d6262576277f1cc0dbe341fd919f9f8c5c92fc9140dff6db4ef34edea0d"));

    final Response initialResponse = signer.eth2Sign(keyPair.getPublicKey().toString(), request);
    assertThat(initialResponse.getStatusCode()).isEqualTo(200);
    assertThat(signer.getMetricsMatching(blockSlashingMetrics))
        .containsOnly(blockSlashingMetrics.get(0) + " 1.0");

    final Response secondResponse = signer.eth2Sign(keyPair.getPublicKey().toString(), request);
    assertThat(secondResponse.getStatusCode()).isEqualTo(200);
    assertThat(signer.getMetricsMatching(blockSlashingMetrics))
        .containsOnly(blockSlashingMetrics.get(0) + " 2.0");
  }

  @Test
  void signingBlockWithDifferentSigningRootForPreviousSlotFailsWith403(@TempDir Path testDirectory)
      throws JsonProcessingException {
    setupSigner(testDirectory, true);

    final Eth2SigningRequestBody initialRequest =
        createBlockRequest(
            UInt64.valueOf(3L),
            Bytes32.fromHexString(
                "0x2b530d6262576277f1cc0dbe341fd919f9f8c5c92fc9140dff6db4ef34edea0d"));

    final Response initialResponse =
        signer.eth2Sign(keyPair.getPublicKey().toString(), initialRequest);
    assertThat(initialResponse.getStatusCode()).isEqualTo(200);
    assertThat(signer.getMetricsMatching(blockSlashingMetrics))
        .containsOnly(blockSlashingMetrics.get(0) + " 1.0");

    final Eth2SigningRequestBody secondRequest =
        createBlockRequest(
            UInt64.valueOf(3L),
            Bytes32.fromHexString(
                "0xb2eedb01adbd02c828d5eec09b4c70cbba12ffffba525ebf48aca33028e8ad89"));

    final Response secondResponse =
        signer.eth2Sign(keyPair.getPublicKey().toString(), secondRequest);
    assertThat(secondResponse.getStatusCode()).isEqualTo(403);
    assertThat(signer.getMetricsMatching(blockSlashingMetrics))
        .containsOnly(blockSlashingMetrics.get(0) + " 1.0", blockSlashingMetrics.get(1) + " 1.0");
  }

  private Eth2SigningRequestBody createAttestationRequest(
      final int sourceEpoch, final int targetEpoch, final UInt64 slot) {
    return new Eth2SigningRequestBody(
        ArtifactType.ATTESTATION,
        Bytes32.fromHexString("0x270d43e74ce340de4bca2b1936beca0f4f5408d9e78aec4850920baf659d5b69"),
        new Fork(
            Bytes4.fromHexString("0x00000001"),
            Bytes4.fromHexString("0x00000001"),
            UInt64.valueOf(1)),
        null,
        new AttestationData(
            UInt64.valueOf(32),
            slot,
            Bytes32.fromHexString(
                "0xb2eedb01adbd02c828d5eec09b4c70cbba12ffffba525ebf48aca33028e8ad89"),
            new Checkpoint(UInt64.valueOf(sourceEpoch), Bytes32.ZERO),
            new Checkpoint(
                UInt64.valueOf(targetEpoch),
                Bytes32.fromHexString(
                    "0xb2eedb01adbd02c828d5eec09b4c70cbba12ffffba525ebf48aca33028e8ad89"))),
        null);
  }

  private Eth2SigningRequestBody createBlockRequest(final UInt64 slot, final Bytes32 stateRoot) {
    return new Eth2SigningRequestBody(
        ArtifactType.BLOCK,
        Bytes32.fromHexString("0x270d43e74ce340de4bca2b1936beca0f4f5408d9e78aec4850920baf659d5b69"),
        new Fork(
            Bytes4.fromHexString("0x00000001"),
            Bytes4.fromHexString("0x00000001"),
            UInt64.valueOf(1)),
        new BeaconBlock(
            slot,
            UInt64.valueOf(5),
            Bytes32.fromHexString(
                "0xb2eedb01adbd02c828d5eec09b4c70cbba12ffffba525ebf48aca33028e8ad89"),
            stateRoot,
            new BeaconBlockBody(
                BLSSignature.fromHexString(
                    "0xa686652aed2617da83adebb8a0eceea24bb0d2ccec9cd691a902087f90db16aa5c7b03172a35e874e07e3b60c5b2435c0586b72b08dfe5aee0ed6e5a2922b956aa88ad0235b36dfaa4d2255dfeb7bed60578d982061a72c7549becab19b3c12f"),
                new Eth1Data(
                    Bytes32.fromHexString(
                        "0x6a0f9d6cb0868daa22c365563bb113b05f7568ef9ee65fdfeb49a319eaf708cf"),
                    UInt64.valueOf(8),
                    Bytes32.fromHexString(
                        "0x4242424242424242424242424242424242424242424242424242424242424242")),
                Bytes32.fromHexString(
                    "0x74656b752f76302e31322e31302d6465762d6338316361363235000000000000"),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList())),
        null,
        null);
  }
}
