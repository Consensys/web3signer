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

import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.BLSSignature;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ContributionAndProof {
  @JsonProperty("aggregator_index")
  public final UInt64 aggregatorIndex;

  @JsonProperty("selection_proof")
  public final BLSSignature selectionProof;

  @JsonProperty("contribution")
  public final SyncCommitteeContribution contribution;

  @JsonCreator
  public ContributionAndProof(
      @JsonProperty("aggregator_index") final UInt64 aggregatorIndex,
      @JsonProperty("selection_proof") final BLSSignature selectionProof,
      @JsonProperty("contribution") final SyncCommitteeContribution contribution) {
    this.aggregatorIndex = aggregatorIndex;
    this.selectionProof = selectionProof;
    this.contribution = contribution;
  }

  public ContributionAndProof(
      final tech.pegasys.teku.spec.datastructures.operations.versions.altair.ContributionAndProof
          contributionAndProof) {
    this(
        contributionAndProof.getAggregatorIndex(),
        new BLSSignature(contributionAndProof.getSelectionProof()),
        new SyncCommitteeContribution(contributionAndProof.getContribution()));
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final ContributionAndProof that = (ContributionAndProof) o;
    return Objects.equals(aggregatorIndex, that.aggregatorIndex)
        && Objects.equals(selectionProof, that.selectionProof)
        && Objects.equals(contribution, that.contribution);
  }

  @Override
  public int hashCode() {
    return Objects.hash(aggregatorIndex, selectionProof, contribution);
  }
}
