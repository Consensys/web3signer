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
package tech.pegasys.web3signer.core.service.jsonrpc.handlers.sendtransaction.transaction;

import org.apache.tuweni.bytes.Bytes;
import org.web3j.crypto.Sign;
import org.web3j.rlp.RlpString;
import org.web3j.utils.Numeric;
import tech.pegasys.web3signer.core.service.jsonrpc.EthSendTransactionJsonParameters;
import tech.pegasys.web3signer.core.service.jsonrpc.JsonRpcRequest;
import tech.pegasys.web3signer.core.service.jsonrpc.JsonRpcRequestId;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.sendtransaction.NonceProvider;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.MoreObjects;
import org.jetbrains.annotations.NotNull;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.Sign.SignatureData;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpType;

public class EthTransaction implements Transaction {

  private static final String JSON_RPC_METHOD = "eth_sendRawTransaction";
  private final long chainId;
  protected final EthSendTransactionJsonParameters transactionJsonParameters;
  protected final NonceProvider nonceProvider;
  protected final JsonRpcRequestId id;
  protected BigInteger nonce;

  public EthTransaction(
      final long chainId,
      final EthSendTransactionJsonParameters transactionJsonParameters,
      final NonceProvider nonceProvider,
      final JsonRpcRequestId id) {
    this.chainId = chainId;
    this.transactionJsonParameters = transactionJsonParameters;
    this.id = id;
    this.nonceProvider = nonceProvider;
    this.nonce = transactionJsonParameters.nonce().orElse(null);
  }

  @Override
  public void updateFieldsIfRequired() {
    if (!this.isNonceUserSpecified()) {
      this.nonce = nonceProvider.getNonce();
    }
  }

  @Override
  @NotNull
  public String getJsonRpcMethodName() {
    return JSON_RPC_METHOD;
  }

  @Override
  public byte[] rlpEncode(final SignatureData signatureData) {
    final RawTransaction rawTransaction = createTransaction();
    final List<RlpType> values = TransactionEncoder.asRlpValues(rawTransaction, signatureData);
    final RlpList rlpList = new RlpList(values);
    return RlpEncoder.encode(rlpList);
  }

  @Override
  public byte[] rlpEncodeToSign() {
    if (!isEip4844()) {
      return rlpEncode(null);
    }
    // eip4844 transaction should be rlp encoded in another way
    List<RlpType> resultTx = new ArrayList<>();

    resultTx.add(RlpString.create(chainId));
    resultTx.add(RlpString.create(nonce));
    resultTx.add(RlpString.create(transactionJsonParameters.maxPriorityFeePerGas().orElseThrow()));
    resultTx.add(RlpString.create(transactionJsonParameters.maxFeePerGas().orElseThrow()));
    resultTx.add(RlpString.create(transactionJsonParameters.gas().orElse(DEFAULT_GAS)));

    String to = transactionJsonParameters.receiver().orElse(DEFAULT_TO);
    if (to.length() > 0) {
      resultTx.add(RlpString.create(Numeric.hexStringToByteArray(to)));
    } else {
      resultTx.add(RlpString.create(""));
    }

    resultTx.add(RlpString.create(transactionJsonParameters.value().orElse(DEFAULT_VALUE)));
    byte[] data = Numeric.hexStringToByteArray(transactionJsonParameters.data().orElse(DEFAULT_DATA));
    resultTx.add(RlpString.create(data));

    // access list
    resultTx.add(new RlpList());

    // Blob Transaction: max_fee_per_blob_gas and versioned_hashes
    resultTx.add(RlpString.create(transactionJsonParameters.maxFeePerBlobGas().orElseThrow()));
    resultTx.add(new RlpList(getRlpVersionedHashes(transactionJsonParameters.versionedHashes().orElseThrow())));

    return RlpEncoder.encode(new RlpList(resultTx));
  }

  @Override
  public boolean isNonceUserSpecified() {
    return transactionJsonParameters.nonce().isPresent();
  }

  @Override
  public String sender() {
    return transactionJsonParameters.sender();
  }

  @Override
  public JsonRpcRequest jsonRpcRequest(
      final String signedTransactionHexString, final JsonRpcRequestId id) {
    return Transaction.jsonRpcRequest(signedTransactionHexString, id, JSON_RPC_METHOD);
  }

  @Override
  public JsonRpcRequestId getId() {
    return id;
  }

  @Override
  public boolean isEip1559() {
    return transactionJsonParameters.maxPriorityFeePerGas().isPresent()
        && transactionJsonParameters.maxFeePerGas().isPresent();
  }

  @Override
  public boolean isEip4844() {
    return transactionJsonParameters.maxFeePerBlobGas().isPresent()
        && transactionJsonParameters.versionedHashes().isPresent();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("chainId", chainId)
        .add("transactionJsonParameters", transactionJsonParameters)
        .add("nonceProvider", nonceProvider)
        .add("id", id)
        .add("nonce", nonce)
        .toString();
  }

  protected RawTransaction createTransaction() {
    if (isEip4844()) {
      return RawTransaction.createTransaction(
          chainId,
          nonce,
          transactionJsonParameters.maxPriorityFeePerGas().orElseThrow(),
          transactionJsonParameters.maxFeePerGas().orElseThrow(),
          transactionJsonParameters.gas().orElse(DEFAULT_GAS),
          transactionJsonParameters.receiver().orElse(DEFAULT_TO),
          transactionJsonParameters.value().orElse(DEFAULT_VALUE),
          transactionJsonParameters.data().orElse(DEFAULT_DATA),
          transactionJsonParameters.maxFeePerBlobGas().orElseThrow(),
          transactionJsonParameters.versionedHashes().orElseThrow());
    } else if (isEip1559()) {
      return RawTransaction.createTransaction(
          chainId,
          nonce,
          transactionJsonParameters.gas().orElse(DEFAULT_GAS),
          transactionJsonParameters.receiver().orElse(DEFAULT_TO),
          transactionJsonParameters.value().orElse(DEFAULT_VALUE),
          transactionJsonParameters.data().orElse(DEFAULT_DATA),
          transactionJsonParameters.maxPriorityFeePerGas().orElseThrow(),
          transactionJsonParameters.maxFeePerGas().orElseThrow());
    } else {
      return RawTransaction.createTransaction(
          nonce,
          transactionJsonParameters.gasPrice().orElse(DEFAULT_GAS_PRICE),
          transactionJsonParameters.gas().orElse(DEFAULT_GAS),
          transactionJsonParameters.receiver().orElse(DEFAULT_TO),
          transactionJsonParameters.value().orElse(DEFAULT_VALUE),
          transactionJsonParameters.data().orElse(DEFAULT_DATA));
    }
  }

  public List<RlpType> getRlpVersionedHashes(List<Bytes> versionedHashes) {
    return versionedHashes.stream()
            .map(hash -> RlpString.create(hash.toArray()))
            .collect(Collectors.toList());
  }
}
