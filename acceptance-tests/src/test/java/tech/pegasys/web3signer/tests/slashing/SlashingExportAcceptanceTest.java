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
package tech.pegasys.web3signer.tests.slashing;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.web3signer.dsl.utils.WaitUtils.waitFor;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.response.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.pegasys.web3signer.core.service.http.ArtifactType;
import tech.pegasys.web3signer.core.service.http.Eth2SigningRequestBody;
import tech.pegasys.web3signer.dsl.signer.Signer;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;

public class SlashingExportAcceptanceTest extends SlashingAcceptanceTest {

  @Test
  void slashingDataIsExported(@TempDir Path testDirectory) throws IOException {
    setupSigner(testDirectory, true);

    final Eth2SigningRequestBody initialRequest =
        new Eth2SigningRequestBody(
            Bytes.fromHexString("0x01"), ArtifactType.BLOCK, UInt64.valueOf(3L), null, null);

    final Response initialResponse =
        signer.eth2Sign(keyPair.getPublicKey().toString(), initialRequest);
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
    builder.withPostfix("export --to=" + exportFile.toAbsolutePath().toString());
    builder.withHttpPort(12345); // prevent wait for Ports file in AT

    final Signer exportSigner = new Signer(builder.build(), null);
    exportSigner.start();
    waitFor(() -> assertThat(exportSigner.isRunning()).isFalse());

    final String exportData = Files.readString(exportFile);
    assertThat(exportData).contains(keyPair.getPublicKey().toString());


  }

}
