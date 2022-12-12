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
import tech.pegasys.teku.infrastructure.bytes.Bytes4;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.tuweni.bytes.Bytes32;

public class DepositMessage {
  private final BLSPubKey pubkey;
  private final Bytes32 withdrawalCredentials;
  private final UInt64 amount;
  private final Bytes4 genesisForkVersion;

  public DepositMessage(
      @JsonProperty(value = "pubkey", required = true) final BLSPubKey pubkey,
      @JsonProperty(value = "withdrawal_credentials", required = true)
          final Bytes32 withdrawalCredentials,
      @JsonProperty(value = "amount", required = true) final UInt64 amount,
      @JsonProperty(value = "genesis_fork_version", required = true)
          final Bytes4 genesisForkVersion) {
    this.pubkey = pubkey;
    this.withdrawalCredentials = withdrawalCredentials;
    this.amount = amount;
    this.genesisForkVersion = genesisForkVersion;
  }

  @JsonProperty("pubkey")
  public BLSPubKey getPubkey() {
    return pubkey;
  }

  @JsonProperty("withdrawal_credentials")
  public Bytes32 getWithdrawalCredentials() {
    return withdrawalCredentials;
  }

  @JsonProperty("amount")
  public UInt64 getAmount() {
    return amount;
  }

  @JsonProperty("genesis_fork_version")
  public Bytes4 getGenesisForkVersion() {
    return genesisForkVersion;
  }

  public tech.pegasys.teku.spec.datastructures.operations.DepositMessage
      asInternalDepositMessage() {
    return new tech.pegasys.teku.spec.datastructures.operations.DepositMessage(
        pubkey.asBLSPublicKey(), withdrawalCredentials, amount);
  }
}
