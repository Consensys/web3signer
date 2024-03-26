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
import static org.assertj.core.api.Assertions.assertThat;
import static org.web3j.crypto.Sign.signedMessageToKey;
import static tech.pegasys.web3signer.signing.secp256k1.EthPublicKeyUtils.createPublicKey;

import tech.pegasys.web3signer.core.service.http.SigningObjectMapperFactory;
import tech.pegasys.web3signer.core.service.http.handlers.signing.SigningExtensionBody;
import tech.pegasys.web3signer.core.service.http.handlers.signing.SigningExtensionType;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.signing.ConfigurationChainId;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.web3signer.signing.KeyType;

import java.io.File;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.SignatureException;
import java.security.interfaces.ECPublicKey;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.web3j.crypto.Sign;

public class Eth1ExtensionSigningAcceptanceTest extends SigningAcceptanceTestBase {
  public static final String PUBLIC_KEY_HEX_STRING =
      "0x09b02f8a5fddd222ade4ea4528faefc399623af3f736be3c44f03e2df22fb792f3931a4d9573d333ca74343305762a753388c3422a86d98b713fc91c1ea04842";

  private static final MetadataFileHelpers METADATA_FILE_HELPERS = new MetadataFileHelpers();
  private static final ObjectMapper JSON_MAPPER = SigningObjectMapperFactory.createObjectMapper();

  @BeforeEach
  void setup() throws URISyntaxException {
    final String keyPath =
        new File(Resources.getResource("secp256k1/wallet.json").toURI()).getAbsolutePath();

    METADATA_FILE_HELPERS.createKeyStoreYamlFileAt(
        testDirectory.resolve(PUBLIC_KEY_HEX_STRING + ".yaml"),
        Path.of(keyPath),
        "pass",
        KeyType.SECP256K1);

    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder
        .withKeyStoreDirectory(testDirectory)
        .withMode("eth1")
        .withChainIdProvider(new ConfigurationChainId(DEFAULT_CHAIN_ID))
        .withGenericSigningExtEnabled(true);

    startSigner(builder.build());
  }

  @Test
  void extensionSigningData() throws Exception {
    final SigningExtensionBody signingExtensionBody =
        new SigningExtensionBody(
            SigningExtensionType.PROOF_OF_VALIDATION,
            "AT",
            String.valueOf(System.currentTimeMillis()),
            PUBLIC_KEY_HEX_STRING);
    final String data = JSON_MAPPER.writeValueAsString(signingExtensionBody);
    System.out.println(JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(signingExtensionBody));
    final Response response = signer.signGenericData(PUBLIC_KEY_HEX_STRING, data, JSON);

    final JsonObject jsonBody = verifyStatusAndGetBody(response);

    final Bytes signature = Bytes.fromHexString(jsonBody.getString("signature"));
    assertThat(
            verifySECP256K1Signature(
                createPublicKey(Bytes.fromHexString(PUBLIC_KEY_HEX_STRING)),
                signingExtensionBody.signingData().toArray(),
                signature))
        .isTrue();

    final SigningExtensionBody message =
        jsonBody.getJsonObject("message").mapTo(SigningExtensionBody.class);
    assertThat(message).isEqualTo(signingExtensionBody);
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
    final Response response = signer.signGenericData("0x1234", data, JSON);
    response.then().statusCode(404);
  }

  @ParameterizedTest(name = "{index} - Testing Invalid Body: {0}")
  @ValueSource(strings = {"", "invalid", "{}", "{\"data\": \"invalid\"}"})
  void invalidBodyCausesBadRequestStatusCode(final String data) {
    final Response response = signer.signGenericData(PUBLIC_KEY_HEX_STRING, data, JSON);
    response.then().statusCode(400);
  }

  private JsonObject verifyStatusAndGetBody(final Response response) {
    response.then().statusCode(200).contentType(JSON);
    return new JsonObject(response.body().print());
  }

  private boolean verifySECP256K1Signature(
      final ECPublicKey publicKey, final byte[] data, final Bytes signature) {

    final byte[] r = signature.slice(0, 32).toArray();
    final byte[] s = signature.slice(32, 32).toArray();
    final byte[] v = signature.slice(64).toArray();
    final BigInteger messagePublicKey = recoverPublicKey(data, new Sign.SignatureData(v, r, s));
    return createPublicKey(messagePublicKey).equals(publicKey);
  }

  private BigInteger recoverPublicKey(final byte[] data, final Sign.SignatureData signature) {
    try {
      return signedMessageToKey(data, signature);
    } catch (final SignatureException e) {
      throw new IllegalStateException("signature cannot be recovered", e);
    }
  }
}
