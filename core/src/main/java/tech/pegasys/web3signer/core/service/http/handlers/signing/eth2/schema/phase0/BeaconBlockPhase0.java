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
package tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.phase0;

import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.BeaconBlock;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.BeaconBlockBody;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.interfaces.UnsignedBlock;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.tuweni.bytes.Bytes32;

@SuppressWarnings("JavaCase")
public class BeaconBlockPhase0 extends BeaconBlock implements UnsignedBlock {
  public BeaconBlockPhase0(final tech.pegasys.teku.spec.datastructures.blocks.BeaconBlock message) {
    super(
        message.getSlot(),
        message.getProposerIndex(),
        message.getParentRoot(),
        message.getStateRoot(),
        new BeaconBlockBody(message.getBody()));
  }

  @JsonCreator
  public BeaconBlockPhase0(
      @JsonProperty("slot") final UInt64 slot,
      @JsonProperty("proposer_index") final UInt64 proposer_index,
      @JsonProperty("parent_root") final Bytes32 parent_root,
      @JsonProperty("state_root") final Bytes32 state_root,
      @JsonProperty("body") final BeaconBlockBody body) {
    super(slot, proposer_index, parent_root, state_root, body);
  }
}
