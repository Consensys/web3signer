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
package tech.pegasys.web3signer.tests.filecoin;

import static io.restassured.RestAssured.given;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import tech.pegasys.web3signer.core.service.jsonrpc.FcJsonRpc;
import tech.pegasys.web3signer.core.service.jsonrpc.FilecoinSignature;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.signing.filecoin.FilecoinAddress;
import tech.pegasys.web3signer.signing.filecoin.FilecoinNetwork;
import tech.pegasys.web3signer.tests.AcceptanceTestBase;

import java.util.Base64;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.github.arteam.simplejsonrpc.core.domain.Request;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;

public class FcSecpVerifyAcceptanceTest extends AcceptanceTestBase {
  // Public Key of Keystore stored in resource "secp256k1/wallet.json"
  public static final String PUBLIC_KEY_HEX_STRING =
      "09b02f8a5fddd222ade4ea4528faefc399623af3f736be3c44f03e2df22fb792f3931a4d9573d333ca74343305762a753388c3422a86d98b713fc91c1ea04842";
  final String dataString = Base64.getEncoder().encodeToString("Hello World".getBytes(UTF_8));
  final String expectedBase64Signature =
      "rapLdQPn5RQkiPeG6ij/0cCSBbsvHgO1Mx/rZVyM+HUrpdjamK6QSwSHzobfTaF2ehGevZ31t4XwZc7M5SnPwwA=";
  final FilecoinAddress identifier =
      FilecoinAddress.secpAddress(Bytes.fromHexString("04" + PUBLIC_KEY_HEX_STRING));

  @Test
  void receiveTrueResponseWhenSubmitValidVerifyRequestToFilecoinEndpoint() {
    startSigner(new SignerConfigurationBuilder().withMode("filecoin").build());

    final ValueNode id = JsonNodeFactory.instance.numberNode(1);
    final ObjectMapper mapper = JsonMapper.builder().build();

    final FilecoinSignature filecoinSignature =
        new FilecoinSignature(FcJsonRpc.SECP_VALUE, expectedBase64Signature);
    final JsonNode params =
        mapper.convertValue(
            List.of(identifier.encode(FilecoinNetwork.TESTNET), dataString, filecoinSignature),
            JsonNode.class);

    final Request request = new Request("2.0", "Filecoin.WalletVerify", params, id);
    final Response response = given().baseUri(signer.getUrl()).body(request).post(JSON_RPC_PATH);

    response
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("jsonrpc", equalTo("2.0"), "id", equalTo(id.asInt()));

    assertThat(response.body().jsonPath().getBoolean("result")).isEqualTo(true);
  }
}
