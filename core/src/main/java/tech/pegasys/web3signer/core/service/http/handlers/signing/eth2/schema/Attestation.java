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

import tech.pegasys.teku.infrastructure.ssz.SszData;
import tech.pegasys.teku.infrastructure.ssz.collections.SszBitvector;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.SpecVersion;
import tech.pegasys.teku.spec.datastructures.operations.AttestationSchema;

import java.util.Objects;
import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.tuweni.bytes.Bytes;

@SuppressWarnings("JavaCase")
public class Attestation {
  public final Bytes aggregation_bits;

  public final AttestationData data;

  @JsonInclude(Include.NON_NULL)
  public final Bytes committee_bits;

  public final BLSSignature signature;

  public Attestation(
      final tech.pegasys.teku.spec.datastructures.operations.Attestation attestation) {
    this.aggregation_bits = attestation.getAggregationBits().sszSerialize();
    this.data = new AttestationData(attestation.getData());
    this.committee_bits = attestation.getCommitteeBits().map(SszData::sszSerialize).orElse(null);
    this.signature = new BLSSignature(attestation.getAggregateSignature());
  }

  @JsonCreator
  public Attestation(
      @JsonProperty("aggregation_bits") final Bytes aggregation_bits,
      @JsonProperty("data") final AttestationData data,
      @JsonProperty("committee_bits") final Bytes committee_bits,
      @JsonProperty("signature") final BLSSignature signature) {
    this.aggregation_bits = aggregation_bits;
    this.data = data;
    this.committee_bits = committee_bits;
    this.signature = signature;
  }

  public tech.pegasys.teku.spec.datastructures.operations.Attestation asInternalAttestation(
      final Spec spec) {
    return asInternalAttestation(spec.atSlot(data.slot));
  }

  public tech.pegasys.teku.spec.datastructures.operations.Attestation asInternalAttestation(
      final SpecVersion specVersion) {
    final AttestationSchema<?> attestationSchema =
        specVersion.getSchemaDefinitions().getAttestationSchema();
    return attestationSchema.create(
        attestationSchema.getAggregationBitsSchema().sszDeserialize(aggregation_bits),
        data.asInternalAttestationData(),
        signature.asInternalBLSSignature(),
        attestationSchema
            .getCommitteeBitsSchema()
            .map(
                committeeBits ->
                    (Supplier<SszBitvector>) () -> committeeBits.sszDeserialize(committee_bits))
            .orElse(() -> null));
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Attestation that)) return false;
    return Objects.equals(aggregation_bits, that.aggregation_bits)
        && Objects.equals(data, that.data)
        && Objects.equals(committee_bits, that.committee_bits)
        && Objects.equals(signature, that.signature);
  }

  @Override
  public int hashCode() {
    return Objects.hash(aggregation_bits, data, committee_bits, signature);
  }
}
