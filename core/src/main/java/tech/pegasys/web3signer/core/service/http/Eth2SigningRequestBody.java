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

import tech.pegasys.teku.api.schema.AttestationData;
import tech.pegasys.teku.api.schema.BeaconBlock;
import tech.pegasys.teku.api.schema.Fork;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public class Eth2SigningRequestBody {

  private final ArtifactType type;
  private final BeaconBlock beaconBlock;
  private final AttestationData attestationData;
  private final Bytes32 genesisValidatorsRoot;
  private final Fork fork;
  private final Bytes signingRoot;

  @JsonCreator
  public Eth2SigningRequestBody(
      @JsonProperty(value = "type", required = true) final ArtifactType type,
      @JsonProperty(value = "genesisValidatorsRoot") final Bytes32 genesisValidatorsRoot,
      @JsonProperty(value = "fork") final Fork fork,
      @JsonProperty(value = "beaconBlock") final BeaconBlock beaconBlock,
      @JsonProperty(value = "attestationData") final AttestationData attestationData,
      @JsonProperty(value = "signingRoot") final Bytes signingRoot) {
    this.type = type;
    this.genesisValidatorsRoot = genesisValidatorsRoot;
    this.fork = fork;
    this.beaconBlock = beaconBlock;
    this.attestationData = attestationData;
    this.signingRoot = signingRoot;
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

  public BeaconBlock getBeaconBlock() {
    return beaconBlock;
  }

  public AttestationData getAttestationData() {
    return attestationData;
  }

  public Bytes getSigningRoot() {
    return signingRoot;
  }
}
