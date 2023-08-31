/*
 * Copyright 2023 ConsenSys AG.
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
package tech.pegasys.web3signer.core.service.http.handlers.signing.eth2;

import tech.pegasys.teku.api.schema.KZGCommitment;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.SpecVersion;
import tech.pegasys.teku.spec.datastructures.blobs.versions.deneb.BlindedBlobSidecarSchema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import org.apache.tuweni.bytes.Bytes32;

/**
 * Json representation of BlindedBlobSidecar and BlobSidecar. The blob_root is the "hash tree root"
 * of BlobSidecar's blob
 */
public record BlobSidecar(
    @JsonProperty("block_root") Bytes32 blockRoot,
    @JsonProperty("index") UInt64 index,
    @JsonProperty("slot") UInt64 slot,
    @JsonProperty("block_parent_root") Bytes32 blockParentRoot,
    @JsonProperty("proposer_index") UInt64 proposerIndex,
    @JsonProperty("blob_root") Bytes32 blobRoot,
    @JsonProperty("kzg_commitment") KZGCommitment kzgCommitment,
    @JsonProperty("kzg_proof") KZGProof kzgProof) {

  public tech.pegasys.teku.spec.datastructures.blobs.versions.deneb.BlindedBlobSidecar
      asInternalBlindedBlobSidecar(final SpecVersion spec) {
    final BlindedBlobSidecarSchema blindedBlobSidecarSchema =
        spec.getSchemaDefinitions().toVersionDeneb().orElseThrow().getBlindedBlobSidecarSchema();
    return new tech.pegasys.teku.spec.datastructures.blobs.versions.deneb.BlindedBlobSidecar(
        blindedBlobSidecarSchema,
        blockRoot,
        index,
        slot,
        blockParentRoot,
        proposerIndex,
        blobRoot,
        kzgCommitment.asInternalKZGCommitment(),
        kzgProof.asInternalKZGProof());
  }

  @VisibleForTesting
  public static BlobSidecar fromInternalBlobSidecar(
      final tech.pegasys.teku.spec.datastructures.blobs.versions.deneb.BlobSidecar sidecar) {
    return new BlobSidecar(
        sidecar.getBlockRoot(),
        sidecar.getIndex(),
        sidecar.getSlot(),
        sidecar.getBlockParentRoot(),
        sidecar.getProposerIndex(),
        // convert blob to hash tree root to make it assignable to W3S BlobSidecar
        sidecar.getBlob().hashTreeRoot(),
        new KZGCommitment(sidecar.getKZGCommitment()),
        new KZGProof(sidecar.getKZGProof()));
  }
}
