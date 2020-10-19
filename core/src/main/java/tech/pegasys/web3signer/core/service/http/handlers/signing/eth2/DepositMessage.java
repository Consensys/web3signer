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
import tech.pegasys.teku.infrastructure.unsigned.UInt64;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.tuweni.bytes.Bytes32;

public class DepositMessage {
  public final BLSPubKey pubkey;
  public final Bytes32 withdrawalCredentials;
  public final UInt64 amount;

  public DepositMessage(
      @JsonProperty("pubkey") final BLSPubKey pubkey,
      @JsonProperty("withdrawal_credentials") final Bytes32 withdrawalCredentials,
      @JsonProperty("amount") final UInt64 amount) {
    this.pubkey = pubkey;
    this.withdrawalCredentials = withdrawalCredentials;
    this.amount = amount;
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

  public tech.pegasys.teku.datastructures.operations.DepositMessage asInternalDepositMessage() {
    return new tech.pegasys.teku.datastructures.operations.DepositMessage(
        pubkey.asBLSPublicKey(), withdrawalCredentials, amount);
  }
}
