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

import static com.google.common.base.Preconditions.checkNotNull;

import tech.pegasys.teku.infrastructure.async.SafeFuture;
import tech.pegasys.teku.spec.SpecVersion;
import tech.pegasys.teku.spec.datastructures.blocks.blockbody.BeaconBlockBodyBuilder;
import tech.pegasys.teku.spec.datastructures.blocks.blockbody.versions.altair.BeaconBlockBodySchemaAltair;
import tech.pegasys.teku.spec.datastructures.blocks.blockbody.versions.altair.SyncAggregateSchema;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.Attestation;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.AttesterSlashing;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.BLSSignature;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.BeaconBlockBody;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.Deposit;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.Eth1Data;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.ProposerSlashing;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.SignedVoluntaryExit;

import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.tuweni.bytes.Bytes32;

@SuppressWarnings("JavaCase")
public class BeaconBlockBodyAltair extends BeaconBlockBody {
  @JsonProperty("sync_aggregate")
  public final SyncAggregate syncAggregate;

  @JsonCreator
  public BeaconBlockBodyAltair(
      @JsonProperty("randao_reveal") final BLSSignature randao_reveal,
      @JsonProperty("eth1_data") final Eth1Data eth1_data,
      @JsonProperty("graffiti") final Bytes32 graffiti,
      @JsonProperty("proposer_slashings") final List<ProposerSlashing> proposer_slashings,
      @JsonProperty("attester_slashings") final List<AttesterSlashing> attester_slashings,
      @JsonProperty("attestations") final List<Attestation> attestations,
      @JsonProperty("deposits") final List<Deposit> deposits,
      @JsonProperty("voluntary_exits") final List<SignedVoluntaryExit> voluntary_exits,
      @JsonProperty("sync_aggregate") final SyncAggregate sync_aggregate) {
    super(
        randao_reveal,
        eth1_data,
        graffiti,
        proposer_slashings,
        attester_slashings,
        attestations,
        deposits,
        voluntary_exits);
    checkNotNull(sync_aggregate, "Sync Aggregate is required for altair blocks");
    this.syncAggregate = sync_aggregate;
  }

  public BeaconBlockBodyAltair(
      final tech.pegasys.teku.spec.datastructures.blocks.blockbody.versions.altair
              .BeaconBlockBodyAltair
          message) {
    super(message);
    this.syncAggregate = new SyncAggregate(message.getSyncAggregate());
    checkNotNull(syncAggregate, "Sync Aggregate is required for altair blocks");
  }

  @Override
  public BeaconBlockBodySchemaAltair<?> getBeaconBlockBodySchema(final SpecVersion spec) {
    return (BeaconBlockBodySchemaAltair<?>) spec.getSchemaDefinitions().getBeaconBlockBodySchema();
  }

  @Override
  public tech.pegasys.teku.spec.datastructures.blocks.blockbody.BeaconBlockBody
      asInternalBeaconBlockBody(
          final SpecVersion spec,
          final Function<BeaconBlockBodyBuilder, SafeFuture<Void>> builderRef) {
    final SyncAggregateSchema syncAggregateSchema =
        getBeaconBlockBodySchema(spec).getSyncAggregateSchema();
    return super.asInternalBeaconBlockBody(
        spec,
        builder ->
            SafeFuture.allOf(
                builderRef.apply(builder),
                SafeFuture.of(
                    () ->
                        builder.syncAggregate(
                            syncAggregateSchema.create(
                                syncAggregateSchema
                                    .getSyncCommitteeBitsSchema()
                                    .fromBytes(syncAggregate.syncCommitteeBits)
                                    .getAllSetBits(),
                                syncAggregate.syncCommitteeSignature.asInternalBLSSignature())))));
  }
}
