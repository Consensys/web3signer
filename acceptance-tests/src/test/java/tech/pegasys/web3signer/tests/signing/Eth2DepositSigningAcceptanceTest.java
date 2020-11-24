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

import tech.pegasys.teku.api.schema.BLSPubKey;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.ssz.SSZTypes.Bytes4;
import tech.pegasys.web3signer.core.service.http.ArtifactType;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.DepositMessage;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.Eth2SigningRequestBody;
import tech.pegasys.web3signer.core.signing.KeyType;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import io.restassured.response.Response;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class Eth2DepositSigningAcceptanceTest extends SigningAcceptanceTestBase {

  private static final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();

  @TestFactory
  Stream<DynamicTest> signDepositData() {
    loadKeystore("keystore-1");
    loadKeystore("keystore-2");

    setupSigner("eth2");

    final ObjectMapper objectMapper = new ObjectMapper();

    final Path testFilesPath = Path.of(Resources.getResource(Path.of("eth2").toString()).getPath());
    try {
      return Files.list(testFilesPath)
          .filter(p -> p.getFileName().toString().startsWith("deposit_data"))
          .flatMap(
              tf -> {
                try {
                  return createTest(objectMapper, tf);
                } catch (IOException e) {
                  throw new RuntimeException("Failed to create dynamic tests", e);
                }
              });
    } catch (IOException e) {
      throw new RuntimeException("Failed to create dynamic tests", e);
    }
  }

  private Stream<DynamicTest> createTest(final ObjectMapper objectMapper, final Path tf)
      throws IOException {
    return objectMapper.<List<Map<String, String>>>readValue(tf.toFile(), new TypeReference<>() {})
        .stream()
        .map(
            depositData -> {
              final String displayName = testDisplayName(tf) + "-" + depositData.get("pubkey");
              return DynamicTest.dynamicTest(displayName, () -> verifyDepositData(depositData));
            });
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
        new Eth2SigningRequestBody(
            ArtifactType.DEPOSIT, null, null, null, null, null, null, null, null, depositMessage);

    final Response response = signer.eth2Sign(publicKey, requestBody, TEXT);

    final Bytes signature = verifyAndGetSignatureResponse(response, TEXT);
    assertThat(signature).isEqualTo(Bytes.fromHexString(depositData.get("signature")));
  }

  private void loadKeystore(final String keystore) {
    final Path keystorePath =
        Path.of(Resources.getResource("eth2/" + keystore + ".json").toString());
    metadataFileHelpers.createKeyStoreYamlFileAt(
        testDirectory.resolve(keystore + ".yaml"), keystorePath, "password", KeyType.BLS);
  }
}
