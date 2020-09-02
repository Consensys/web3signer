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
package tech.pegasys.eth2signer.tests.signing;

import static io.restassured.RestAssured.given;
import static java.nio.charset.StandardCharsets.UTF_8;

import tech.pegasys.eth2signer.core.signing.KeyType;
import tech.pegasys.eth2signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSSecretKey;

import java.nio.file.Path;

import io.restassured.response.Response;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

public class JsonRpcSigningErrorsAcceptanceTest extends SigningAcceptanceTestBase {
  private static final Bytes DATA = Bytes.wrap("Hello, world!".getBytes(UTF_8));
  private static final String PRIVATE_KEY =
      "3ee2224386c82ffea477e2adf28a2929f5c349165a4196158c7f3a2ecca40f35";
  private static final BLSSecretKey key =
      BLSSecretKey.fromBytes(Bytes32.fromHexString(PRIVATE_KEY));
  private static final BLSKeyPair keyPair = new BLSKeyPair(key);
  private static final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();

  @Test
  void signReturnErrorCode30000WhenIdentifierNotAvailable() {
    setupSigner();

    final String publicKey = keyPair.getPublicKey().toString();
    final Response jsonResponse = callJsonRpcSign(publicKey, DATA.toHexString());
    verifyJsonRpcSignatureErrorResponse(
        jsonResponse, -30000, "Signer not found for identifier", new String[] {publicKey});
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"zzzddd"})
  public void signReturnInvalidParamErrorWhenDataValueIsInvalid(final String data) {
    final String publicKey = keyPair.getPublicKey().toString();
    final String configFilename = publicKey.substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createUnencryptedYamlFileAt(keyConfigFile, PRIVATE_KEY, KeyType.BLS);

    setupSigner();

    final Response jsonResponse = callJsonRpcSign(publicKey, data);
    verifyJsonRpcSignatureErrorResponse(jsonResponse, -32602, "Invalid params", null);
  }

  @Test
  public void signReturnParseErrorWhenInvalidJsonBodyIsSent() {
    final String publicKey = keyPair.getPublicKey().toString();
    final String configFilename = publicKey.substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createUnencryptedYamlFileAt(keyConfigFile, PRIVATE_KEY, KeyType.BLS);

    setupSigner();

    final Response jsonResponse =
        given().baseUri(signer.getUrl()).body("invalid json body").post(JSON_RPC_PATH);
    verifyJsonRpcSignatureErrorResponse(jsonResponse, -32700, "Parse error", null);
  }
}
