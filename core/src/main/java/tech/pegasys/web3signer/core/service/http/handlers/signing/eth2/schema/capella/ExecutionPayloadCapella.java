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
import tech.pegasys.teku.spec.datastructures.execution.ExecutionPayloadBuilder;
import tech.pegasys.teku.spec.datastructures.execution.ExecutionPayloadSchema;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.ExecutionPayload;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.bellatrix.ExecutionPayloadBellatrix;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;

public class ExecutionPayloadCapella extends ExecutionPayloadBellatrix implements ExecutionPayload {

  @JsonProperty("withdrawals")
  public final List<Withdrawal> withdrawals;

  @JsonCreator
  public ExecutionPayloadCapella(
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
      final @JsonProperty("transactions") List<Bytes> transactions,
      final @JsonProperty("withdrawals") List<Withdrawal> withdrawals) {
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
        transactions);
    this.withdrawals = withdrawals != null ? withdrawals : Collections.emptyList();
  }

  public ExecutionPayloadCapella(
      final tech.pegasys.teku.spec.datastructures.execution.ExecutionPayload executionPayload) {
    super(executionPayload);
    this.withdrawals =
        executionPayload.getOptionalWithdrawals().orElseThrow().stream()
            .map(Withdrawal::new)
            .toList();
  }

  @Override
  protected ExecutionPayloadBuilder applyToBuilder(
      final ExecutionPayloadSchema<?> executionPayloadSchema,
      final ExecutionPayloadBuilder builder) {
    return super.applyToBuilder(executionPayloadSchema, builder)
        .withdrawals(
            () ->
                withdrawals.stream()
                    .map(
                        withdrawal ->
                            withdrawal.asInternalWithdrawal(
                                executionPayloadSchema.getWithdrawalSchemaRequired()))
                    .toList());
  }

  @Override
  public Optional<ExecutionPayloadCapella> toVersionCapella() {
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
    final ExecutionPayloadCapella that = (ExecutionPayloadCapella) o;
    return Objects.equals(withdrawals, that.withdrawals);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), withdrawals);
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
        .add("transactions", transactions)
        .add("withdrawals", withdrawals)
        .toString();
  }
}
