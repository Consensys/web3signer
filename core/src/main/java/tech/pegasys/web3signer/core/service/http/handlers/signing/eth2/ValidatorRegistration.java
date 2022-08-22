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
package tech.pegasys.web3signer.core.service.http.handlers.signing.eth2;

import tech.pegasys.teku.api.schema.BLSPubKey;
import tech.pegasys.teku.infrastructure.bytes.Bytes20;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ValidatorRegistration {
  private final Bytes20 feeRecipient;

  private final UInt64 gasLimit;

  private final UInt64 timestamp;

  private final BLSPubKey pubkey;

  public ValidatorRegistration(
      @JsonProperty(value = "fee_recipient", required = true) final Bytes20 feeRecipient,
      @JsonProperty(value = "gas_limit", required = true) final UInt64 gasLimit,
      @JsonProperty(value = "timestamp", required = true) final UInt64 timestamp,
      @JsonProperty(value = "pubkey", required = true) final BLSPubKey pubkey) {
    this.feeRecipient = feeRecipient;
    this.gasLimit = gasLimit;
    this.timestamp = timestamp;
    this.pubkey = pubkey;
  }

  @JsonProperty("fee_recipient")
  public Bytes20 getFeeRecipient() {
    return feeRecipient;
  }

  @JsonProperty("gas_limit")
  public UInt64 getGasLimit() {
    return gasLimit;
  }

  @JsonProperty("timestamp")
  public UInt64 getTimestamp() {
    return timestamp;
  }

  @JsonProperty("pubkey")
  public BLSPubKey getPubkey() {
    return pubkey;
  }

  public tech.pegasys.teku.spec.datastructures.builder.ValidatorRegistration
      asInternalValidatorRegistration() {
    return new tech.pegasys.teku.spec.datastructures.builder.ValidatorRegistrationSchema()
        .create(feeRecipient, gasLimit, timestamp, pubkey.asBLSPublicKey());
  }
}
