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
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

import tech.pegasys.eth2signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.eth2signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.eth2signer.tests.AcceptanceTestBase;
import tech.pegasys.teku.bls.BLS;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.teku.bls.BLSSignature;

import java.nio.file.Path;

import io.restassured.http.ContentType;
import io.vertx.core.json.JsonObject;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class KeyLoadAndSignAcceptanceTest extends AcceptanceTestBase {

  private static final Bytes DATA = Bytes.wrap("Hello, world!".getBytes(UTF_8));
  private static final String PRIVATE_KEY =
      "3ee2224386c82ffea477e2adf28a2929f5c349165a4196158c7f3a2ecca40f35";

  private static final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();
  private static final BLSSecretKey key = BLSSecretKey.fromBytes(Bytes.fromHexString(PRIVATE_KEY));
  private static final BLSKeyPair keyPair = new BLSKeyPair(key);
  private static final BLSPublicKey publicKey = keyPair.getPublicKey();
  private static final BLSSignature expectedSignature = BLS.sign(keyPair.getSecretKey(), DATA);
  private static final String SIGN_ENDPOINT = "/signer/sign/{identifier}";

  @TempDir Path testDirectory;

  @Test
  public void receiveA404IfRequestedKeyDoesNotExist() {
    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    startSigner(builder.build());

    given()
        .baseUri(signer.getUrl())
        .filter(getOpenApiValidationFilter())
        .contentType(ContentType.JSON)
        .pathParam("identifier", keyPair.getPublicKey().toString())
        .body(new JsonObject().put("data", DATA.toHexString()).toString())
        .when()
        .post(SIGN_ENDPOINT)
        .then()
        .assertThat()
        .statusCode(404);
  }

  @Test
  public void receiveA400IfDataIsNull() {
    final String configFilename = publicKey.toString().substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createUnencryptedYamlFileAt(keyConfigFile, PRIVATE_KEY);

    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withKeyStoreDirectory(testDirectory);
    startSigner(builder.build());

    // without client-side openapi validator
    given()
        .baseUri(signer.getUrl())
        .contentType(ContentType.JSON)
        .pathParam("identifier", keyPair.getPublicKey().toString())
        .body(new JsonObject().put("data", (String) null).toString())
        .when()
        .post(SIGN_ENDPOINT)
        .then()
        .assertThat()
        .statusCode(400);
  }

  @Test
  public void receiveA400IfDataIsMissingFromJsonBody() {
    final String configFilename = keyPair.getPublicKey().toString().substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createUnencryptedYamlFileAt(keyConfigFile, PRIVATE_KEY);

    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withKeyStoreDirectory(testDirectory);
    startSigner(builder.build());

    // without OpenAPI validation filter
    given()
        .baseUri(signer.getUrl())
        .contentType(ContentType.JSON)
        .pathParam("identifier", keyPair.getPublicKey().toString())
        .body("{\"invalid\": \"json body\"}")
        .when()
        .post(SIGN_ENDPOINT)
        .then()
        .assertThat()
        .statusCode(400);
  }

  @Test
  public void receiveA400IfJsonBodyIsMalformed() {
    final String configFilename = keyPair.getPublicKey().toString().substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createUnencryptedYamlFileAt(keyConfigFile, PRIVATE_KEY);

    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withKeyStoreDirectory(testDirectory);
    startSigner(builder.build());

    // without OpenAPI validation filter
    given()
        .baseUri(signer.getUrl())
        .contentType(ContentType.JSON)
        .pathParam("identifier", keyPair.getPublicKey().toString())
        .body("not a json body")
        .when()
        .post(SIGN_ENDPOINT)
        .then()
        .assertThat()
        .statusCode(400);
  }

  @Test
  public void unusedFieldsInRequestDoesNotAffectSigning() {
    final String configFilename = keyPair.getPublicKey().toString().substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createUnencryptedYamlFileAt(keyConfigFile, PRIVATE_KEY);

    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withKeyStoreDirectory(testDirectory);
    startSigner(builder.build());

    given()
        .baseUri(signer.getUrl())
        .filter(getOpenApiValidationFilter())
        .contentType(ContentType.JSON)
        .pathParam("identifier", keyPair.getPublicKey().toString())
        .body(
            new JsonObject()
                .put("data", DATA.toHexString())
                .put("unknownField", "someValue")
                .toString())
        .when()
        .post(SIGN_ENDPOINT)
        .then()
        .assertThat()
        .statusCode(200)
        .body(equalToIgnoringCase(expectedSignature.toString()));
  }
}
