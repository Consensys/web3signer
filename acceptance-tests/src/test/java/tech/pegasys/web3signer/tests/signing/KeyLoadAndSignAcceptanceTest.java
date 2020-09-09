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

import static io.restassured.RestAssured.given;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

import tech.pegasys.teku.bls.BLS;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.teku.bls.BLSSignature;
import tech.pegasys.web3signer.core.signing.KeyType;
import tech.pegasys.web3signer.dsl.signer.Signer;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;

import java.nio.file.Path;

import io.restassured.http.ContentType;
import io.vertx.core.json.JsonObject;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

public class KeyLoadAndSignAcceptanceTest extends SigningAcceptanceTestBase {

  private static final Bytes DATA = Bytes.wrap("Hello, world!".getBytes(UTF_8));
  private static final String PRIVATE_KEY =
      "3ee2224386c82ffea477e2adf28a2929f5c349165a4196158c7f3a2ecca40f35";

  private static final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();
  private static final BLSSecretKey key =
      BLSSecretKey.fromBytes(Bytes32.fromHexString(PRIVATE_KEY));
  private static final BLSKeyPair keyPair = new BLSKeyPair(key);
  private static final BLSPublicKey publicKey = keyPair.getPublicKey();
  private static final BLSSignature expectedSignature = BLS.sign(keyPair.getSecretKey(), DATA);

  @ParameterizedTest
  @EnumSource(value = KeyType.class)
  public void receiveA404IfRequestedKeyDoesNotExist(final KeyType keyType) {
    setupSigner(keyType == KeyType.BLS ? "eth2" : "eth1");
    given()
        .baseUri(signer.getUrl())
        .filter(signer.getOpenApiValidationFilter())
        .contentType(ContentType.JSON)
        .pathParam("identifier", keyPair.getPublicKey().toString())
        .body(new JsonObject().put("data", DATA.toHexString()).toString())
        .when()
        .post(Signer.signPath(keyType))
        .then()
        .assertThat()
        .statusCode(404);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"zzzddd"})
  public void receiveA400IfDataIsNotValid(final String data) {
    final String configFilename = publicKey.toString().substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createUnencryptedYamlFileAt(keyConfigFile, PRIVATE_KEY, KeyType.BLS);

    setupSigner("eth2");

    // without client-side openapi validator
    given()
        .baseUri(signer.getUrl())
        .contentType(ContentType.JSON)
        .pathParam("identifier", keyPair.getPublicKey().toString())
        .body(new JsonObject().put("data", data).toString())
        .when()
        .post(Signer.signPath(KeyType.BLS))
        .then()
        .assertThat()
        .statusCode(400);
  }

  @Test
  public void receiveA400IfDataIsMissingFromJsonBody() {
    final String configFilename = keyPair.getPublicKey().toString().substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createUnencryptedYamlFileAt(keyConfigFile, PRIVATE_KEY, KeyType.BLS);

    setupSigner("eth2");

    // without OpenAPI validation filter
    given()
        .baseUri(signer.getUrl())
        .contentType(ContentType.JSON)
        .pathParam("identifier", keyPair.getPublicKey().toString())
        .body("{\"invalid\": \"json body\"}")
        .when()
        .post(Signer.signPath(KeyType.BLS))
        .then()
        .assertThat()
        .statusCode(400);
  }

  @Test
  public void receiveA400IfJsonBodyIsMalformed() {
    final String configFilename = keyPair.getPublicKey().toString().substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createUnencryptedYamlFileAt(keyConfigFile, PRIVATE_KEY, KeyType.BLS);

    setupSigner("eth2");

    // without OpenAPI validation filter
    given()
        .baseUri(signer.getUrl())
        .contentType(ContentType.JSON)
        .pathParam("identifier", keyPair.getPublicKey().toString())
        .body("not a json body")
        .when()
        .post(Signer.signPath(KeyType.BLS))
        .then()
        .assertThat()
        .statusCode(400);
  }

  @Test
  public void unusedFieldsInRequestDoesNotAffectSigning() {
    final String configFilename = keyPair.getPublicKey().toString().substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createUnencryptedYamlFileAt(keyConfigFile, PRIVATE_KEY, KeyType.BLS);

    setupSigner("eth2");

    given()
        .baseUri(signer.getUrl())
        .filter(signer.getOpenApiValidationFilter())
        .contentType(ContentType.JSON)
        .pathParam("identifier", keyPair.getPublicKey().toString())
        .body(
            new JsonObject()
                .put("data", DATA.toHexString())
                .put("unknownField", "someValue")
                .toString())
        .when()
        .post(Signer.signPath(KeyType.BLS))
        .then()
        .assertThat()
        .statusCode(200)
        .body(equalToIgnoringCase(expectedSignature.toString()));
  }
}
