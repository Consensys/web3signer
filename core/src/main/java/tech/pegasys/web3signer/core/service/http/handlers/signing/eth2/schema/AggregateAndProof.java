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
package tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema;

import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.SpecVersion;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("JavaCase")
public class AggregateAndProof {

  public final UInt64 aggregator_index;

  public final Attestation aggregate;

  public final BLSSignature selection_proof;

  @JsonCreator
  public AggregateAndProof(
      @JsonProperty("aggregator_index") final UInt64 aggregator_index,
      @JsonProperty("aggregate") final Attestation aggregate,
      @JsonProperty("selection_proof") final BLSSignature selection_proof) {
    this.aggregator_index = aggregator_index;
    this.aggregate = aggregate;
    this.selection_proof = selection_proof;
  }

  public AggregateAndProof(
      final tech.pegasys.teku.spec.datastructures.operations.AggregateAndProof aggregateAndProof) {
    aggregator_index = aggregateAndProof.getIndex();
    aggregate = new Attestation(aggregateAndProof.getAggregate());
    selection_proof = new BLSSignature(aggregateAndProof.getSelectionProof());
  }

  public tech.pegasys.teku.spec.datastructures.operations.AggregateAndProof
      asInternalAggregateAndProof(final Spec spec) {
    return asInternalAggregateAndProof(spec.atSlot(aggregate.data.slot));
  }

  public tech.pegasys.teku.spec.datastructures.operations.AggregateAndProof
      asInternalAggregateAndProof(final SpecVersion specVersion) {
    return specVersion
        .getSchemaDefinitions()
        .getAggregateAndProofSchema()
        .create(
            aggregator_index,
            aggregate.asInternalAttestation(specVersion),
            selection_proof.asInternalBLSSignature());
  }
}
