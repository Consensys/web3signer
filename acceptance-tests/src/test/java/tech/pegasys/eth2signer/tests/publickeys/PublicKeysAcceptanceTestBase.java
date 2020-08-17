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
package tech.pegasys.eth2signer.tests.publickeys;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import tech.pegasys.eth2signer.core.signing.KeyType;
import tech.pegasys.eth2signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.eth2signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.eth2signer.tests.AcceptanceTestBase;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.bls.BLSSecretKey;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.github.arteam.simplejsonrpc.core.domain.Request;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.tuweni.bytes.Bytes;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.io.TempDir;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.WalletUtils;
import org.web3j.utils.Numeric;

public class PublicKeysAcceptanceTestBase extends AcceptanceTestBase {
  static final String SIGNER_PUBLIC_KEYS_PATH = "/signer/publicKeys";
  static final String BLS = "BLS";
  static final String SECP256K1 = "SECP256K1";

  private static final String BLS_PRIVATE_KEY_1 =
      "3ee2224386c82ffea477e2adf28a2929f5c349165a4196158c7f3a2ecca40f35";
  private static final String BLS_PRIVATE_KEY_2 =
      "32ae313afff2daa2ef7005a7f834bdf291855608fe82c24d30be6ac2017093a8";
  private static final String SECP_PRIVATE_KEY_1 =
      "d392469474ec227b9ec4be232b402a0490045478ab621ca559d166965f0ffd32";
  private static final String SECP_PRIVATE_KEY_2 =
      "2e322a5f72c525422dc275e006d5cb3954ca5e02e9610fae0ed4cc389f622f33";

  protected static final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();

  @TempDir Path testDirectory;

  protected String[] privateKeys(final String keyType) {
    return keyType.equals("BLS")
        ? new String[] {BLS_PRIVATE_KEY_1, BLS_PRIVATE_KEY_2}
        : new String[] {SECP_PRIVATE_KEY_1, SECP_PRIVATE_KEY_2};
  }

  protected String[] createKeys(
      final String keyType, boolean isValid, final String... privateKeys) {
    return keyType.equals("BLS")
        ? createBlsKeys(isValid, privateKeys)
        : createSecpKeys(isValid, privateKeys);
  }

  protected String[] createBlsKeys(boolean isValid, final String... privateKeys) {
    return Stream.of(privateKeys)
        .map(
            privateKey -> {
              final BLSKeyPair keyPair =
                  new BLSKeyPair(BLSSecretKey.fromBytes(Bytes.fromHexString(privateKey)));
              final Path keyConfigFile = blsConfigFileName(keyPair.getPublicKey());
              if (isValid) {
                metadataFileHelpers.createUnencryptedYamlFileAt(
                    keyConfigFile, privateKey, KeyType.BLS);
              } else {
                createInvalidFile(keyConfigFile);
              }
              return keyPair.getPublicKey().toString();
            })
        .toArray(String[]::new);
  }

  protected String[] createSecpKeys(boolean isValid, final String... privateKeys) {
    return Stream.of(privateKeys)
        .map(
            privateKey -> {
              final ECKeyPair ecKeyPair =
                  ECKeyPair.create(Numeric.toBigInt(Bytes.fromHexString(privateKey).toArray()));
              final String publicKey = Numeric.toHexStringWithPrefix(ecKeyPair.getPublicKey());
              if (isValid) {
                createSecpKey(privateKey);
              } else {
                final Path keyConfigFile = testDirectory.resolve(publicKey + ".yaml");
                createInvalidFile(keyConfigFile);
              }
              return publicKey;
            })
        .toArray(String[]::new);
  }

  private void createInvalidFile(final Path keyConfigFile) {
    try {
      Files.createFile(keyConfigFile);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void createSecpKey(final String privateKeyHexString) {
    final String password = "pass";
    final Bytes privateKey = Bytes.fromHexString(privateKeyHexString);
    final ECKeyPair ecKeyPair = ECKeyPair.create(Numeric.toBigInt(privateKey.toArray()));
    final String publicKey = Numeric.toHexStringNoPrefix(ecKeyPair.getPublicKey());

    final String walletFile;
    try {
      walletFile =
          WalletUtils.generateWalletFile(password, ecKeyPair, testDirectory.toFile(), false);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to create wallet file", e);
    }

    metadataFileHelpers.createKeyStoreYamlFileAt(
        testDirectory.resolve(publicKey + ".yaml"),
        Path.of(walletFile),
        password,
        KeyType.SECP256K1);
  }

  private Path blsConfigFileName(final BLSPublicKey publicKey) {
    final String configFilename = publicKey.toString().substring(2);
    return testDirectory.resolve(configFilename + ".yaml");
  }

  protected void initAndStartSigner() {
    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withKeyStoreDirectory(testDirectory);
    startSigner(builder.build());
  }

  protected Response callApiPublicKeys(final String keyType) {
    return given()
        .filter(getOpenApiValidationFilter())
        .baseUri(signer.getUrl())
        .get(SIGNER_PUBLIC_KEYS_PATH + "/" + keyType);
  }

  protected Response callApiPublicKeysWithoutOpenApiClientSideFilter(final String keyType) {
    return given().baseUri(signer.getUrl()).accept("").get(SIGNER_PUBLIC_KEYS_PATH + "/" + keyType);
  }

  protected void validateApiResponse(final Response response, final Matcher<?> matcher) {
    response.then().statusCode(200).contentType(ContentType.JSON).body("", matcher);
  }

  protected Response callRpcPublicKeys(final String keyType) {
    final JsonNode params = JsonNodeFactory.instance.objectNode().put("keyType", keyType);
    final ValueNode id = JsonNodeFactory.instance.numberNode(1);
    final Request request = new Request("2.0", "public_keys", params, id);
    return given().baseUri(signer.getUrl()).body(request).post(JSON_RPC_PATH);
  }

  protected void validateRpcResponse(final Response response, final Matcher<?> resultMatcher) {
    response
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("jsonrpc", equalTo("2.0"), "id", equalTo(1), "result", resultMatcher);
  }
}
