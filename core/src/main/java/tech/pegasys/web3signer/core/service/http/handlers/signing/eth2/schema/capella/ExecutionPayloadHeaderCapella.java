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
package tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.capella;

import tech.pegasys.teku.infrastructure.bytes.Bytes20;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.datastructures.execution.ExecutionPayloadHeader;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.bellatrix.ExecutionPayloadHeaderBellatrix;

import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;

public class ExecutionPayloadHeaderCapella extends ExecutionPayloadHeaderBellatrix {

  @JsonProperty("withdrawals_root")
  public final Bytes32 withdrawalsRoot;

  @JsonCreator
  public ExecutionPayloadHeaderCapella(
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
      final @JsonProperty("transactions_root") Bytes32 transactionsRoot,
      final @JsonProperty("withdrawals_root") Bytes32 withdrawalsRoot) {
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
        blockHash,
        transactionsRoot);
    this.withdrawalsRoot = withdrawalsRoot;
  }

  public ExecutionPayloadHeaderCapella(final ExecutionPayloadHeader executionPayloadHeader) {
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
        executionPayloadHeader.getBlockHash(),
        executionPayloadHeader.getTransactionsRoot());
    this.withdrawalsRoot = executionPayloadHeader.getOptionalWithdrawalsRoot().orElseThrow();
  }

  @Override
  public Optional<ExecutionPayloadHeaderCapella> toVersionCapella() {
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
    final ExecutionPayloadHeaderCapella that = (ExecutionPayloadHeaderCapella) o;
    return Objects.equals(withdrawalsRoot, that.withdrawalsRoot);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), withdrawalsRoot);
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
        .add("withdrawalsRoot", withdrawalsRoot)
        .toString();
  }
}
