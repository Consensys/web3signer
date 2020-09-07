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
package tech.pegasys.web3signer.tests.comparison;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.web3signer.core.service.jsonrpc.FilecoinJsonRpcModule;
import tech.pegasys.web3signer.core.service.jsonrpc.FilecoinMessage;
import tech.pegasys.web3signer.core.service.jsonrpc.FilecoinSignedMessage;

import java.math.BigInteger;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariables;

@EnabledIfEnvironmentVariables({
  @EnabledIfEnvironmentVariable(named = "LOTUS_PORT", matches = ".*")
})
public class CompareSignMessageAcceptanceTest extends CompareApisAcceptanceTestBase {

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().registerModule(new FilecoinJsonRpcModule());

  @BeforeEach
  void initSigner() {
    super.initAndStartSigner(true);
  }

  @Test
  void compareRandomMessageSignaturesBetweenLotusAndEthSigner() {
    final int signatureCount =
        Integer.parseInt(System.getenv().getOrDefault("WEB3SIGNER_SIGN_MESSAGE_COUNT", "500"));

    final List<CompletableFuture<Void>> futures = Lists.newArrayList();
    for (int i = 0; i < signatureCount; i++) {
      futures.add(CompletableFuture.runAsync(this::testRandomMessage));
    }
    futures.forEach(CompletableFuture::join);
  }

  private FilecoinMessage createRandomMessage() {
    final Random rand = new Random();

    final byte[] paramByteArray = new byte[Math.abs(rand.nextInt(Integer.MAX_VALUE)) % 50];
    rand.nextBytes(paramByteArray);
    final Bytes params = Bytes.wrap(paramByteArray);

    return new FilecoinMessage(
        createRandomUInt64(rand),
        "t0" + String.format("%d", rand.nextInt(Integer.MAX_VALUE)),
        "t0" + String.format("%d", rand.nextInt(Integer.MAX_VALUE)),
        createRandomUInt64(rand),
        createRandomBigInt(rand),
        rand.nextLong(),
        createRandomBigInt(rand),
        createRandomBigInt(rand),
        createRandomUInt64(rand),
        params.toBase64String());
  }

  private UInt64 createRandomUInt64(final Random rand) {
    final byte[] bytes = new byte[8];
    rand.nextBytes(bytes);
    return UInt64.fromBytes(Bytes.wrap(bytes));
  }

  private BigInteger createRandomBigInt(final Random rand) {
    final int size = Math.abs(rand.nextInt(50));
    if (size == 0) {
      return BigInteger.ZERO;
    }
    final byte[] bytes = new byte[size];
    rand.nextBytes(bytes);
    return new BigInteger(bytes);
  }

  private void testRandomMessage() {
    final FilecoinMessage message = createRandomMessage();
    testMessage(message);
  }

  private void testMessage(final FilecoinMessage message) {
    final String jsonRequest;
    try {
      jsonRequest = OBJECT_MAPPER.writeValueAsString(message);
    } catch (JsonProcessingException e) {
      return;
    }
    addressMap
        .keySet()
        .parallelStream()
        .forEach(
            address -> {
              final FilecoinSignedMessage lotusFcSig =
                  LOTUS_NODE.walletSignMessage(address, message);
              final FilecoinSignedMessage signerFcSig = signer.walletSignMessage(address, message);

              assertThat(lotusFcSig.getMessage())
                  .isEqualToComparingFieldByField(signerFcSig.getMessage());
              assertThat(lotusFcSig.getSignature())
                  .overridingErrorMessage(
                      "Signature Comparison failed from msg = "
                          + jsonRequest
                          + ". With Priv key = ("
                          + addressMap.get(address).getType().toString()
                          + ") "
                          + addressMap.get(address).getPrivateKeyHex())
                  .isEqualToComparingFieldByField(signerFcSig.getSignature());
            });
  }

  @Test
  public void sendRawJsonMessage() throws JsonProcessingException {
    final String RAW_MSG =
        "{\"Nonce\":13117230712907131170,\"Version\":11213758133386406978,\"Value\":\"38164833859141271395520522612114109479178310324742244683413474\",\"Params\":\"G7omY+uWQuG6e/TLkSMzIywhushE/9F89AsXnE0P8dY=\",\"To\":\"t01157483643\",\"From\":\"t0269911794\",\"Method\":11918628191480157653,\"GasPremium\":\"-14648621342499321657422325578978005386855108954434874563422746186085866059298876960050856091161545992097\",\"GasLimit\":5168621066399406165,\"GasFeeCap\":\"16167912050521749707069396536\"}";
    final FilecoinMessage msg = OBJECT_MAPPER.readValue(RAW_MSG, FilecoinMessage.class);
    testMessage(msg);
  }
}
