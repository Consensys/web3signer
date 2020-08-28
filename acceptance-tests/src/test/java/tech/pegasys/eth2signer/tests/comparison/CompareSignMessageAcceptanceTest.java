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
package tech.pegasys.eth2signer.tests.comparison;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.eth2signer.dsl.lotus.FilecoinJsonRequests.walletSignMessage;
import static tech.pegasys.eth2signer.dsl.lotus.LotusNode.OBJECT_MAPPER;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.pegasys.eth2signer.core.service.jsonrpc.FilecoinSignedMessage;

public class CompareSignMessageAcceptanceTest extends CompareApisAcceptanceTestBase {

  @BeforeEach
  void initSigner() {
    super.initAndStartSigner(true);
  }

  @Test
  void compareRandomMessageSignaturesBetweenLotusAndEthSigner() {
    final List<CompletableFuture<Void>> futures = Lists.newArrayList();
    for (int i = 0; i < 500; i++) {
      futures.add(CompletableFuture.runAsync(this::testRandomMessage));
    }
    futures.forEach(CompletableFuture::join);
  }

  private Map<String, Object> createRandomMessage() {
    final Random rand = new Random();

    final byte[] paramByteArray = new byte[Math.abs(rand.nextInt(Integer.MAX_VALUE)) % 50];
    rand.nextBytes(paramByteArray);
    final Bytes params = Bytes.wrap(paramByteArray);

    final Map<String, Object> messageMap = new HashMap<>();
    messageMap.put("Version", createRandomUInt64(rand));
    messageMap.put("To", "t01234");
    messageMap.put("From", "t01234");
    messageMap.put("Nonce", createRandomUInt64(rand));
    messageMap.put("Value", createRandomBigInt(rand));
    messageMap.put("GasLimit", rand.nextLong());
    messageMap.put("GasFeeCap", createRandomBigInt(rand));
    messageMap.put("GasPremium", createRandomBigInt(rand));
    messageMap.put("Method", createRandomUInt64(rand));
    messageMap.put("Params", params);

    return messageMap;
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
    final Map<String, Object> message = createRandomMessage();
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
                  walletSignMessage(LOTUS_NODE.getJsonRpcClient(), address, message);
              final FilecoinSignedMessage signerFcSig =
                  walletSignMessage(getSignerJsonRpcClient(), address, message);

              assertThat(lotusFcSig.getMessage())
                  .isEqualToComparingFieldByField(signerFcSig.getMessage());
              assertThat(lotusFcSig.getSignature())
                  .overridingErrorMessage("Signature Comparison failed from msg = " + jsonRequest)
                  .isEqualToComparingFieldByField(signerFcSig.getSignature());
            });
  }
}
