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

import tech.pegasys.teku.infrastructure.ssz.collections.SszBitvector;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.datastructures.operations.versions.altair.SyncCommitteeContributionSchema;
import tech.pegasys.teku.spec.schemas.SchemaDefinitionsAltair;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.BLSSignature;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public class SyncCommitteeContribution {
  @JsonProperty("slot")
  public final UInt64 slot;

  @JsonProperty("beacon_block_root")
  public final Bytes32 beaconBlockRoot;

  @JsonProperty("subcommittee_index")
  public final UInt64 subcommitteeIndex;

  @JsonProperty("aggregation_bits")
  public final Bytes aggregationBits;

  @JsonProperty("signature")
  public final BLSSignature signature;

  @JsonCreator
  public SyncCommitteeContribution(
      @JsonProperty("slot") final UInt64 slot,
      @JsonProperty("beacon_block_root") final Bytes32 beaconBlockRoot,
      @JsonProperty("subcommittee_index") final UInt64 subcommitteeIndex,
      @JsonProperty("aggregation_bits") final Bytes aggregationBits,
      @JsonProperty("signature") final BLSSignature signature) {
    this.slot = slot;
    this.beaconBlockRoot = beaconBlockRoot;
    this.subcommitteeIndex = subcommitteeIndex;
    this.aggregationBits = aggregationBits;
    this.signature = signature;
  }

  public SyncCommitteeContribution(
      final tech.pegasys.teku.spec.datastructures.operations.versions.altair
              .SyncCommitteeContribution
          contribution) {
    this(
        contribution.getSlot(),
        contribution.getBeaconBlockRoot(),
        contribution.getSubcommitteeIndex(),
        contribution.getAggregationBits().sszSerialize(),
        new BLSSignature(contribution.getSignature()));
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final SyncCommitteeContribution that = (SyncCommitteeContribution) o;
    return Objects.equals(slot, that.slot)
        && Objects.equals(beaconBlockRoot, that.beaconBlockRoot)
        && Objects.equals(subcommitteeIndex, that.subcommitteeIndex)
        && Objects.equals(aggregationBits, that.aggregationBits)
        && Objects.equals(signature, that.signature);
  }

  @Override
  public int hashCode() {
    return Objects.hash(slot, beaconBlockRoot, subcommitteeIndex, aggregationBits, signature);
  }

  public static tech.pegasys.teku.spec.datastructures.operations.versions.altair
          .SyncCommitteeContribution
      asInternalSyncCommitteeContribution(
          final Spec spec, final SyncCommitteeContribution syncCommitteeContribution) {
    final SchemaDefinitionsAltair altairDefinitions =
        SchemaDefinitionsAltair.required(
            spec.atSlot(syncCommitteeContribution.slot).getSchemaDefinitions());
    final SyncCommitteeContributionSchema syncCommitteeContributionSchema =
        altairDefinitions.getSyncCommitteeContributionSchema();

    final SszBitvector aggregationBitsVector =
        syncCommitteeContributionSchema
            .getAggregationBitsSchema()
            .fromBytes(syncCommitteeContribution.aggregationBits);
    return spec.getSyncCommitteeUtilRequired(syncCommitteeContribution.slot)
        .createSyncCommitteeContribution(
            syncCommitteeContribution.slot,
            syncCommitteeContribution.beaconBlockRoot,
            syncCommitteeContribution.subcommitteeIndex,
            aggregationBitsVector.getAllSetBits(),
            syncCommitteeContribution.signature.asInternalBLSSignature());
  }
}
