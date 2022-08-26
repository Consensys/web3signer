/*
 * Copyright 2021 ConsenSys AG.
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

import tech.pegasys.teku.infrastructure.unsigned.UInt64;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.tuweni.bytes.Bytes32;

public class SyncCommitteeMessage {
  private final Bytes32 beaconBlockRoot;
  private final UInt64 slot;

  @JsonCreator
  public SyncCommitteeMessage(
      @JsonProperty(value = "beacon_block_root", required = true) final Bytes32 beaconBlockRoot,
      @JsonProperty(value = "slot", required = true) final UInt64 slot) {
    this.beaconBlockRoot = beaconBlockRoot;
    this.slot = slot;
  }

  @JsonProperty(value = "beacon_block_root")
  public Bytes32 getBeaconBlockRoot() {
    return beaconBlockRoot;
  }

  @JsonProperty(value = "slot")
  public UInt64 getSlot() {
    return slot;
  }
}
