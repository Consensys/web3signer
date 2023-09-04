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
package tech.pegasys.web3signer.tests.signing;

import static io.restassured.http.ContentType.TEXT;
import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.web3signer.signing.KeyType.BLS;

import tech.pegasys.teku.api.schema.BLSPubKey;
import tech.pegasys.teku.infrastructure.bytes.Bytes4;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.SpecMilestone;
import tech.pegasys.teku.spec.networks.Eth2Network;
import tech.pegasys.web3signer.core.service.http.ArtifactType;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.DepositMessage;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.Eth2SigningRequestBody;
import tech.pegasys.web3signer.dsl.utils.Eth2SigningRequestBodyBuilder;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.io.Resources;
import io.restassured.response.Response;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class Eth2DepositSigningAcceptanceTest extends SigningAcceptanceTestBase {
  private static final String PRIVATE_KEY1 =
      "0x4b743372aedfaecec4c890fd4f6d4fc2ff751bfb6c71449f326baa80fea00a21";
  private static final String PRIVATE_KEY2 =
      "0x39792f5b344a4b00d8147fee77ad5d60b115130a5e2d2ef56895baae0960f880";
  private final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();

  @TestFactory
  List<DynamicTest> signDepositData() {
    metadataFileHelpers.createUnencryptedYamlFileAt(
        testDirectory.resolve("1.yaml"), PRIVATE_KEY1, BLS);
    metadataFileHelpers.createUnencryptedYamlFileAt(
        testDirectory.resolve("2.yaml"), PRIVATE_KEY2, BLS);

    setupEth2Signer(Eth2Network.MAINNET, SpecMilestone.ALTAIR);

    final ObjectMapper objectMapper = JsonMapper.builder().build();

    final Path testFilesPath = Path.of(Resources.getResource("eth2").getPath());
    try (final Stream<Path> files = Files.list(testFilesPath)) {
      return files
          .filter(p -> p.getFileName().toString().startsWith("deposit_data"))
          .flatMap(tf -> createTest(objectMapper, tf))
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new RuntimeException("Failed to create sign deposit tests", e);
    }
  }

  private Stream<DynamicTest> createTest(final ObjectMapper objectMapper, final Path tf) {
    try {
      return objectMapper
          .<List<Map<String, String>>>readValue(tf.toFile(), new TypeReference<>() {})
          .stream()
          .map(
              depositData -> {
                final String displayName = testDisplayName(tf) + "-" + depositData.get("pubkey");
                return DynamicTest.dynamicTest(displayName, () -> verifyDepositData(depositData));
              });
    } catch (IOException e) {
      throw new RuntimeException("Failed to create sign deposit tests", e);
    }
  }

  private String testDisplayName(final Path tf) {
    final String filename = tf.getFileName().toString();
    return filename.replaceFirst("deposit_data-", "").replaceFirst(".json", "");
  }

  private void verifyDepositData(final Map<String, String> depositData) throws IOException {
    final String publicKey = depositData.get("pubkey");
    final DepositMessage depositMessage =
        new DepositMessage(
            BLSPubKey.fromHexString(publicKey),
            Bytes32.fromHexString(depositData.get("withdrawal_credentials")),
            UInt64.valueOf(depositData.get("amount")),
            Bytes4.fromHexString(depositData.get("fork_version")));
    final Eth2SigningRequestBody requestBody =
        Eth2SigningRequestBodyBuilder.anEth2SigningRequestBody()
            .withType(ArtifactType.DEPOSIT)
            .withDeposit(depositMessage)
            .build();

    final Response response = signer.eth2Sign(publicKey, requestBody, TEXT);

    final Bytes signature = verifyAndGetSignatureResponse(response, TEXT);
    assertThat(signature).isEqualTo(Bytes.fromHexString(depositData.get("signature")));
  }
}
