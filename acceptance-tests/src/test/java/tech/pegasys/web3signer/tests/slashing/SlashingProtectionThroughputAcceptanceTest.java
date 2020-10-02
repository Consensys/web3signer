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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.response.Response;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.web3signer.core.service.http.ArtifactType;
import tech.pegasys.web3signer.core.service.http.Eth2SigningRequestBody;
import tech.pegasys.web3signer.core.signing.KeyType;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.web3signer.tests.AcceptanceTestBase;

public class SlashingProtectionThroughputAcceptanceTest extends AcceptanceTestBase {

  private static final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();
  protected final List<BLSKeyPair>
      keys = IntStream.rangeClosed(1, 150).mapToObj(BLSKeyPair::random).collect(Collectors.toList());

  void setupSigner(final Path testDirectory) {
    final SignerConfigurationBuilder builder =
        new SignerConfigurationBuilder()
            .withMode("eth2")
            .withSlashingEnabled(true)
            .withSlashingProtectionDbUsername("postgres")
            .withSlashingProtectionDbPassword("password")
            .withMetricsEnabled(true)
            .withKeyStoreDirectory(testDirectory);

    keys.forEach(kp -> {
      final Path keyConfigFile = testDirectory.resolve(kp.getPublicKey().toString() + ".yaml");
      metadataFileHelpers.createUnencryptedYamlFileAt(
          keyConfigFile, kp.getSecretKey().toBytes().toHexString(), KeyType.BLS);
    });

    startSigner(builder.build());
  }

  @Test
  void doIt(@TempDir Path testDirectory) {

    setupSigner(testDirectory);

    keys.stream().parallel().forEach(kp -> {
      IntStream.rangeClosed(1, 100).parallel().forEach(i -> {

            final Eth2SigningRequestBody request;
            if (i % 2 == 0) {
              request =
                  new Eth2SigningRequestBody(
                      Bytes.fromHexString("0x01"),
                      ArtifactType.ATTESTATION,
                      null,
                      UInt64.valueOf(i),
                      UInt64.valueOf(i));
            } else {
              request =
                  new Eth2SigningRequestBody(
                      Bytes.fromHexString("0x01"),
                      ArtifactType.BLOCK,
                      UInt64.valueOf(i),
                      null,
                      null);
            }

            final Response response;
            try {
              response = signer.eth2Sign(kp.getPublicKey().toString(), request);
            } catch (JsonProcessingException e) {
              throw new RuntimeException("OOPS!");
            }
            assertThat(response.getStatusCode()).isEqualTo(200);
          }
      );
    });
  }
}
