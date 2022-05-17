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

import tech.pegasys.teku.api.schema.BeaconBlock;
import tech.pegasys.teku.api.schema.BeaconBlockHeader;
import tech.pegasys.teku.spec.SpecMilestone;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.json.BlockRequestDeserializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.annotations.VisibleForTesting;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(using = BlockRequestDeserializer.class)
public class BlockRequest {
  private final SpecMilestone version;
  private final BeaconBlock beaconBlock; // will be used for PHASE0 and ALTAIR spec
  private final BeaconBlockHeader beaconBlockHeader; // will be used for BELLATRIX and later spec

  public BlockRequest(final SpecMilestone version, final BeaconBlock beaconBlock) {
    this.version = version;
    this.beaconBlock = beaconBlock;
    this.beaconBlockHeader = null;
  }

  public BlockRequest(final SpecMilestone version, final BeaconBlockHeader beaconBlockHeader) {
    this.version = version;
    this.beaconBlock = null;
    this.beaconBlockHeader = beaconBlockHeader;
  }

  @VisibleForTesting
  public BlockRequest(final SpecMilestone version) {
    this.version = version;
    this.beaconBlock = null;
    this.beaconBlockHeader = null;
  }

  @JsonProperty("version")
  public SpecMilestone getVersion() {
    return version;
  }

  @JsonProperty("block")
  public BeaconBlock getBeaconBlock() {
    return beaconBlock;
  }

  @JsonProperty("block_header")
  public BeaconBlockHeader getBeaconBlockHeader() {
    return beaconBlockHeader;
  }
}
