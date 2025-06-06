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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SignedAggregateAndProof {

  public final AggregateAndProof message;

  public final BLSSignature signature;

  @JsonCreator
  public SignedAggregateAndProof(
      @JsonProperty("message") final AggregateAndProof message,
      @JsonProperty("signature") final BLSSignature signature) {
    this.message = message;
    this.signature = signature;
  }

  public SignedAggregateAndProof(
      final tech.pegasys.teku.spec.datastructures.operations.SignedAggregateAndProof
          signedAggregateAndProof) {
    this.message = new AggregateAndProof(signedAggregateAndProof.getMessage());
    this.signature = new BLSSignature(signedAggregateAndProof.getSignature());
  }

  public tech.pegasys.teku.spec.datastructures.operations.SignedAggregateAndProof
      asInternalSignedAggregateAndProof(final Spec spec) {
    final SpecVersion specVersion = spec.atSlot(message.aggregate.data.slot);
    return specVersion
        .getSchemaDefinitions()
        .getSignedAggregateAndProofSchema()
        .create(
            message.asInternalAggregateAndProof(specVersion), signature.asInternalBLSSignature());
  }
}
