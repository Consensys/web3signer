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
import static tech.pegasys.web3signer.dsl.signer.Signer.ETH_2_INTERFACE_OBJECT_MAPPER;

import tech.pegasys.teku.bls.BLS;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.teku.spec.SpecMilestone;
import tech.pegasys.teku.spec.networks.Eth2Network;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.Eth2SigningRequestBody;
import tech.pegasys.web3signer.dsl.signer.Signer;
import tech.pegasys.web3signer.dsl.utils.Eth2RequestUtils;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.web3signer.signing.KeyType;

import java.nio.file.Path;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.http.ContentType;
import io.vertx.core.json.JsonObject;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

public class KeyLoadAndSignAcceptanceTest extends SigningAcceptanceTestBase {

  private static final Bytes DATA = Bytes.wrap("Hello, world!".getBytes(UTF_8));
  private static final String PRIVATE_KEY =
      "3ee2224386c82ffea477e2adf28a2929f5c349165a4196158c7f3a2ecca40f35";

  private static final MetadataFileHelpers METADATA_FILE_HELPERS = new MetadataFileHelpers();
  private static final BLSSecretKey BLS_SECRET_KEY =
      BLSSecretKey.fromBytes(Bytes32.fromHexString(PRIVATE_KEY));
  private static final BLSKeyPair BLS_KEY_PAIR = new BLSKeyPair(BLS_SECRET_KEY);
  private static final BLSPublicKey PUBLIC_KEY = BLS_KEY_PAIR.getPublicKey();

  @ParameterizedTest
  @EnumSource(value = KeyType.class)
  public void receiveA404IfRequestedKeyDoesNotExist(final KeyType keyType)
      throws JsonProcessingException {
    if (keyType == KeyType.BLS) {
      setupEth2Signer(Eth2Network.MINIMAL, SpecMilestone.PHASE0);
    } else {
      setupEth1Signer();
    }
    final String body = createBody(keyType);
    given()
        .baseUri(signer.getUrl())
        .contentType(ContentType.JSON)
        .pathParam("identifier", BLS_KEY_PAIR.getPublicKey().toString())
        .body(body)
        .when()
        .post(Signer.signPath(keyType))
        .then()
        .assertThat()
        .statusCode(404);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"zzzddd"})
  public void receiveA400IfDataIsNotValid(final String data) {
    final String configFilename = PUBLIC_KEY.toString().substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    METADATA_FILE_HELPERS.createUnencryptedYamlFileAt(keyConfigFile, PRIVATE_KEY, KeyType.BLS);

    setupEth2Signer(Eth2Network.MAINNET, SpecMilestone.ALTAIR);

    // without client-side openapi validator
    given()
        .baseUri(signer.getUrl())
        .contentType(ContentType.JSON)
        .pathParam("identifier", BLS_KEY_PAIR.getPublicKey().toString())
        .body(new JsonObject().put("signingRoot", data).toString())
        .when()
        .post(Signer.signPath(KeyType.BLS))
        .then()
        .assertThat()
        .statusCode(400);
  }

  @Test
  public void receiveA400IfDataIsMissingFromJsonBody() {
    final String configFilename = BLS_KEY_PAIR.getPublicKey().toString().substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    METADATA_FILE_HELPERS.createUnencryptedYamlFileAt(keyConfigFile, PRIVATE_KEY, KeyType.BLS);

    setupEth2Signer(Eth2Network.MAINNET, SpecMilestone.ALTAIR);

    // without OpenAPI validation filter
    given()
        .baseUri(signer.getUrl())
        .contentType(ContentType.JSON)
        .pathParam("identifier", BLS_KEY_PAIR.getPublicKey().toString())
        .body("{\"invalid\": \"json body\"}")
        .when()
        .post(Signer.signPath(KeyType.BLS))
        .then()
        .assertThat()
        .statusCode(400);
  }

  @Test
  public void receiveA400IfJsonBodyIsMalformed() {
    final String configFilename = BLS_KEY_PAIR.getPublicKey().toString().substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    METADATA_FILE_HELPERS.createUnencryptedYamlFileAt(keyConfigFile, PRIVATE_KEY, KeyType.BLS);

    setupEth2Signer(Eth2Network.MAINNET, SpecMilestone.ALTAIR);

    // without OpenAPI validation filter
    given()
        .baseUri(signer.getUrl())
        .contentType(ContentType.JSON)
        .pathParam("identifier", BLS_KEY_PAIR.getPublicKey().toString())
        .body("not a json body")
        .when()
        .post(Signer.signPath(KeyType.BLS))
        .then()
        .assertThat()
        .statusCode(400);
  }

  @Test
  public void unusedFieldsInRequestDoesNotAffectSigning() throws JsonProcessingException {
    final String configFilename = BLS_KEY_PAIR.getPublicKey().toString().substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    METADATA_FILE_HELPERS.createUnencryptedYamlFileAt(keyConfigFile, PRIVATE_KEY, KeyType.BLS);

    setupEth2Signer(Eth2Network.MINIMAL, SpecMilestone.PHASE0);

    final Eth2SigningRequestBody blockRequest = Eth2RequestUtils.createBlockRequest();
    final JsonObject jsonObject =
        new JsonObject(ETH_2_INTERFACE_OBJECT_MAPPER.writeValueAsString(blockRequest));
    final String body = jsonObject.put("unknownField", "someValue").toString();
    final String expectedSignature =
        BLS.sign(BLS_KEY_PAIR.getSecretKey(), blockRequest.signingRoot()).toString();

    given()
        .baseUri(signer.getUrl())
        .contentType(ContentType.JSON)
        .pathParam("identifier", BLS_KEY_PAIR.getPublicKey().toString())
        .body(body)
        .when()
        .post(Signer.signPath(KeyType.BLS))
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("signature", equalToIgnoringCase(expectedSignature));
  }

  private String createBody(final KeyType keyType) throws JsonProcessingException {
    if (keyType == KeyType.SECP256K1) {
      return new JsonObject().put("data", DATA.toHexString()).toString();
    } else {
      return ETH_2_INTERFACE_OBJECT_MAPPER.writeValueAsString(
          Eth2RequestUtils.createBlockRequest());
    }
  }
}
