/*
 * Copyright 2023 ConsenSys AG.
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
package tech.pegasys.web3signer.core.jsonrpcproxy;

import static java.util.Collections.singletonList;
import static org.web3j.crypto.Keys.getAddress;
import static tech.pegasys.web3signer.core.service.jsonrpc.response.JsonRpcError.SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT;

import tech.pegasys.web3signer.core.jsonrpcproxy.support.EthSignTypedData;
import tech.pegasys.web3signer.core.service.jsonrpc.response.JsonRpcErrorResponse;
import tech.pegasys.web3signer.core.service.jsonrpc.response.JsonRpcSuccessResponse;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.Map;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.json.Json;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Keys;
import org.web3j.protocol.core.Request;

public class EthSignTypedDataIntegrationTest extends IntegrationTestBase {

  private static final String eip712Json =
      """
          {
            "domain": {
              "name": "Ether Mail",
              "version": "1",
              "chainId": 1,
              "verifyingContract": "0xCcCCccccCCCCcCCCCCCcCcCccCcCCCcCcccccccC"
            },
            "message": {
              "from": {
                "name": "Cow",
                "wallet": "0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826"
              },
              "to": {
                "name": "Bob",
                "wallet": "0xbBbBBBBbbBBBbbbBbbBbbbbBBbBbbbbBbBbbBBbB"
              },
              "contents": "Hello, Bob!"
            },
            "primaryType": "Mail",
            "types": {
              "EIP712Domain": [
                {
                  "name": "name",
                  "type": "string"
                },
                {
                  "name": "version",
                  "type": "string"
                },
                {
                  "name": "chainId",
                  "type": "uint256"
                },
                {
                  "name": "verifyingContract",
                  "type": "address"
                }
              ],
              "Mail": [
                {
                  "name": "from",
                  "type": "Person"
                },
                {
                  "name": "to",
                  "type": "Person"
                },
                {
                  "name": "contents",
                  "type": "string"
                }
              ],
              "Person": [
                {
                  "name": "name",
                  "type": "string"
                },
                {
                  "name": "wallet",
                  "type": "address"
                }
              ]
            }
          }
          """;

  @Test
  void ethSignTypedDataSignsDataWhenAnUnlockedAccountIsPassed() {
    final Request<?, EthSignTypedData> requestBody =
        new Request<>(
            "eth_signTypedData",
            Arrays.asList(unlockedAccount, eip712Json),
            null,
            EthSignTypedData.class);

    final Iterable<Map.Entry<String, String>> expectedHeaders =
        singletonList(ImmutablePair.of("Content", HttpHeaderValues.APPLICATION_JSON.toString()));

    final JsonRpcSuccessResponse responseBody =
        new JsonRpcSuccessResponse(
            requestBody.getId(),
            "0x590dc3b33e5055bfc5f4c2da5aa1f890340faefd2ba287cef9c019198f0391d95f85ff5f65471e1f50b541b81e720e353cd3cdaa41b9e08a5e1cd7134cb909711c");

    sendPostRequestAndVerifyResponse(
        request.web3Signer(Json.encode(requestBody)),
        response.web3Signer(expectedHeaders, Json.encode(responseBody)));
  }

  @Test
  void ethSignTypedDataDoNotSignMessageWhenSignerAccountIsNotUnlocked()
      throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
    final String A_RANDOM_ADDRESS = getAddress(Keys.createEcKeyPair().getPublicKey());

    final Request<?, EthSignTypedData> requestBody =
        new Request<>(
            "eth_signTypedData",
            Arrays.asList(A_RANDOM_ADDRESS, eip712Json),
            null,
            EthSignTypedData.class);

    final Iterable<Map.Entry<String, String>> expectedHeaders =
        singletonList(ImmutablePair.of("Content", HttpHeaderValues.APPLICATION_JSON.toString()));

    final JsonRpcErrorResponse responseBody =
        new JsonRpcErrorResponse(requestBody.getId(), SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT);

    sendPostRequestAndVerifyResponse(
        request.web3Signer(Json.encode(requestBody)),
        response.web3Signer(expectedHeaders, Json.encode(responseBody)));
  }
}
