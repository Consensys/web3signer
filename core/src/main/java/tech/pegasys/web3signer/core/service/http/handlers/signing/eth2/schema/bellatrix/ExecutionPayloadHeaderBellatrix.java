/*
 * Copyright 2025 ConsenSys AG.
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
package tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.bellatrix;

import tech.pegasys.teku.infrastructure.bytes.Bytes20;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.datastructures.execution.ExecutionPayload;
import tech.pegasys.teku.spec.datastructures.execution.ExecutionPayloadHeaderSchema;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.ExecutionPayloadHeader;

import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;

public class ExecutionPayloadHeaderBellatrix extends ExecutionPayloadCommon
    implements ExecutionPayloadHeader {

  @JsonProperty("transactions_root")
  public final Bytes32 transactionsRoot;

  @JsonCreator
  public ExecutionPayloadHeaderBellatrix(
      final @JsonProperty("parent_hash") Bytes32 parentHash,
      final @JsonProperty("fee_recipient") Bytes20 feeRecipient,
      final @JsonProperty("state_root") Bytes32 stateRoot,
      final @JsonProperty("receipts_root") Bytes32 receiptsRoot,
      final @JsonProperty("logs_bloom") Bytes logsBloom,
      final @JsonProperty("prev_randao") Bytes32 prevRandao,
      final @JsonProperty("block_number") UInt64 blockNumber,
      final @JsonProperty("gas_limit") UInt64 gasLimit,
      final @JsonProperty("gas_used") UInt64 gasUsed,
      final @JsonProperty("timestamp") UInt64 timestamp,
      final @JsonProperty("extra_data") Bytes extraData,
      final @JsonProperty("base_fee_per_gas") UInt256 baseFeePerGas,
      final @JsonProperty("block_hash") Bytes32 blockHash,
      final @JsonProperty("transactions_root") Bytes32 transactionsRoot) {
    super(
        parentHash,
        feeRecipient,
        stateRoot,
        receiptsRoot,
        logsBloom,
        prevRandao,
        blockNumber,
        gasLimit,
        gasUsed,
        timestamp,
        extraData,
        baseFeePerGas,
        blockHash);
    this.transactionsRoot = transactionsRoot;
  }

  public ExecutionPayloadHeaderBellatrix(
      final tech.pegasys.teku.spec.datastructures.execution.ExecutionPayloadHeader
          executionPayloadHeader) {
    super(
        executionPayloadHeader.getParentHash(),
        executionPayloadHeader.getFeeRecipient(),
        executionPayloadHeader.getStateRoot(),
        executionPayloadHeader.getReceiptsRoot(),
        executionPayloadHeader.getLogsBloom(),
        executionPayloadHeader.getPrevRandao(),
        executionPayloadHeader.getBlockNumber(),
        executionPayloadHeader.getGasLimit(),
        executionPayloadHeader.getGasUsed(),
        executionPayloadHeader.getTimestamp(),
        executionPayloadHeader.getExtraData(),
        executionPayloadHeader.getBaseFeePerGas(),
        executionPayloadHeader.getBlockHash());
    this.transactionsRoot = executionPayloadHeader.getTransactionsRoot();
  }

  public ExecutionPayloadHeaderBellatrix(final ExecutionPayload executionPayload) {
    super(
        executionPayload.getParentHash(),
        executionPayload.getFeeRecipient(),
        executionPayload.getStateRoot(),
        executionPayload.getReceiptsRoot(),
        executionPayload.getLogsBloom(),
        executionPayload.getPrevRandao(),
        executionPayload.getBlockNumber(),
        executionPayload.getGasLimit(),
        executionPayload.getGasUsed(),
        executionPayload.getTimestamp(),
        executionPayload.getExtraData(),
        executionPayload.getBaseFeePerGas(),
        executionPayload.getBlockHash());
    this.transactionsRoot = executionPayload.getTransactions().hashTreeRoot();
  }

  @Override
  public tech.pegasys.teku.spec.datastructures.execution.ExecutionPayloadHeader
      asInternalExecutionPayloadHeader(final ExecutionPayloadHeaderSchema<?> schema) {
    return schema.createExecutionPayloadHeader(
        payloadBuilder ->
            payloadBuilder
                .parentHash(parentHash)
                .feeRecipient(feeRecipient)
                .stateRoot(stateRoot)
                .receiptsRoot(receiptsRoot)
                .logsBloom(logsBloom)
                .prevRandao(prevRandao)
                .blockNumber(blockNumber)
                .gasLimit(gasLimit)
                .gasUsed(gasUsed)
                .timestamp(timestamp)
                .extraData(extraData)
                .baseFeePerGas(baseFeePerGas)
                .blockHash(blockHash)
                .transactionsRoot(transactionsRoot));
  }

  @Override
  public Optional<ExecutionPayloadHeaderBellatrix> toVersionBellatrix() {
    return Optional.of(this);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    final ExecutionPayloadHeaderBellatrix that = (ExecutionPayloadHeaderBellatrix) o;
    return Objects.equals(transactionsRoot, that.transactionsRoot);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), transactionsRoot);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("parentHash", parentHash)
        .add("feeRecipient", feeRecipient)
        .add("stateRoot", stateRoot)
        .add("receiptsRoot", receiptsRoot)
        .add("logsBloom", logsBloom)
        .add("prevRandao", prevRandao)
        .add("blockNumber", blockNumber)
        .add("gasLimit", gasLimit)
        .add("gasUsed", gasUsed)
        .add("timestamp", timestamp)
        .add("extraData", extraData)
        .add("baseFeePerGas", baseFeePerGas)
        .add("blockHash", blockHash)
        .add("transactionsRoot", transactionsRoot)
        .toString();
  }
}
