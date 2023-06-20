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

import static tech.pegasys.web3signer.core.service.jsonrpc.RpcUtil.JSON_RPC_VERSION;

import tech.pegasys.web3signer.core.service.jsonrpc.EthSendTransactionJsonParameters;
import tech.pegasys.web3signer.core.service.jsonrpc.JsonRpcRequest;
import tech.pegasys.web3signer.core.service.jsonrpc.JsonRpcRequestId;
import tech.pegasys.web3signer.core.service.jsonrpc.exceptions.JsonRpcException;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.sendtransaction.EnclaveLookupIdProvider;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.sendtransaction.NonceProvider;
import tech.pegasys.web3signer.core.service.jsonrpc.response.JsonRpcError;

import java.math.BigInteger;
import java.util.List;

import com.google.common.base.MoreObjects;
import io.vertx.core.json.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.Sign.SignatureData;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpType;
import org.web3j.utils.Base64String;

public class GoQuorumPrivateTransaction extends EthTransaction {

  private final List<Base64String> privateFor;
  private final EnclaveLookupIdProvider enclaveLookupIdProvider;
  private String lookupId = "";

  public static GoQuorumPrivateTransaction from(
      final EthSendTransactionJsonParameters transactionJsonParameters,
      final NonceProvider nonceProvider,
      final EnclaveLookupIdProvider enclaveLookupIdProvider,
      final JsonRpcRequestId id) {

    if (transactionJsonParameters.privateFor().isEmpty()) {
      throw new IllegalArgumentException("Transaction does not contain a valid privateFor list.");
    }

    if (transactionJsonParameters.value().isPresent()
        && !transactionJsonParameters.value().get().equals(BigInteger.ZERO)) {
      throw new JsonRpcException(JsonRpcError.ETHER_VALUE_NOT_SUPPORTED);
    }

    return new GoQuorumPrivateTransaction(
        transactionJsonParameters,
        nonceProvider,
        enclaveLookupIdProvider,
        id,
        transactionJsonParameters.privateFor().get());
  }

  private GoQuorumPrivateTransaction(
      final EthSendTransactionJsonParameters transactionJsonParameters,
      final NonceProvider nonceProvider,
      final EnclaveLookupIdProvider enclaveLookupIdProvider,
      final JsonRpcRequestId id,
      final List<Base64String> privateFor) {
    super(transactionJsonParameters, nonceProvider, id);
    this.privateFor = privateFor;
    this.enclaveLookupIdProvider = enclaveLookupIdProvider;
  }

  @Override
  @NotNull
  public String getJsonRpcMethodName() {
    return "eth_sendRawPrivateTransaction";
  }

  @Override
  public void updateFieldsIfRequired() {

    if (!this.isNonceUserSpecified()) {
      this.nonce = nonceProvider.getNonce();
    }

    final String data =
        this.transactionJsonParameters
            .data()
            .orElseThrow(
                () ->
                    new IllegalArgumentException("GoQuorum private transaction must contain data"));
    this.lookupId = enclaveLookupIdProvider.getLookupId(data);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("transactionJsonParameters", transactionJsonParameters)
        .add("nonceProvider", nonceProvider)
        .add("id", id)
        .add("nonce", nonce)
        .add("enclaveLookupIdProvider", enclaveLookupIdProvider)
        .add("lookupId", lookupId)
        .toString();
  }

  private JsonObject getGoQuorumRawTxJsonParams() {
    final JsonObject jsonObject = new JsonObject();
    if (this.transactionJsonParameters.privateFrom().isPresent()) {
      jsonObject.put("privateFrom", this.transactionJsonParameters.privateFrom().get().toString());
    }
    jsonObject.put("privateFor", Base64String.unwrapList(privateFor));
    return jsonObject;
  }

  @Override
  public byte[] rlpEncode(final SignatureData signatureData) {
    final RawTransaction rawTransaction = createTransaction();
    final List<RlpType> values = TransactionEncoder.asRlpValues(rawTransaction, signatureData);
    final RlpList rlpList = new RlpList(values);
    return RlpEncoder.encode(rlpList);
  }

  @Override
  public JsonRpcRequest jsonRpcRequest(
      final String signedTransactionHexString, final JsonRpcRequestId id) {
    final JsonRpcRequest request = new JsonRpcRequest(JSON_RPC_VERSION, getJsonRpcMethodName());
    request.setParams(new Object[] {signedTransactionHexString, getGoQuorumRawTxJsonParams()});
    request.setId(id);
    return request;
  }

  @Override
  protected RawTransaction createTransaction() {
    return RawTransaction.createTransaction(
        nonce,
        transactionJsonParameters.gasPrice().orElse(DEFAULT_GAS_PRICE),
        transactionJsonParameters.gas().orElse(DEFAULT_GAS),
        transactionJsonParameters.receiver().orElse(DEFAULT_TO),
        lookupId);
  }
}
