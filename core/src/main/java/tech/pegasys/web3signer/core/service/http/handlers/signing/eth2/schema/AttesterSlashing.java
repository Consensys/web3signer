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

import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.SpecVersion;
import tech.pegasys.teku.spec.datastructures.operations.AttesterSlashingSchema;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("JavaCase")
public class AttesterSlashing {
  public final IndexedAttestation attestation_1;
  public final IndexedAttestation attestation_2;

  public AttesterSlashing(
      final tech.pegasys.teku.spec.datastructures.operations.AttesterSlashing attesterSlashing) {
    this.attestation_1 = new IndexedAttestation(attesterSlashing.getAttestation1());
    this.attestation_2 = new IndexedAttestation(attesterSlashing.getAttestation2());
  }

  @JsonCreator
  public AttesterSlashing(
      @JsonProperty("attestation_1") final IndexedAttestation attestation_1,
      @JsonProperty("attestation_2") final IndexedAttestation attestation_2) {
    this.attestation_1 = attestation_1;
    this.attestation_2 = attestation_2;
  }

  public tech.pegasys.teku.spec.datastructures.operations.AttesterSlashing
      asInternalAttesterSlashing(final Spec spec) {
    return asInternalAttesterSlashing(spec.atSlot(attestation_1.data.slot));
  }

  public tech.pegasys.teku.spec.datastructures.operations.AttesterSlashing
      asInternalAttesterSlashing(final SpecVersion spec) {
    final AttesterSlashingSchema attesterSlashingSchema =
        spec.getSchemaDefinitions().getAttesterSlashingSchema();
    return attesterSlashingSchema.create(
        attestation_1.asInternalIndexedAttestation(spec),
        attestation_2.asInternalIndexedAttestation(spec));
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AttesterSlashing that)) {
      return false;
    }
    return Objects.equals(attestation_1, that.attestation_1)
        && Objects.equals(attestation_2, that.attestation_2);
  }

  @Override
  public int hashCode() {
    return Objects.hash(attestation_1, attestation_2);
  }
}
