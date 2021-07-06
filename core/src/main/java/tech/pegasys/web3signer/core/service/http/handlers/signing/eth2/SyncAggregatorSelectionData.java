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

public class SyncAggregatorSelectionData {
  private final UInt64 slot;
  private final UInt64 subcommitteeIndex;

  @JsonCreator
  public SyncAggregatorSelectionData(
      @JsonProperty(value = "slot", required = true) final UInt64 slot,
      @JsonProperty(value = "subcommittee_index", required = true) final UInt64 subcommitteeIndex) {
    this.slot = slot;
    this.subcommitteeIndex = subcommitteeIndex;
  }

  @JsonProperty(value = "slot")
  public UInt64 getSlot() {
    return slot;
  }

  @JsonProperty(value = "subcommittee_index")
  public UInt64 getSubcommitteeIndex() {
    return subcommitteeIndex;
  }
}
