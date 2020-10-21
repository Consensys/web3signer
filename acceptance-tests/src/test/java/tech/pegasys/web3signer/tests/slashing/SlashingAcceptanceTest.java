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

import tech.pegasys.teku.bls.BLSKeyPair;
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
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class SlashingAcceptanceTest extends AcceptanceTestBase {

  protected static final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();
  protected final BLSKeyPair keyPair = BLSKeyPair.random(0);

  final List<String> attestationSlashingMetrics =
      List.of(
          "eth2_slashingprotection_permitted_signings",
          "eth2_slashingprotection_prevented_signings");

  final List<String> blockSlashingMetrics =
      List.of(
          "eth2_slashingprotection_permitted_signings",
          "eth2_slashingprotection_prevented_signings");

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

    final Eth2SigningRequestBody request =
        new Eth2SigningRequestBody(
            Bytes.fromHexString("0x01"),
            ArtifactType.ATTESTATION,
            null,
            UInt64.valueOf(5L),
            UInt64.valueOf(6L));

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
  void cannotSignASecondAttestationForSameSlotWithDifferentSigningRoot(@TempDir Path testDirectory)
      throws JsonProcessingException {
    setupSigner(testDirectory, true);

    final Eth2SigningRequestBody initialRequest =
        new Eth2SigningRequestBody(
            Bytes.fromHexString("0x01"),
            ArtifactType.ATTESTATION,
            null,
            UInt64.valueOf(5L),
            UInt64.valueOf(6L));

    final Response initialResponse =
        signer.eth2Sign(keyPair.getPublicKey().toString(), initialRequest);
    assertThat(initialResponse.getStatusCode()).isEqualTo(200);

    assertThat(signer.getMetricsMatching(attestationSlashingMetrics))
        .containsOnly(
            attestationSlashingMetrics.get(0) + " 1.0", attestationSlashingMetrics.get(1) + " 0.0");

    final Eth2SigningRequestBody secondRequest =
        new Eth2SigningRequestBody(
            Bytes.fromHexString("0x02"),
            ArtifactType.ATTESTATION,
            null,
            UInt64.valueOf(5L),
            UInt64.valueOf(6L));

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

    final Eth2SigningRequestBody initialRequest =
        new Eth2SigningRequestBody(
            Bytes.fromHexString("0x01"),
            ArtifactType.ATTESTATION,
            null,
            UInt64.valueOf(3L),
            UInt64.valueOf(6L));
    final Response initialResponse =
        signer.eth2Sign(keyPair.getPublicKey().toString(), initialRequest);
    assertThat(initialResponse.getStatusCode()).isEqualTo(200);

    // attempt a surrounded Request
    final Eth2SigningRequestBody surroundedRequest =
        new Eth2SigningRequestBody(
            Bytes.fromHexString("0x01"),
            ArtifactType.ATTESTATION,
            null,
            UInt64.valueOf(4L),
            UInt64.valueOf(5L));

    final Response surroundedResponse =
        signer.eth2Sign(keyPair.getPublicKey().toString(), surroundedRequest);
    assertThat(surroundedResponse.getStatusCode()).isEqualTo(403);
  }

  @Test
  void cannotSignASurroundingAttestationWhenSlashingEnabled(@TempDir Path testDirectory)
      throws JsonProcessingException {
    setupSigner(testDirectory, true);

    final Eth2SigningRequestBody initialRequest =
        new Eth2SigningRequestBody(
            Bytes.fromHexString("0x01"),
            ArtifactType.ATTESTATION,
            null,
            UInt64.valueOf(3L),
            UInt64.valueOf(6L));
    final Response initialResponse =
        signer.eth2Sign(keyPair.getPublicKey().toString(), initialRequest);
    assertThat(initialResponse.getStatusCode()).isEqualTo(200);

    // attempt a surrounding Request
    final Eth2SigningRequestBody surroundingRequest =
        new Eth2SigningRequestBody(
            Bytes.fromHexString("0x01"),
            ArtifactType.ATTESTATION,
            null,
            UInt64.valueOf(2L),
            UInt64.valueOf(7L));

    final Response surroundingResponse =
        signer.eth2Sign(keyPair.getPublicKey().toString(), surroundingRequest);
    assertThat(surroundingResponse.getStatusCode()).isEqualTo(403);
  }

  @Test
  void canSignSameBlockTwiceWhenSlashingIsEnabled(@TempDir Path testDirectory)
      throws JsonProcessingException {

    setupSigner(testDirectory, true);

    final Eth2SigningRequestBody request =
        new Eth2SigningRequestBody(
            Bytes.fromHexString("0x01"), ArtifactType.BLOCK, UInt64.valueOf(3L), null, null);

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
  void signingBlockWithDifferentSigningRootForPreviousSlotFailsWith403(@TempDir Path testDirectory)
      throws JsonProcessingException {
    setupSigner(testDirectory, true);

    final Eth2SigningRequestBody initialRequest =
        new Eth2SigningRequestBody(
            Bytes.fromHexString("0x01"), ArtifactType.BLOCK, UInt64.valueOf(3L), null, null);

    final Response initialResponse =
        signer.eth2Sign(keyPair.getPublicKey().toString(), initialRequest);
    assertThat(initialResponse.getStatusCode()).isEqualTo(200);
    assertThat(signer.getMetricsMatching(blockSlashingMetrics))
        .containsOnly(blockSlashingMetrics.get(0) + " 1.0", blockSlashingMetrics.get(1) + " 0.0");

    final Eth2SigningRequestBody secondRequest =
        new Eth2SigningRequestBody(
            Bytes.fromHexString("0x02"), ArtifactType.BLOCK, UInt64.valueOf(3L), null, null);

    final Response secondResponse =
        signer.eth2Sign(keyPair.getPublicKey().toString(), secondRequest);
    assertThat(secondResponse.getStatusCode()).isEqualTo(403);
    assertThat(signer.getMetricsMatching(blockSlashingMetrics))
        .containsOnly(blockSlashingMetrics.get(0) + " 1.0", blockSlashingMetrics.get(1) + " 1.0");
  }

  @Test
  void twoDifferentBlocksCanBeSignedForSameSlotIfSlashingIsDisabled(@TempDir Path testDirectory)
      throws JsonProcessingException {
    setupSigner(testDirectory, false);
    final Eth2SigningRequestBody initialRequest =
        new Eth2SigningRequestBody(
            Bytes.fromHexString("0x01"), ArtifactType.BLOCK, UInt64.valueOf(3L), null, null);

    final Response initialResponse =
        signer.eth2Sign(keyPair.getPublicKey().toString(), initialRequest);
    assertThat(initialResponse.getStatusCode()).isEqualTo(200);

    final Eth2SigningRequestBody secondRequest =
        new Eth2SigningRequestBody(
            Bytes.fromHexString("0x02"), ArtifactType.BLOCK, UInt64.valueOf(3L), null, null);

    final Response secondResponse =
        signer.eth2Sign(keyPair.getPublicKey().toString(), secondRequest);
    assertThat(secondResponse.getStatusCode()).isEqualTo(200);
  }
}
