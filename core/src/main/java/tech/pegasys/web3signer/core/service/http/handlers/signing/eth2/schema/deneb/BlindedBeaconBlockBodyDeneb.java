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
package tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.deneb;

import static com.google.common.base.Preconditions.checkNotNull;

import tech.pegasys.teku.infrastructure.async.SafeFuture;
import tech.pegasys.teku.infrastructure.ssz.schema.SszListSchema;
import tech.pegasys.teku.spec.SpecVersion;
import tech.pegasys.teku.spec.datastructures.blocks.blockbody.BeaconBlockBody;
import tech.pegasys.teku.spec.datastructures.blocks.blockbody.versions.deneb.BlindedBeaconBlockBodySchemaDeneb;
import tech.pegasys.teku.spec.datastructures.execution.ExecutionPayloadHeaderSchema;
import tech.pegasys.teku.spec.datastructures.type.SszKZGCommitment;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.Attestation;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.AttesterSlashing;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.BLSSignature;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.Deposit;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.Eth1Data;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.KZGCommitment;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.ProposerSlashing;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.SignedVoluntaryExit;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.altair.BeaconBlockBodyAltair;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.altair.SyncAggregate;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.capella.SignedBlsToExecutionChange;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.tuweni.bytes.Bytes32;

public class BlindedBeaconBlockBodyDeneb extends BeaconBlockBodyAltair {

  @JsonProperty("execution_payload_header")
  public final ExecutionPayloadHeaderDeneb executionPayloadHeader;

  @JsonProperty("bls_to_execution_changes")
  public final List<SignedBlsToExecutionChange> blsToExecutionChanges;

  @JsonProperty("blob_kzg_commitments")
  public final List<KZGCommitment> blobKZGCommitments;

  @JsonCreator
  public BlindedBeaconBlockBodyDeneb(
      @JsonProperty("randao_reveal") final BLSSignature randaoReveal,
      @JsonProperty("eth1_data") final Eth1Data eth1Data,
      @JsonProperty("graffiti") final Bytes32 graffiti,
      @JsonProperty("proposer_slashings") final List<ProposerSlashing> proposerSlashings,
      @JsonProperty("attester_slashings") final List<AttesterSlashing> attesterSlashings,
      @JsonProperty("attestations") final List<Attestation> attestations,
      @JsonProperty("deposits") final List<Deposit> deposits,
      @JsonProperty("voluntary_exits") final List<SignedVoluntaryExit> voluntaryExits,
      @JsonProperty("sync_aggregate") final SyncAggregate syncAggregate,
      @JsonProperty("execution_payload_header")
          final ExecutionPayloadHeaderDeneb executionPayloadHeader,
      @JsonProperty("bls_to_execution_changes")
          final List<SignedBlsToExecutionChange> blsToExecutionChanges,
      @JsonProperty("blob_kzg_commitments") final List<KZGCommitment> blobKZGCommitments) {
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
    checkNotNull(
        executionPayloadHeader, "Execution Payload Header is required for Deneb blinded blocks");
    this.executionPayloadHeader = executionPayloadHeader;
    checkNotNull(
        blsToExecutionChanges, "blsToExecutionChanges is required for Deneb blinded blocks");
    this.blsToExecutionChanges = blsToExecutionChanges;
    checkNotNull(blobKZGCommitments, "blobKZGCommitments is required for Deneb blinded blocks");
    this.blobKZGCommitments = blobKZGCommitments;
  }

  public BlindedBeaconBlockBodyDeneb(
      final tech.pegasys.teku.spec.datastructures.blocks.blockbody.versions.deneb
              .BlindedBeaconBlockBodyDeneb
          blockBody) {
    super(blockBody);
    this.executionPayloadHeader =
        new ExecutionPayloadHeaderDeneb(blockBody.getExecutionPayloadHeader());
    this.blsToExecutionChanges =
        blockBody.getBlsToExecutionChanges().stream().map(SignedBlsToExecutionChange::new).toList();
    this.blobKZGCommitments =
        blockBody.getBlobKzgCommitments().stream()
            .map(SszKZGCommitment::getKZGCommitment)
            .map(KZGCommitment::new)
            .toList();
  }

  @Override
  public BlindedBeaconBlockBodySchemaDeneb<?> getBeaconBlockBodySchema(final SpecVersion spec) {
    return (BlindedBeaconBlockBodySchemaDeneb<?>)
        spec.getSchemaDefinitions().getBlindedBeaconBlockBodySchema();
  }

  @Override
  public boolean isBlinded() {
    return true;
  }

  @Override
  public BeaconBlockBody asInternalBeaconBlockBody(final SpecVersion spec) {

    final ExecutionPayloadHeaderSchema<?> executionPayloadHeaderSchema =
        getBeaconBlockBodySchema(spec).getExecutionPayloadHeaderSchema();

    final SszListSchema<
            tech.pegasys.teku.spec.datastructures.operations.SignedBlsToExecutionChange, ?>
        blsToExecutionChangesSchema = getBeaconBlockBodySchema(spec).getBlsToExecutionChanges();

    final SszListSchema<SszKZGCommitment, ?> blobKZGCommitmentsSchema =
        getBeaconBlockBodySchema(spec).getBlobKzgCommitmentsSchema();

    return super.asInternalBeaconBlockBody(
        spec,
        builder -> {
          builder.executionPayloadHeader(
              executionPayloadHeader.asInternalExecutionPayloadHeader(
                  executionPayloadHeaderSchema));
          builder.blsToExecutionChanges(
              this.blsToExecutionChanges.stream()
                  .map(b -> b.asInternalSignedBlsToExecutionChange(spec))
                  .collect(blsToExecutionChangesSchema.collector()));
          builder.blobKzgCommitments(
              this.blobKZGCommitments.stream()
                  .map(KZGCommitment::asInternalKZGCommitment)
                  .map(SszKZGCommitment::new)
                  .collect(blobKZGCommitmentsSchema.collector()));
          return SafeFuture.COMPLETE;
        });
  }
}
