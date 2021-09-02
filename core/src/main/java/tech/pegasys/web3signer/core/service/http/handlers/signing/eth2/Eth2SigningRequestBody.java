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

import tech.pegasys.teku.api.schema.AggregateAndProof;
import tech.pegasys.teku.api.schema.AttestationData;
import tech.pegasys.teku.api.schema.BeaconBlock;
import tech.pegasys.teku.api.schema.VoluntaryExit;
import tech.pegasys.teku.api.schema.altair.ContributionAndProof;
import tech.pegasys.web3signer.core.service.http.ArtifactType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.tuweni.bytes.Bytes;

public class Eth2SigningRequestBody {
  private final ArtifactType type;
  private final Bytes signingRoot;
  private final ForkInfo fork_info;
  private final BeaconBlock beaconBlock; // phase 0
  private final BlockRequest blockRequest; // altair and onward
  private final AttestationData attestation;
  private final AggregationSlot aggregation_slot;
  private final AggregateAndProof aggregate_and_proof;
  private final VoluntaryExit voluntary_exit;
  private final RandaoReveal randao_reveal;
  private final DepositMessage deposit;
  private final SyncCommitteeMessage syncCommitteeMessage;
  private final SyncAggregatorSelectionData syncAggregatorSelectionData;
  private final ContributionAndProof contributionAndProof;

  @JsonCreator
  public Eth2SigningRequestBody(
      @JsonProperty(value = "type", required = true) final ArtifactType type,
      @JsonProperty("signingRoot") final Bytes signingRoot,
      @JsonProperty("fork_info") final ForkInfo fork_info,
      @JsonProperty("block") final BeaconBlock block,
      @JsonProperty("beacon_block") final BlockRequest blockRequest,
      @JsonProperty("attestation") final AttestationData attestation,
      @JsonProperty("aggregation_slot") final AggregationSlot aggregation_slot,
      @JsonProperty("aggregate_and_proof") final AggregateAndProof aggregate_and_proof,
      @JsonProperty("voluntary_exit") final VoluntaryExit voluntary_exit,
      @JsonProperty("randao_reveal") final RandaoReveal randao_reveal,
      @JsonProperty("deposit") final DepositMessage deposit,
      @JsonProperty("sync_committee_message") final SyncCommitteeMessage syncCommitteeMessage,
      @JsonProperty("sync_aggregator_selection_data")
          final SyncAggregatorSelectionData syncAggregatorSelectionData,
      @JsonProperty("contribution_and_proof") final ContributionAndProof contributionAndProof) {
    this.type = type;
    this.signingRoot = signingRoot;
    this.fork_info = fork_info;
    this.beaconBlock = block;
    this.blockRequest = blockRequest;
    this.attestation = attestation;
    this.aggregation_slot = aggregation_slot;
    this.aggregate_and_proof = aggregate_and_proof;
    this.voluntary_exit = voluntary_exit;
    this.randao_reveal = randao_reveal;
    this.deposit = deposit;
    this.syncCommitteeMessage = syncCommitteeMessage;
    this.syncAggregatorSelectionData = syncAggregatorSelectionData;
    this.contributionAndProof = contributionAndProof;
  }

  @JsonProperty("type")
  public ArtifactType getType() {
    return type;
  }

  @JsonProperty("fork_info")
  public ForkInfo getForkInfo() {
    return fork_info;
  }

  @JsonProperty("block")
  public BeaconBlock getBlock() {
    return beaconBlock;
  }

  @JsonProperty("beacon_block")
  public BlockRequest getBlockRequest() {
    return blockRequest;
  }

  @JsonProperty("attestation")
  public AttestationData getAttestation() {
    return attestation;
  }

  @JsonProperty("signingRoot")
  public Bytes getSigningRoot() {
    return signingRoot;
  }

  @JsonProperty("aggregation_slot")
  public AggregationSlot getAggregationSlot() {
    return aggregation_slot;
  }

  @JsonProperty("aggregate_and_proof")
  public AggregateAndProof getAggregateAndProof() {
    return aggregate_and_proof;
  }

  @JsonProperty("voluntary_exit")
  public VoluntaryExit getVoluntaryExit() {
    return voluntary_exit;
  }

  @JsonProperty("randao_reveal")
  public RandaoReveal getRandaoReveal() {
    return randao_reveal;
  }

  @JsonProperty("deposit")
  public DepositMessage getDeposit() {
    return deposit;
  }

  @JsonProperty("sync_committee_message")
  public SyncCommitteeMessage getSyncCommitteeMessage() {
    return syncCommitteeMessage;
  }

  @JsonProperty("sync_aggregator_selection_data")
  public SyncAggregatorSelectionData getSyncAggregatorSelectionData() {
    return syncAggregatorSelectionData;
  }

  @JsonProperty("contribution_and_proof")
  public ContributionAndProof getContributionAndProof() {
    return contributionAndProof;
  }
}
