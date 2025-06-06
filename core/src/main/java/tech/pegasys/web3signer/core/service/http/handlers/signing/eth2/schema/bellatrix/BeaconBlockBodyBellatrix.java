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
package tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.bellatrix;

import static com.google.common.base.Preconditions.checkNotNull;

import tech.pegasys.teku.infrastructure.async.SafeFuture;
import tech.pegasys.teku.spec.SpecVersion;
import tech.pegasys.teku.spec.datastructures.blocks.blockbody.BeaconBlockBody;
import tech.pegasys.teku.spec.datastructures.blocks.blockbody.BeaconBlockBodyBuilder;
import tech.pegasys.teku.spec.datastructures.blocks.blockbody.versions.bellatrix.BeaconBlockBodySchemaBellatrix;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.Attestation;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.AttesterSlashing;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.BLSSignature;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.Deposit;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.Eth1Data;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.ProposerSlashing;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.SignedVoluntaryExit;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.altair.BeaconBlockBodyAltair;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.altair.SyncAggregate;

import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.tuweni.bytes.Bytes32;

public class BeaconBlockBodyBellatrix extends BeaconBlockBodyAltair {
  @JsonProperty("execution_payload")
  public final ExecutionPayloadBellatrix executionPayload;

  @JsonCreator
  public BeaconBlockBodyBellatrix(
      @JsonProperty("randao_reveal") final BLSSignature randaoReveal,
      @JsonProperty("eth1_data") final Eth1Data eth1Data,
      @JsonProperty("graffiti") final Bytes32 graffiti,
      @JsonProperty("proposer_slashings") final List<ProposerSlashing> proposerSlashings,
      @JsonProperty("attester_slashings") final List<AttesterSlashing> attesterSlashings,
      @JsonProperty("attestations") final List<Attestation> attestations,
      @JsonProperty("deposits") final List<Deposit> deposits,
      @JsonProperty("voluntary_exits") final List<SignedVoluntaryExit> voluntaryExits,
      @JsonProperty("sync_aggregate") final SyncAggregate syncAggregate,
      @JsonProperty("execution_payload") final ExecutionPayloadBellatrix executionPayload) {
    super(
        randaoReveal,
        eth1Data,
        graffiti,
        proposerSlashings,
        attesterSlashings,
        attestations,
        deposits,
        voluntaryExits,
        syncAggregate);
    checkNotNull(executionPayload, "Execution Payload is required for bellatrix blocks");
    this.executionPayload = executionPayload;
  }

  public BeaconBlockBodyBellatrix(
      final tech.pegasys.teku.spec.datastructures.blocks.blockbody.versions.bellatrix
              .BeaconBlockBodyBellatrix
          message) {
    super(message);
    checkNotNull(
        message.getExecutionPayload(), "Execution Payload is required for bellatrix blocks");
    this.executionPayload = new ExecutionPayloadBellatrix(message.getExecutionPayload());
  }

  @Override
  public BeaconBlockBodySchemaBellatrix<?> getBeaconBlockBodySchema(final SpecVersion spec) {
    return (BeaconBlockBodySchemaBellatrix<?>)
        spec.getSchemaDefinitions().getBeaconBlockBodySchema();
  }

  @Override
  public BeaconBlockBody asInternalBeaconBlockBody(
      final SpecVersion spec, final Function<BeaconBlockBodyBuilder, SafeFuture<Void>> builderRef) {
    return super.asInternalBeaconBlockBody(
        spec,
        builder ->
            SafeFuture.allOf(
                builderRef.apply(builder),
                SafeFuture.of(
                    () ->
                        builder.executionPayload(
                            executionPayload.asInternalExecutionPayload(spec)))));
  }
}
