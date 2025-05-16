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

import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.BLSSignature;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SignedContributionAndProof {

  @JsonProperty("message")
  public final ContributionAndProof message;

  @JsonProperty("signature")
  public final BLSSignature signature;

  @JsonCreator
  public SignedContributionAndProof(
      @JsonProperty("message") final ContributionAndProof message,
      @JsonProperty("signature") final BLSSignature signature) {
    this.message = message;
    this.signature = signature;
  }

  public SignedContributionAndProof(
      final tech.pegasys.teku.spec.datastructures.operations.versions.altair
              .SignedContributionAndProof
          proof) {
    this.message = new ContributionAndProof(proof.getMessage());
    this.signature = new BLSSignature(proof.getSignature());
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final SignedContributionAndProof that = (SignedContributionAndProof) o;
    return Objects.equals(message, that.message) && Objects.equals(signature, that.signature);
  }

  @Override
  public int hashCode() {
    return Objects.hash(message, signature);
  }
}
