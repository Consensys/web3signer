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
package tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema;

import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.SpecVersion;
import tech.pegasys.teku.spec.datastructures.blocks.BeaconBlockSchema;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.interfaces.UnsignedBlindedBlock;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.interfaces.UnsignedBlock;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.tuweni.bytes.Bytes32;

@SuppressWarnings("JavaCase")
public class BeaconBlock implements UnsignedBlock, UnsignedBlindedBlock {
  public final UInt64 slot;

  public final UInt64 proposer_index;

  public final Bytes32 parent_root;

  public final Bytes32 state_root;

  protected final BeaconBlockBody body;

  public BeaconBlockBody getBody() {
    return body;
  }

  protected BeaconBlock(final tech.pegasys.teku.spec.datastructures.blocks.BeaconBlock message) {
    this.slot = message.getSlot();
    this.proposer_index = message.getProposerIndex();
    this.parent_root = message.getParentRoot();
    this.state_root = message.getStateRoot();
    this.body = new BeaconBlockBody(message.getBody());
  }

  @JsonCreator
  public BeaconBlock(
      @JsonProperty("slot") final UInt64 slot,
      @JsonProperty("proposer_index") final UInt64 proposer_index,
      @JsonProperty("parent_root") final Bytes32 parent_root,
      @JsonProperty("state_root") final Bytes32 state_root,
      @JsonProperty("body") final BeaconBlockBody body) {
    this.slot = slot;
    this.proposer_index = proposer_index;
    this.parent_root = parent_root;
    this.state_root = state_root;
    this.body = body;
  }

  public BeaconBlockSchema getBeaconBlockSchema(final SpecVersion spec) {
    return spec.getSchemaDefinitions().getBeaconBlockSchema();
  }

  public tech.pegasys.teku.spec.datastructures.blocks.BeaconBlock asInternalBeaconBlock(
      final Spec spec) {
    final SpecVersion specVersion = spec.atSlot(slot);
    return getBeaconBlockSchema(spec.atSlot(slot))
        .create(
            slot,
            proposer_index,
            parent_root,
            state_root,
            body.asInternalBeaconBlockBody(specVersion));
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BeaconBlock)) {
      return false;
    }
    BeaconBlock that = (BeaconBlock) o;
    return Objects.equals(slot, that.slot)
        && Objects.equals(proposer_index, that.proposer_index)
        && Objects.equals(parent_root, that.parent_root)
        && Objects.equals(state_root, that.state_root)
        && Objects.equals(body, that.body);
  }

  @Override
  public int hashCode() {
    return Objects.hash(slot, proposer_index, parent_root, state_root, body);
  }
}
