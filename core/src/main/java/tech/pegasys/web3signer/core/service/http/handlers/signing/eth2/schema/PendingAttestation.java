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
import tech.pegasys.teku.spec.datastructures.state.PendingAttestation.PendingAttestationSchema;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.tuweni.bytes.Bytes;

@SuppressWarnings("JavaCase")
public class PendingAttestation {
  public final Bytes aggregation_bits;

  public final AttestationData data;

  public final UInt64 inclusion_delay;

  public final UInt64 proposer_index;

  @JsonCreator
  public PendingAttestation(
      @JsonProperty("aggregation_bits") final Bytes aggregation_bits,
      @JsonProperty("data") final AttestationData data,
      @JsonProperty("inclusion_delay") final UInt64 inclusion_delay,
      @JsonProperty("proposer_index") final UInt64 proposer_index) {
    this.aggregation_bits = aggregation_bits;
    this.data = data;
    this.inclusion_delay = inclusion_delay;
    this.proposer_index = proposer_index;
  }

  public PendingAttestation(
      final tech.pegasys.teku.spec.datastructures.state.PendingAttestation pendingAttestation) {
    this.aggregation_bits = pendingAttestation.getAggregationBits().sszSerialize();
    this.data = new AttestationData(pendingAttestation.getData());
    this.inclusion_delay = pendingAttestation.getInclusionDelay();
    this.proposer_index = pendingAttestation.getProposerIndex();
  }

  public tech.pegasys.teku.spec.datastructures.state.PendingAttestation
      asInternalPendingAttestation(final PendingAttestationSchema pendingAttestationSchema) {
    return pendingAttestationSchema.create(
        pendingAttestationSchema.getAggregationBitfieldSchema().sszDeserialize(aggregation_bits),
        data.asInternalAttestationData(),
        inclusion_delay,
        proposer_index);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PendingAttestation)) {
      return false;
    }
    PendingAttestation that = (PendingAttestation) o;
    return Objects.equals(aggregation_bits, that.aggregation_bits)
        && Objects.equals(data, that.data)
        && Objects.equals(inclusion_delay, that.inclusion_delay)
        && Objects.equals(proposer_index, that.proposer_index);
  }

  @Override
  public int hashCode() {
    return Objects.hash(aggregation_bits, data, inclusion_delay, proposer_index);
  }
}
