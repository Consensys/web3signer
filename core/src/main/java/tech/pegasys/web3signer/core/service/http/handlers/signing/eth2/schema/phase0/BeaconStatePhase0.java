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

import tech.pegasys.teku.infrastructure.ssz.collections.SszBitvector;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.SpecVersion;
import tech.pegasys.teku.spec.datastructures.state.PendingAttestation.PendingAttestationSchema;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.MutableBeaconState;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.BeaconBlockHeader;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.BeaconState;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.Checkpoint;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.Eth1Data;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.Fork;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.PendingAttestation;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.Validator;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.interfaces.State;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.tuweni.bytes.Bytes32;

@SuppressWarnings("JavaCase")
public class BeaconStatePhase0 extends BeaconState implements State {
  public final List<PendingAttestation> previous_epoch_attestations;
  public final List<PendingAttestation> current_epoch_attestations;

  @JsonCreator
  public BeaconStatePhase0(
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
      @JsonProperty("previous_epoch_attestations")
          final List<PendingAttestation> previous_epoch_attestations,
      @JsonProperty("current_epoch_attestations")
          final List<PendingAttestation> current_epoch_attestations,
      @JsonProperty("justification_bits") final SszBitvector justification_bits,
      @JsonProperty("previous_justified_checkpoint") final Checkpoint previous_justified_checkpoint,
      @JsonProperty("current_justified_checkpoint") final Checkpoint current_justified_checkpoint,
      @JsonProperty("finalized_checkpoint") final Checkpoint finalized_checkpoint) {
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
    this.previous_epoch_attestations = previous_epoch_attestations;
    this.current_epoch_attestations = current_epoch_attestations;
  }

  public BeaconStatePhase0(
      final tech.pegasys.teku.spec.datastructures.state.beaconstate.BeaconState beaconState) {
    super(beaconState);
    // Optionally set phase0-specific versioned fields
    final Optional<
            tech.pegasys.teku.spec.datastructures.state.beaconstate.versions.phase0
                .BeaconStatePhase0>
        maybePhase0State = beaconState.toVersionPhase0();
    if (maybePhase0State.isPresent()) {
      final tech.pegasys.teku.spec.datastructures.state.beaconstate.versions.phase0
              .BeaconStatePhase0
          genesisState = maybePhase0State.get();
      this.previous_epoch_attestations =
          genesisState.getPreviousEpochAttestations().stream()
              .map(PendingAttestation::new)
              .toList();
      this.current_epoch_attestations =
          genesisState.getCurrentEpochAttestations().stream().map(PendingAttestation::new).toList();
    } else {
      this.previous_epoch_attestations = null;
      this.current_epoch_attestations = null;
    }
  }

  @Override
  protected void applyAdditionalFields(
      final MutableBeaconState state, final SpecVersion specVersion) {
    state
        .toMutableVersionPhase0()
        .ifPresent(
            mutableState -> {
              final PendingAttestationSchema pendingAttestationSchema =
                  mutableState.getBeaconStateSchema().getPendingAttestationSchema();
              mutableState
                  .getPreviousEpochAttestations()
                  .setAll(
                      previous_epoch_attestations.stream()
                          .map(
                              pendingAttestation ->
                                  pendingAttestation.asInternalPendingAttestation(
                                      pendingAttestationSchema))
                          .toList());
              mutableState
                  .getCurrentEpochAttestations()
                  .setAll(
                      current_epoch_attestations.stream()
                          .map(
                              pendingAttestation ->
                                  pendingAttestation.asInternalPendingAttestation(
                                      pendingAttestationSchema))
                          .toList());
            });
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final BeaconStatePhase0 that = (BeaconStatePhase0) o;
    return Objects.equals(previous_epoch_attestations, that.previous_epoch_attestations)
        && Objects.equals(current_epoch_attestations, that.current_epoch_attestations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(previous_epoch_attestations, current_epoch_attestations);
  }
}
