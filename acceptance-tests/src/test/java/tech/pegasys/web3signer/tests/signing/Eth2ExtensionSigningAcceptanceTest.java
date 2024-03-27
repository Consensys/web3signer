/*
 * Copyright 2024 ConsenSys AG.
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

import static io.restassured.http.ContentType.JSON;
import static io.restassured.http.ContentType.TEXT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.teku.spec.SpecMilestone.DENEB;

import tech.pegasys.teku.bls.BLS;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.teku.bls.BLSSignature;
import tech.pegasys.teku.spec.networks.Eth2Network;
import tech.pegasys.web3signer.core.service.http.SigningObjectMapperFactory;
import tech.pegasys.web3signer.core.service.http.handlers.signing.SigningExtensionBody;
import tech.pegasys.web3signer.core.service.http.handlers.signing.SigningExtensionType;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.web3signer.signing.KeyType;

import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.BaseEncoding;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

public class Eth2ExtensionSigningAcceptanceTest extends SigningAcceptanceTestBase {

  private static final String PRIVATE_KEY =
      "3ee2224386c82ffea477e2adf28a2929f5c349165a4196158c7f3a2ecca40f35";
  private static final MetadataFileHelpers METADATA_FILE_HELPERS = new MetadataFileHelpers();
  private static final BLSSecretKey KEY =
      BLSSecretKey.fromBytes(Bytes32.fromHexString(PRIVATE_KEY));
  private static final BLSKeyPair KEY_PAIR = new BLSKeyPair(KEY);
  private static final BLSPublicKey PUBLIC_KEY = KEY_PAIR.getPublicKey();
  private static final ObjectMapper JSON_MAPPER = SigningObjectMapperFactory.createObjectMapper();

  @BeforeEach
  void setup() {
    final String configFilename = PUBLIC_KEY.toString().substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    METADATA_FILE_HELPERS.createUnencryptedYamlFileAt(keyConfigFile, PRIVATE_KEY, KeyType.BLS);

    setForkEpochsAndStartSigner(
        new SignerConfigurationBuilder()
            .withKeyStoreDirectory(testDirectory)
            .withMode("eth2")
            .withNetwork(Eth2Network.MINIMAL.configName())
            .withSigningExtEnabled(true),
        DENEB);
  }

  @ParameterizedTest
  @EnumSource(
      value = ContentType.class,
      names = {"ANY", "JSON", "TEXT"})
  void extensionSigningData(ContentType acceptMediaType) throws Exception {
    SigningExtensionBody signingExtensionBody =
        new SigningExtensionBody(
            SigningExtensionType.PROOF_OF_VALIDATION,
            "AT",
            String.valueOf(System.currentTimeMillis()),
            PUBLIC_KEY.toString());
    var payload = JSON_MAPPER.writeValueAsString(signingExtensionBody);

    var response = signer.signExtensionPayload(PUBLIC_KEY.toString(), payload, acceptMediaType);
    response.then().statusCode(200);

    String encodedSignature;
    if (acceptMediaType == TEXT) {
      response.then().contentType(TEXT);
      encodedSignature = response.body().print();
    } else {
      response.then().contentType(JSON);
      encodedSignature = new JsonObject(response.body().print()).getString("signature");
    }

    /*
     Header = {"alg"="BLS", typ="BLS_SIG"}
     Data_To_Sign = Base64(Header) + "." + Base64(payload)
     signature = Data_To_Sign + "." + Base64(BLS.sign(Data_To_Sign))
    */

    final String headerBase64 = encodedSignature.substring(0, encodedSignature.indexOf('.'));
    final String payloadBase64 =
        encodedSignature.substring(
            encodedSignature.indexOf('.') + 1, encodedSignature.lastIndexOf('.'));
    final String blsSigBase64 = encodedSignature.substring(encodedSignature.lastIndexOf('.') + 1);

    var headerDecoded =
        new JsonObject(new String(BaseEncoding.base64().decode(headerBase64), UTF_8));
    var payloadDecoded =
        new JsonObject(new String(BaseEncoding.base64().decode(payloadBase64), UTF_8));
    var blsSigDecoded = Bytes.fromBase64String(blsSigBase64);

    assertThat(headerDecoded.getString("alg")).isEqualTo("BLS");
    assertThat(headerDecoded.getString("typ")).isEqualTo("BLS_SIG");

    assertThat(payloadDecoded.mapTo(SigningExtensionBody.class)).isEqualTo(signingExtensionBody);

    var blsSignature = BLSSignature.fromBytesCompressed(blsSigDecoded);
    final String dataSigned = headerBase64 + "." + payloadBase64;
    assertThat(BLS.verify(PUBLIC_KEY, Bytes.wrap(dataSigned.getBytes(UTF_8)), blsSignature))
        .isTrue();
  }

  @Test
  void invalidIdentifierCausesNotFound() throws Exception {
    SigningExtensionBody signingExtensionBody =
        new SigningExtensionBody(
            SigningExtensionType.PROOF_OF_VALIDATION,
            "AT",
            String.valueOf(System.currentTimeMillis()),
            "0x1234");
    String data = JSON_MAPPER.writeValueAsString(signingExtensionBody);
    final Response response = signer.signExtensionPayload("0x1234", data, JSON);
    response.then().statusCode(404);
  }

  @ParameterizedTest(name = "{index} - Testing Invalid Body: {0}")
  @ValueSource(strings = {"", "invalid", "{}", "{\"data\": \"invalid\"}"})
  void invalidBodyCausesBadRequestStatusCode(final String data) {
    final Response response = signer.signExtensionPayload(PUBLIC_KEY.toString(), data, JSON);
    response.then().statusCode(400);
  }

  @Test
  void invalidSignExtensionTypeCausesBadRequestStatusCode() throws Exception {
    SigningExtensionBody signingExtensionBody =
        new SigningExtensionBody(
            SigningExtensionType.PROOF_OF_VALIDATION,
            "AT",
            String.valueOf(System.currentTimeMillis()),
            PUBLIC_KEY.toString());
    var payload = JSON_MAPPER.writeValueAsString(signingExtensionBody);
    payload = payload.replace("PROOF_OF_VALIDATION", "INVALID_TYPE");
    var response = signer.signExtensionPayload(PUBLIC_KEY.toString(), payload, JSON);
    response.then().statusCode(400);
  }
}
