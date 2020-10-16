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
import tech.pegasys.web3signer.core.service.http.ArtifactType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.tuweni.bytes.Bytes;

public class Eth2SigningRequestBody {
  private final ArtifactType type;
  private final Bytes signingRoot;
  private final ForkInfo forkInfo;
  private final BeaconBlock beaconBlock;
  private final AttestationData attestation;
  private final AggregationSlot aggregationSlot;
  private final AggregateAndProof aggregateAndProof;
  private final VoluntaryExit voluntaryExit;
  private final RandaoReveal randaoReveal;
  private final DepositMessage deposit;

  @JsonCreator
  public Eth2SigningRequestBody(
      @JsonProperty(value = "type", required = true) final ArtifactType type,
      @JsonProperty(value = "signingRoot") final Bytes signingRoot,
      @JsonProperty(value = "forkInfo") final ForkInfo forkInfo,
      @JsonProperty(value = "block") final BeaconBlock block,
      @JsonProperty(value = "attestation") final AttestationData attestation,
      @JsonProperty(value = "aggregationSlot") final AggregationSlot aggregationSlot,
      @JsonProperty(value = "aggregateAndProof") final AggregateAndProof aggregateAndProof,
      @JsonProperty(value = "voluntaryExit") final VoluntaryExit voluntaryExit,
      @JsonProperty(value = "randaoReveal") final RandaoReveal randaoReveal,
      @JsonProperty(value = "deposit") final DepositMessage deposit) {
    this.type = type;
    this.signingRoot = signingRoot;
    this.forkInfo = forkInfo;
    this.beaconBlock = block;
    this.attestation = attestation;
    this.aggregationSlot = aggregationSlot;
    this.aggregateAndProof = aggregateAndProof;
    this.voluntaryExit = voluntaryExit;
    this.randaoReveal = randaoReveal;
    this.deposit = deposit;
  }

  public ArtifactType getType() {
    return type;
  }

  public ForkInfo getForkInfo() {
    return forkInfo;
  }

  public BeaconBlock getBlock() {
    return beaconBlock;
  }

  public AttestationData getAttestation() {
    return attestation;
  }

  public Bytes getSigningRoot() {
    return signingRoot;
  }

  public BeaconBlock getBeaconBlock() {
    return beaconBlock;
  }

  public AggregationSlot getAggregationSlot() {
    return aggregationSlot;
  }

  public AggregateAndProof getAggregateAndProof() {
    return aggregateAndProof;
  }

  public VoluntaryExit getVoluntaryExit() {
    return voluntaryExit;
  }

  public RandaoReveal getRandaoReveal() {
    return randaoReveal;
  }

  public DepositMessage getDeposit() {
    return deposit;
  }
}
