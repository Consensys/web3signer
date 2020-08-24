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
import static org.hamcrest.Matchers.equalTo;

import com.fasterxml.jackson.core.JsonProcessingException;
import tech.pegasys.eth2signer.core.service.jsonrpc.FilecoinSignedMessage;
import tech.pegasys.eth2signer.core.signing.KeyType;
import tech.pegasys.eth2signer.core.signing.filecoin.FilecoinAddress;
import tech.pegasys.eth2signer.core.signing.filecoin.FilecoinNetwork;
import tech.pegasys.eth2signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.eth2signer.tests.signing.SigningAcceptanceTestBase;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.bls.BLSSecretKey;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.github.arteam.simplejsonrpc.core.domain.Request;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;

public class FcSignMessageAcceptanceTest extends SigningAcceptanceTestBase {

  private static final String PRIVATE_KEY = "z38sVnSEnswoEHFC9e4g/aPk96c1NvXt425UKv/tKz0=";

  private static final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();
  private static final BLSSecretKey key =
      BLSSecretKey.fromBytes(Bytes.fromBase64String(PRIVATE_KEY));
  private static final BLSKeyPair keyPair = new BLSKeyPair(key);
  private static final BLSPublicKey publicKey = keyPair.getPublicKey();
  private static final FilecoinNetwork network = FilecoinNetwork.TESTNET;
  final FilecoinAddress sender = FilecoinAddress.blsAddress(publicKey.toBytes());

  @Test
  void fcSignMessageReturnsASignedMessage() throws JsonProcessingException {
    final String configFilename = publicKey.toString().substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createUnencryptedYamlFileAt(keyConfigFile,
        key.getSecretKey().toBytes().toUnprefixedHexString(), KeyType.BLS);

    setupSigner();

    final ValueNode id = JsonNodeFactory.instance.numberNode(1);
    ObjectMapper mapper = new ObjectMapper();

    final Map<String, Object> paramMap = new HashMap<>();

    paramMap.put("Version", 9);
    paramMap.put("To", "t01234");
    paramMap.put("From", sender.encode(network));
    paramMap.put("Nonce", 42);
    paramMap.put("Value", "0");
    paramMap.put("GasPrice", "0");
    paramMap.put("GasLimit", 9);
    paramMap.put("Method", 1);
    paramMap.put("Params", "Ynl0ZSBhcnJheQ==");

    final Request request =
        new Request(
            "2.0", "Filecoin.WalletSignMessage", mapper.convertValue(paramMap, JsonNode.class), id);
    final Response response =
        given().baseUri(signer.getUrl()).body(request).post(JSON_RPC_PATH + "/filecoin");

    response
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("jsonrpc", equalTo("2.0"), "id", equalTo(id.asInt()));

    final FilecoinSignedMessage signedMessage = response.body().jsonPath().getObject("result", FilecoinSignedMessage.class);
  }
}
