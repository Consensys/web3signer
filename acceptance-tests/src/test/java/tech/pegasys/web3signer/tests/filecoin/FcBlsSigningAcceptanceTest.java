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
import tech.pegasys.web3signer.core.signing.BlsArtifactSignature;
import tech.pegasys.web3signer.core.signing.FcBlsArtifactSigner;
import tech.pegasys.web3signer.core.signing.KeyType;
import tech.pegasys.web3signer.core.signing.filecoin.FilecoinAddress;
import tech.pegasys.web3signer.core.signing.filecoin.FilecoinNetwork;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.web3signer.tests.signing.SigningAcceptanceTestBase;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.github.arteam.simplejsonrpc.core.domain.Request;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;

public class FcBlsSigningAcceptanceTest extends SigningAcceptanceTestBase {

  private static final Bytes DATA = Bytes.wrap("Hello World".getBytes(UTF_8));
  private static final Bytes CID =
      Bytes.fromHexString(
          "0x0171a0e402201dc01772ee0171f5f614c673e3c7fa1107a8cf727bdf5a6dadb379e93c0d1d00");
  private static final String PRIVATE_KEY =
      "3ee2224386c82ffea477e2adf28a2929f5c349165a4196158c7f3a2ecca40f35";

  private static final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();
  private static final BLSSecretKey key =
      BLSSecretKey.fromBytes(Bytes32.fromHexString(PRIVATE_KEY));
  private static final BLSKeyPair keyPair = new BLSKeyPair(key);
  private static final BLSPublicKey publicKey = keyPair.getPublicKey();
  private static final FilecoinNetwork network = FilecoinNetwork.MAINNET;
  private static final FcBlsArtifactSigner signatureGenerator =
      new FcBlsArtifactSigner(keyPair, network);

  final FilecoinAddress identifier = FilecoinAddress.blsAddress(publicKey.toBytesCompressed());

  @Test
  void receiveASignatureWhenSubmitSigningRequestToFilecoinEndpoint() {
    final String configFilename = publicKey.toString().substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createUnencryptedYamlFileAt(keyConfigFile, PRIVATE_KEY, KeyType.BLS);
    setupFilecoinSigner();

    final ValueNode id = JsonNodeFactory.instance.numberNode(1);
    final ObjectMapper mapper = new ObjectMapper();
    final Map<String, String> metaData = Map.of("type", "message", "extra", DATA.toBase64String());
    final JsonNode params =
        mapper.convertValue(
            List.of(identifier.encode(FilecoinNetwork.MAINNET), CID.toBase64String(), metaData),
            JsonNode.class);

    final Request request = new Request("2.0", "Filecoin.WalletSign", params, id);
    final Response response = given().baseUri(signer.getUrl()).body(request).post(JSON_RPC_PATH);

    response
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("jsonrpc", equalTo("2.0"), "id", equalTo(id.asInt()));

    final BlsArtifactSignature expectedSignature = signatureGenerator.sign(CID);
    final Map<String, Object> result = response.body().jsonPath().get("result");
    assertThat(result.get("Type")).isEqualTo(2);
    assertThat(result.get("Data"))
        .isEqualTo(expectedSignature.getSignatureData().toBytesCompressed().toBase64String());
  }
}
