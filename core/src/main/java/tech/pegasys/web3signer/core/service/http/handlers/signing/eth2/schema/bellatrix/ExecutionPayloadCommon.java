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

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;

public abstract class ExecutionPayloadCommon {

  @JsonProperty("parent_hash")
  public final Bytes32 parentHash;

  @JsonProperty("fee_recipient")
  public final Bytes20 feeRecipient;

  @JsonProperty("state_root")
  public final Bytes32 stateRoot;

  @JsonProperty("receipts_root")
  public final Bytes32 receiptsRoot;

  @JsonProperty("logs_bloom")
  public final Bytes logsBloom;

  @JsonProperty("prev_randao")
  public final Bytes32 prevRandao;

  @JsonProperty("block_number")
  public final UInt64 blockNumber;

  @JsonProperty("gas_limit")
  public final UInt64 gasLimit;

  @JsonProperty("gas_used")
  public final UInt64 gasUsed;

  public final UInt64 timestamp;

  @JsonProperty("extra_data")
  public final Bytes extraData;

  @JsonProperty("base_fee_per_gas")
  public final UInt256 baseFeePerGas;

  @JsonProperty("block_hash")
  public final Bytes32 blockHash;

  public ExecutionPayloadCommon(
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
      final @JsonProperty("block_hash") Bytes32 blockHash) {
    this.parentHash = parentHash;
    this.feeRecipient = feeRecipient;
    this.stateRoot = stateRoot;
    this.receiptsRoot = receiptsRoot;
    this.logsBloom = logsBloom;
    this.prevRandao = prevRandao;
    this.blockNumber = blockNumber;
    this.gasLimit = gasLimit;
    this.gasUsed = gasUsed;
    this.timestamp = timestamp;
    this.extraData = extraData;
    this.baseFeePerGas = baseFeePerGas;
    this.blockHash = blockHash;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final ExecutionPayloadCommon that = (ExecutionPayloadCommon) o;
    return Objects.equals(parentHash, that.parentHash)
        && Objects.equals(feeRecipient, that.feeRecipient)
        && Objects.equals(stateRoot, that.stateRoot)
        && Objects.equals(receiptsRoot, that.receiptsRoot)
        && Objects.equals(logsBloom, that.logsBloom)
        && Objects.equals(prevRandao, that.prevRandao)
        && Objects.equals(blockNumber, that.blockNumber)
        && Objects.equals(gasLimit, that.gasLimit)
        && Objects.equals(gasUsed, that.gasUsed)
        && Objects.equals(timestamp, that.timestamp)
        && Objects.equals(extraData, that.extraData)
        && Objects.equals(baseFeePerGas, that.baseFeePerGas)
        && Objects.equals(blockHash, that.blockHash);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
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
        .toString();
  }
}
