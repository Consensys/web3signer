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
package tech.pegasys.web3signer.core.service.http;

import tech.pegasys.teku.api.schema.AggregateAndProof;
import tech.pegasys.teku.api.schema.AttestationData;
import tech.pegasys.teku.api.schema.BeaconBlock;
import tech.pegasys.teku.api.schema.Fork;
import tech.pegasys.teku.api.schema.VoluntaryExit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public class Eth2SigningRequestBody {
  private final ArtifactType type;
  private final Bytes signingRoot;
  private final Bytes32 genesisValidatorsRoot;
  private final Fork fork;
  private final BeaconBlock beaconBlock;
  private final AttestationData attestation;
  private final AggregationSlot aggregationSlot;
  private final AggregateAndProof aggregateAndProof;
  private final VoluntaryExit voluntaryExit;
  private final RandaoReveal randaoReveal;

  @JsonCreator
  public Eth2SigningRequestBody(
      @JsonProperty(value = "type", required = true) final ArtifactType type,
      @JsonProperty(value = "signingRoot") final Bytes signingRoot,
      @JsonProperty(value = "genesisValidatorsRoot") final Bytes32 genesisValidatorsRoot,
      @JsonProperty(value = "fork") final Fork fork,
      @JsonProperty(value = "block") final BeaconBlock block,
      @JsonProperty(value = "attestation") final AttestationData attestation,
      @JsonProperty(value = "aggregationSlot") final AggregationSlot aggregationSlot,
      @JsonProperty(value = "aggregateAndProof") final AggregateAndProof aggregateAndProof,
      @JsonProperty(value = "voluntaryExit") final VoluntaryExit voluntaryExit,
      @JsonProperty(value = "randaoReveal") final RandaoReveal randaoReveal) {
    this.type = type;
    this.signingRoot = signingRoot;
    this.genesisValidatorsRoot = genesisValidatorsRoot;
    this.fork = fork;
    this.beaconBlock = block;
    this.attestation = attestation;
    this.aggregationSlot = aggregationSlot;
    this.aggregateAndProof = aggregateAndProof;
    this.voluntaryExit = voluntaryExit;
    this.randaoReveal = randaoReveal;
  }

  public ArtifactType getType() {
    return type;
  }

  public Bytes32 getGenesisValidatorsRoot() {
    return genesisValidatorsRoot;
  }

  public Fork getFork() {
    return fork;
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
}
