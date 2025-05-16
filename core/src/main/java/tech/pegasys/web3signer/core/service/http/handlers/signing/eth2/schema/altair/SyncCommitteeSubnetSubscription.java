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
package tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.altair;

import tech.pegasys.teku.infrastructure.unsigned.UInt64;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SyncCommitteeSubnetSubscription {
  @JsonProperty("validator_index")
  public final UInt64 validatorIndex;

  @JsonProperty("sync_committee_indices")
  public final List<UInt64> syncCommitteeIndices;

  @JsonProperty("until_epoch")
  public final UInt64 untilEpoch;

  @JsonCreator
  public SyncCommitteeSubnetSubscription(
      @JsonProperty("validator_index") final UInt64 validatorIndex,
      @JsonProperty("sync_committee_indices") final List<UInt64> syncCommitteeIndices,
      @JsonProperty("until_epoch") final UInt64 untilEpoch) {
    this.validatorIndex = validatorIndex;
    this.syncCommitteeIndices = syncCommitteeIndices;
    this.untilEpoch = untilEpoch;
  }
}
