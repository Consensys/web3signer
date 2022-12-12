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
  private final ForkInfo forkInfo;
  private final BeaconBlock beaconBlock; // phase 0
  private final BlockRequest blockRequest; // altair and onward
  private final AttestationData attestation;
  private final AggregationSlot aggregationSlot;
  private final AggregateAndProof aggregateAndProof;
  private final VoluntaryExit voluntaryExit;
  private final RandaoReveal randaoReveal;
  private final DepositMessage deposit;
  private final SyncCommitteeMessage syncCommitteeMessage;
  private final SyncAggregatorSelectionData syncAggregatorSelectionData;
  private final ContributionAndProof contributionAndProof;
  private final ValidatorRegistration validatorRegistration;

  @JsonCreator
  public Eth2SigningRequestBody(
      @JsonProperty(value = "type", required = true) final ArtifactType type,
      @JsonProperty("signingRoot") final Bytes signingRoot,
      @JsonProperty("fork_info") final ForkInfo forkInfo,
      @JsonProperty("block") final BeaconBlock block,
      @JsonProperty("beacon_block") final BlockRequest blockRequest,
      @JsonProperty("attestation") final AttestationData attestation,
      @JsonProperty("aggregation_slot") final AggregationSlot aggregationSlot,
      @JsonProperty("aggregate_and_proof") final AggregateAndProof aggregateAndProof,
      @JsonProperty("voluntary_exit") final VoluntaryExit voluntaryExit,
      @JsonProperty("randao_reveal") final RandaoReveal randaoReveal,
      @JsonProperty("deposit") final DepositMessage deposit,
      @JsonProperty("sync_committee_message") final SyncCommitteeMessage syncCommitteeMessage,
      @JsonProperty("sync_aggregator_selection_data")
          final SyncAggregatorSelectionData syncAggregatorSelectionData,
      @JsonProperty("contribution_and_proof") final ContributionAndProof contributionAndProof,
      @JsonProperty("validator_registration") final ValidatorRegistration validatorRegistration) {
    this.type = type;
    this.signingRoot = signingRoot;
    this.forkInfo = forkInfo;
    this.beaconBlock = block;
    this.blockRequest = blockRequest;
    this.attestation = attestation;
    this.aggregationSlot = aggregationSlot;
    this.aggregateAndProof = aggregateAndProof;
    this.voluntaryExit = voluntaryExit;
    this.randaoReveal = randaoReveal;
    this.deposit = deposit;
    this.syncCommitteeMessage = syncCommitteeMessage;
    this.syncAggregatorSelectionData = syncAggregatorSelectionData;
    this.contributionAndProof = contributionAndProof;
    this.validatorRegistration = validatorRegistration;
  }

  @JsonProperty("type")
  public ArtifactType getType() {
    return type;
  }

  @JsonProperty("fork_info")
  public ForkInfo getForkInfo() {
    return forkInfo;
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
    return aggregationSlot;
  }

  @JsonProperty("aggregate_and_proof")
  public AggregateAndProof getAggregateAndProof() {
    return aggregateAndProof;
  }

  @JsonProperty("voluntary_exit")
  public VoluntaryExit getVoluntaryExit() {
    return voluntaryExit;
  }

  @JsonProperty("randao_reveal")
  public RandaoReveal getRandaoReveal() {
    return randaoReveal;
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

  @JsonProperty("validator_registration")
  public ValidatorRegistration getValidatorRegistration() {
    return validatorRegistration;
  }
}
