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
import tech.pegasys.teku.spec.datastructures.operations.IndexedAttestationSchema;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("JavaCase")
public class IndexedAttestation {
  public final List<UInt64> attesting_indices;

  public final AttestationData data;

  public final BLSSignature signature;

  public IndexedAttestation(
      final tech.pegasys.teku.spec.datastructures.operations.IndexedAttestation
          indexedAttestation) {
    this.attesting_indices = indexedAttestation.getAttestingIndices().streamUnboxed().toList();
    this.data = new AttestationData(indexedAttestation.getData());
    this.signature = new BLSSignature(indexedAttestation.getSignature());
  }

  @JsonCreator
  public IndexedAttestation(
      @JsonProperty("attesting_indices") final List<UInt64> attesting_indices,
      @JsonProperty("data") final AttestationData data,
      @JsonProperty("signature") final BLSSignature signature) {
    this.attesting_indices = attesting_indices;
    this.data = data;
    this.signature = signature;
  }

  public tech.pegasys.teku.spec.datastructures.operations.IndexedAttestation
      asInternalIndexedAttestation(final Spec spec) {
    return asInternalIndexedAttestation(spec.atSlot(data.slot));
  }

  public tech.pegasys.teku.spec.datastructures.operations.IndexedAttestation
      asInternalIndexedAttestation(final SpecVersion spec) {
    final IndexedAttestationSchema indexedAttestationSchema =
        spec.getSchemaDefinitions().getIndexedAttestationSchema();
    return indexedAttestationSchema.create(
        indexedAttestationSchema.getAttestingIndicesSchema().of(attesting_indices),
        data.asInternalAttestationData(),
        signature.asInternalBLSSignature());
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof IndexedAttestation that)) {
      return false;
    }
    return Objects.equals(attesting_indices, that.attesting_indices)
        && Objects.equals(data, that.data)
        && Objects.equals(signature, that.signature);
  }

  @Override
  public int hashCode() {
    return Objects.hash(attesting_indices, data, signature);
  }
}
