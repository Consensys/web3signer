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
package tech.pegasys.web3signer.core.service.jsonrpc.handlers.sendtransaction.transaction;

import tech.pegasys.web3signer.core.service.jsonrpc.JsonRpcRequest;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.sendtransaction.NonceProvider;

import java.math.BigInteger;
import java.util.List;

import org.web3j.utils.Base64String;

public class EeaPrivateNonceProvider implements NonceProvider {

  private final String accountAddress;
  private final Base64String privateFrom;
  private final List<Base64String> privateFor;
  private final VertxNonceRequestTransmitter vertxNonceRequestTransmitter;

  public EeaPrivateNonceProvider(
      final String accountAddress,
      final Base64String privateFrom,
      final List<Base64String> privateFor,
      final VertxNonceRequestTransmitter vertxNonceRequestTransmitter) {
    this.accountAddress = accountAddress;
    this.privateFrom = privateFrom;
    this.privateFor = privateFor;
    this.vertxNonceRequestTransmitter = vertxNonceRequestTransmitter;
  }

  @Override
  public BigInteger getNonce() {
    final JsonRpcRequest request = generateRequest();
    return vertxNonceRequestTransmitter.requestNonce(request);
  }

  protected JsonRpcRequest generateRequest() {
    final JsonRpcRequest request = new JsonRpcRequest("2.0", "priv_getEeaTransactionCount");
    request.setParams(new Object[] {accountAddress, privateFrom, privateFor});

    return request;
  }
}
