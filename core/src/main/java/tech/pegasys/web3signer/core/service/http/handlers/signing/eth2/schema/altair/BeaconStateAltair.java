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

import tech.pegasys.teku.infrastructure.ssz.SszList;
import tech.pegasys.teku.infrastructure.ssz.collections.SszBitvector;
import tech.pegasys.teku.infrastructure.ssz.primitive.SszByte;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.SpecVersion;
import tech.pegasys.teku.spec.datastructures.state.SyncCommittee.SyncCommitteeSchema;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.MutableBeaconState;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.versions.altair.BeaconStateSchemaAltair;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.versions.altair.MutableBeaconStateAltair;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.BeaconBlockHeader;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.BeaconState;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.Checkpoint;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.Eth1Data;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.Fork;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.Validator;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.interfaces.State;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

@SuppressWarnings("JavaCase")
public class BeaconStateAltair extends BeaconState implements State {
  public final byte[] previous_epoch_participation;

  public final byte[] current_epoch_participation;

  @JsonProperty("inactivity_scores")
  public final List<UInt64> inactivity_scores;

  public final SyncCommittee current_sync_committee;
  public final SyncCommittee next_sync_committee;

  @JsonCreator
  public BeaconStateAltair(
      @JsonProperty("genesis_time") final UInt64 genesis_time,
      @JsonProperty("genesis_validators_root") final Bytes32 genesis_validators_root,
      @JsonProperty("slot") final UInt64 slot,
      @JsonProperty("fork") final Fork fork,
      @JsonProperty("latest_block_header") final BeaconBlockHeader latest_block_header,
      @JsonProperty("block_roots") final List<Bytes32> block_roots,
      @JsonProperty("state_roots") final List<Bytes32> state_roots,
      @JsonProperty("historical_roots") final List<Bytes32> historical_roots,
      @JsonProperty("eth1_data") final Eth1Data eth1_data,
      @JsonProperty("eth1_data_votes") final List<Eth1Data> eth1_data_votes,
      @JsonProperty("eth1_deposit_index") final UInt64 eth1_deposit_index,
      @JsonProperty("validators") final List<Validator> validators,
      @JsonProperty("balances") final List<UInt64> balances,
      @JsonProperty("randao_mixes") final List<Bytes32> randao_mixes,
      @JsonProperty("slashings") final List<UInt64> slashings,
      @JsonProperty("previous_epoch_participation") final byte[] previous_epoch_participation,
      @JsonProperty("current_epoch_participation") final byte[] current_epoch_participation,
      @JsonProperty("justification_bits") final SszBitvector justification_bits,
      @JsonProperty("previous_justified_checkpoint") final Checkpoint previous_justified_checkpoint,
      @JsonProperty("current_justified_checkpoint") final Checkpoint current_justified_checkpoint,
      @JsonProperty("finalized_checkpoint") final Checkpoint finalized_checkpoint,
      @JsonProperty("inactivity_scores") final List<UInt64> inactivity_scores,
      @JsonProperty("current_sync_committee") final SyncCommittee current_sync_committee,
      @JsonProperty("next_sync_committee") final SyncCommittee next_sync_committee) {
    super(
        genesis_time,
        genesis_validators_root,
        slot,
        fork,
        latest_block_header,
        block_roots,
        state_roots,
        historical_roots,
        eth1_data,
        eth1_data_votes,
        eth1_deposit_index,
        validators,
        balances,
        randao_mixes,
        slashings,
        justification_bits,
        previous_justified_checkpoint,
        current_justified_checkpoint,
        finalized_checkpoint);
    this.previous_epoch_participation = previous_epoch_participation;
    this.current_epoch_participation = current_epoch_participation;
    this.inactivity_scores = inactivity_scores;
    this.current_sync_committee = current_sync_committee;
    this.next_sync_committee = next_sync_committee;
  }

  public BeaconStateAltair(
      final tech.pegasys.teku.spec.datastructures.state.beaconstate.BeaconState beaconState) {
    super(beaconState);
    final tech.pegasys.teku.spec.datastructures.state.beaconstate.versions.altair.BeaconStateAltair
        altair = beaconState.toVersionAltair().orElseThrow();
    this.previous_epoch_participation = toByteArray(altair.getPreviousEpochParticipation());
    this.current_epoch_participation = toByteArray(altair.getCurrentEpochParticipation());
    this.inactivity_scores = altair.getInactivityScores().asListUnboxed();
    this.current_sync_committee = new SyncCommittee(altair.getCurrentSyncCommittee());
    this.next_sync_committee = new SyncCommittee(altair.getNextSyncCommittee());
  }

  @Override
  protected void applyAdditionalFields(
      final MutableBeaconState state, final SpecVersion specVersion) {
    state
        .toMutableVersionAltair()
        .ifPresent(
            beaconStateAltair -> {
              final SyncCommitteeSchema syncCommitteeSchema =
                  BeaconStateSchemaAltair.required(beaconStateAltair.getBeaconStateSchema())
                      .getCurrentSyncCommitteeSchema();
              applyAltairFields(beaconStateAltair, syncCommitteeSchema, this);
            });
  }

  public static void applyAltairFields(
      final MutableBeaconStateAltair state,
      final SyncCommitteeSchema syncCommitteeSchema,
      final BeaconStateAltair instance) {
    final SszList<SszByte> previousEpochParticipation =
        state
            .getPreviousEpochParticipation()
            .getSchema()
            .sszDeserialize(Bytes.wrap(instance.previous_epoch_participation));
    final SszList<SszByte> currentEpochParticipation =
        state
            .getCurrentEpochParticipation()
            .getSchema()
            .sszDeserialize(Bytes.wrap(instance.current_epoch_participation));

    state.setPreviousEpochParticipation(previousEpochParticipation);
    state.setCurrentEpochParticipation(currentEpochParticipation);
    state.getInactivityScores().setAllElements(instance.inactivity_scores);

    state.setCurrentSyncCommittee(
        instance.current_sync_committee.asInternalSyncCommittee(syncCommitteeSchema));
    state.setNextSyncCommittee(
        instance.next_sync_committee.asInternalSyncCommittee(syncCommitteeSchema));
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final BeaconStateAltair that = (BeaconStateAltair) o;
    return Arrays.equals(previous_epoch_participation, that.previous_epoch_participation)
        && Arrays.equals(current_epoch_participation, that.current_epoch_participation)
        && Objects.equals(inactivity_scores, that.inactivity_scores)
        && Objects.equals(current_sync_committee, that.current_sync_committee)
        && Objects.equals(next_sync_committee, that.next_sync_committee);
  }

  @Override
  public int hashCode() {
    int result =
        Objects.hash(
            System.identityHashCode(this),
            inactivity_scores,
            current_sync_committee,
            next_sync_committee);
    result = 31 * result + Arrays.hashCode(previous_epoch_participation);
    result = 31 * result + Arrays.hashCode(current_epoch_participation);
    return result;
  }

  private byte[] toByteArray(final SszList<SszByte> byteList) {
    final byte[] array = new byte[byteList.size()];
    for (int i = 0; i < array.length; i++) {
      array[i] = byteList.get(i).get();
    }
    return array;
  }
}
