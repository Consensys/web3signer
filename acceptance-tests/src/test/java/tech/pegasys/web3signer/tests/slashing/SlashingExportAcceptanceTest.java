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
import static tech.pegasys.web3signer.dsl.utils.WaitUtils.waitFor;

import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.Eth2SigningRequestBody;
import tech.pegasys.web3signer.core.signing.Eth2Network;
import tech.pegasys.web3signer.dsl.signer.Signer;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.slashingprotection.interchange.InterchangeModule;
import tech.pegasys.web3signer.slashingprotection.interchange.model.SignedAttestation;

import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import dsl.InterchangeV5Format;
import dsl.SignedArtifacts;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class SlashingExportAcceptanceTest extends SlashingAcceptanceTest {

  @Test
  void slashingDataIsExported(@TempDir Path testDirectory) throws IOException {
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
    builder.withSlashingProtectionNetwork("MEDALLA");
    builder.withKeyStoreDirectory(testDirectory);
    builder.withSlashingExportPath(exportFile);
    builder.withHttpPort(12345); // prevent wait for Ports file in AT

    final Signer exportSigner = new Signer(builder.build(), null);
    exportSigner.start();
    waitFor(() -> assertThat(exportSigner.isRunning()).isFalse());

    final ObjectMapper mapper = new ObjectMapper().registerModule(new InterchangeModule());

    final InterchangeV5Format mappedData =
        mapper.readValue(exportFile.toFile(), InterchangeV5Format.class);

    assertThat(mappedData.getMetadata().getFormatVersionAsString()).isEqualTo("5");
    assertThat(mappedData.getMetadata().getGenesisValidatorsRoot())
        .isEqualTo(Eth2Network.MEDALLA.getGenesisValidatorsRoot());

    assertThat(mappedData.getSignedArtifacts()).hasSize(1);
    final SignedArtifacts artifacts = mappedData.getSignedArtifacts().get(0);

    assertThat(artifacts.getSignedBlocks()).hasSize(0);

    assertThat(artifacts.getSignedAttestations()).hasSize(1);
    final SignedAttestation attestation = artifacts.getSignedAttestations().get(0);
    assertThat(attestation.getSourceEpoch().toLong())
        .isEqualTo(request.getAttestation().source.epoch.longValue());
    assertThat(attestation.getTargetEpoch().toLong())
        .isEqualTo(request.getAttestation().target.epoch.longValue());
    assertThat(attestation.getSigningRoot()).isNotNull();
  }
}
