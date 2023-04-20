/*
 * Copyright 2019 ConsenSys AG.
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

import tech.pegasys.web3signer.core.service.jsonrpc.EthSendTransactionJsonParameters;
import tech.pegasys.web3signer.core.service.jsonrpc.JsonDecoder;
import tech.pegasys.web3signer.core.service.jsonrpc.JsonRpcRequest;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.VertxRequestTransmitterFactory;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.sendtransaction.NonceProvider;

import java.util.List;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class TransactionFactory {

  private final VertxRequestTransmitterFactory transmitterFactory;
  private final JsonDecoder decoder;

  public TransactionFactory(
      final JsonDecoder decoder, final VertxRequestTransmitterFactory transmitterFactory) {
    this.transmitterFactory = transmitterFactory;
    this.decoder = decoder;
  }

  public Transaction createTransaction(final RoutingContext context, final JsonRpcRequest request) {
    final VertxNonceRequestTransmitter nonceRequestTransmitter =
        new VertxNonceRequestTransmitter(context.request().headers(), decoder, transmitterFactory);
    return createEthTransaction(request, nonceRequestTransmitter);
  }

  private Transaction createEthTransaction(
      final JsonRpcRequest request, final VertxNonceRequestTransmitter nonceRequestTransmitter) {
    final EthSendTransactionJsonParameters params =
        fromRpcRequestToJsonParam(EthSendTransactionJsonParameters.class, request);

    final NonceProvider ethNonceProvider =
        new EthNonceProvider(params.sender(), nonceRequestTransmitter);
    return new EthTransaction(params, ethNonceProvider, request.getId());
  }

  public <T> T fromRpcRequestToJsonParam(final Class<T> type, final JsonRpcRequest request) {

    final Object object;
    final Object params = request.getParams();
    if (params instanceof List) {
      @SuppressWarnings("unchecked")
      final List<Object> paramList = (List<Object>) params;
      if (paramList.size() != 1) {
        throw new IllegalArgumentException(
            type.getSimpleName()
                + " json Rpc requires a single parameter, request contained "
                + paramList.size());
      }
      object = paramList.get(0);
    } else {
      object = params;
    }

    final JsonObject receivedParams = JsonObject.mapFrom(object);

    return decoder.decodeValue(receivedParams.toBuffer(), type);
  }
}
