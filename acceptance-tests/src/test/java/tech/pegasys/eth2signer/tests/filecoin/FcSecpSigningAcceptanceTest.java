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
package tech.pegasys.eth2signer.tests.filecoin;

import static io.restassured.RestAssured.given;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import tech.pegasys.eth2signer.core.signing.KeyType;
import tech.pegasys.eth2signer.core.signing.filecoin.FilecoinAddress;
import tech.pegasys.eth2signer.core.signing.filecoin.FilecoinNetwork;
import tech.pegasys.eth2signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.eth2signer.tests.signing.SigningAcceptanceTestBase;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

import com.google.common.io.Resources;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;

public class FcSecpSigningAcceptanceTest extends SigningAcceptanceTestBase {

  private final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();

  // Public Key of Keystore stored in resource "secp256k1/wallet.json"
  public static final String PUBLIC_KEY_HEX_STRING =
      "09b02f8a5fddd222ade4ea4528faefc399623af3f736be3c44f03e2df22fb792f3931a4d9573d333ca74343305762a753388c3422a86d98b713fc91c1ea04842";

  @Test
  void receiveASignatureWhenSubmitSigningRequestToFilecoinEndpoint() throws URISyntaxException {
    final String keyPath =
        new File(Resources.getResource("secp256k1/wallet.json").toURI()).getAbsolutePath();

    final Path keyConfigFile = testDirectory.resolve("arbitrary_secp.yaml");

    metadataFileHelpers.createKeyStoreYamlFileAt(
        keyConfigFile, Path.of(keyPath), "pass", KeyType.SECP256K1);

    setupSigner();

    final FilecoinAddress identifier =
        FilecoinAddress.secpAddress(Bytes.fromHexString("04" + PUBLIC_KEY_HEX_STRING));
    final String dataString = Base64.getEncoder().encodeToString("Hello World".getBytes(UTF_8));
    final Response response =
        given()
            .baseUri(signer.getUrl())
            .body(
                "{\"jsonrpc\":\"2.0\",\"method\":\"Filecoin.WalletSign\",\"params\":["
                    + "\""
                    + identifier.encode(FilecoinNetwork.TESTNET)
                    + "\""
                    + ","
                    + "\""
                    + dataString
                    + "\""
                    + "],\"id\":1}")
            .post(JSON_RPC_PATH + "/filecoin");

    response
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("jsonrpc", equalTo("2.0"), "id", equalTo(1));

    final Map<String, Object> result = response.body().jsonPath().get("result");
    assertThat(result.get("Type")).isEqualTo(1);
    assertThat(result.get("Data"))
        .isEqualTo(
            "CyIqxcj+aAmEIWF0s27hLMT9/65DWFsTKeLr2a0SEoNO+gfQGM8L5FuS8rcr2CHy5YEGkyslpm8ZTJbZJIjEIwE=");
  }
}
