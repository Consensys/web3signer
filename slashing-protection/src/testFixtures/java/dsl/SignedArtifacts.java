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
package dsl;

import tech.pegasys.web3signer.slashingprotection.interchange.model.SignedAttestation;
import tech.pegasys.web3signer.slashingprotection.interchange.model.SignedBlock;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SignedArtifacts {

  private final String publicKey;
  private final List<SignedBlock> signedBlocks;
  private final List<SignedAttestation> signedAttestations;

  public SignedArtifacts(
      @JsonProperty(value = "pubkey", required = true) final String publicKey,
      @JsonProperty(value = "signed_blocks", required = true) final List<SignedBlock> signedBlocks,
      @JsonProperty(value = "signed_attestations", required = true)
          final List<SignedAttestation> signedAttestations) {
    this.publicKey = publicKey;
    this.signedBlocks = signedBlocks;
    this.signedAttestations = signedAttestations;
  }

  @JsonGetter(value = "pubkey")
  public String getPublicKey() {
    return publicKey;
  }

  @JsonGetter(value = "signed_blocks")
  public List<SignedBlock> getSignedBlocks() {
    return signedBlocks;
  }

  @JsonGetter(value = "signed_attestations")
  public List<SignedAttestation> getSignedAttestations() {
    return signedAttestations;
  }
}
