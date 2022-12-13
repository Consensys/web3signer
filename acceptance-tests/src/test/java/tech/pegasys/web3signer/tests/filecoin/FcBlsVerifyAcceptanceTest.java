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

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.web3signer.core.service.jsonrpc.FcJsonRpc;
import tech.pegasys.web3signer.core.service.jsonrpc.FilecoinSignature;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.signing.BlsArtifactSignature;
import tech.pegasys.web3signer.signing.FcBlsArtifactSigner;
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
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;

public class FcBlsVerifyAcceptanceTest extends AcceptanceTestBase {
  private static final String DATA_STRING =
      Base64.getEncoder().encodeToString("Hello World".getBytes(UTF_8));
  private static final String PRIVATE_KEY =
      "5abc1334d98d1432150df310f9d2fd51780a2b8a7489891f5d4ab9e77e6fb169";

  private static final BLSSecretKey KEY =
      BLSSecretKey.fromBytes(Bytes32.fromHexString(PRIVATE_KEY));
  private static final BLSKeyPair KEY_PAIR = new BLSKeyPair(KEY);
  private static final BLSPublicKey PUBLIC_KEY = KEY_PAIR.getPublicKey();
  private static final FilecoinNetwork NETWORK = FilecoinNetwork.TESTNET;
  private static final FcBlsArtifactSigner SIGNATURE_GENERATOR =
      new FcBlsArtifactSigner(KEY_PAIR, NETWORK);
  private static final BlsArtifactSignature EXPECTED_SIGNATURE =
      SIGNATURE_GENERATOR.sign(Bytes.fromBase64String(DATA_STRING));

  final FilecoinAddress identifier = FilecoinAddress.blsAddress(PUBLIC_KEY.toBytesCompressed());

  @Test
  void receiveTrueResponseWhenSubmitValidVerifyRequestToFilecoinEndpoint() {
    startSigner(new SignerConfigurationBuilder().withMode("filecoin").build());

    final ValueNode id = JsonNodeFactory.instance.numberNode(1);
    final ObjectMapper mapper = JsonMapper.builder().build();

    final FilecoinSignature filecoinSignature =
        new FilecoinSignature(
            FcJsonRpc.BLS_VALUE,
            EXPECTED_SIGNATURE.getSignatureData().toBytesCompressed().toBase64String());
    final JsonNode params =
        mapper.convertValue(
            List.of(identifier.encode(NETWORK), DATA_STRING, filecoinSignature), JsonNode.class);

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
