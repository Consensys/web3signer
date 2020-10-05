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
package tech.pegasys.web3signer.slashingprotection.interchange.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SignedArtifacts {

  @JsonProperty("pubKey")
  private final String publicKey;

  @JsonProperty("signed_blocks")
  private final List<SignedBlock> signedBlocks;

  @JsonProperty("signed_attestations")
  private final List<SignedAttestation> signedAttestations;

  public SignedArtifacts(
      final String publicKey,
      final List<SignedBlock> signedBlocks,
      final List<SignedAttestation> signedAttestations) {
    this.publicKey = publicKey;
    this.signedBlocks = signedBlocks;
    this.signedAttestations = signedAttestations;
  }

  public String getPublicKey() {
    return publicKey;
  }

  public List<SignedBlock> getSignedBlocks() {
    return signedBlocks;
  }

  public List<SignedAttestation> getSignedAttestations() {
    return signedAttestations;
  }
}
