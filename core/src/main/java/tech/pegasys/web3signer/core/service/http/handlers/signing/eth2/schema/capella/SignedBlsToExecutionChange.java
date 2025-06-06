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
package tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.capella;

import tech.pegasys.teku.spec.SpecVersion;
import tech.pegasys.teku.spec.schemas.SchemaDefinitionsCapella;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.BLSSignature;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SignedBlsToExecutionChange {

  @JsonProperty("message")
  public final BlsToExecutionChange message;

  @JsonProperty("signature")
  public final BLSSignature signature;

  @JsonCreator
  public SignedBlsToExecutionChange(
      @JsonProperty("message") final BlsToExecutionChange message,
      @JsonProperty("signature") final BLSSignature signature) {
    this.message = message;
    this.signature = signature;
  }

  public SignedBlsToExecutionChange(
      final tech.pegasys.teku.spec.datastructures.operations.SignedBlsToExecutionChange
          signedBlsToExecutionChanges) {
    this.message = new BlsToExecutionChange(signedBlsToExecutionChanges.getMessage());
    this.signature = new BLSSignature(signedBlsToExecutionChanges.getSignature());
  }

  public tech.pegasys.teku.spec.datastructures.operations.SignedBlsToExecutionChange
      asInternalSignedBlsToExecutionChange(final SpecVersion spec) {
    final Optional<SchemaDefinitionsCapella> schemaDefinitionsCapella =
        spec.getSchemaDefinitions().toVersionCapella();

    if (schemaDefinitionsCapella.isEmpty()) {
      throw new IllegalArgumentException(
          "Could not create BlsToExecutionChange for non-capella spec");
    }

    return schemaDefinitionsCapella
        .get()
        .getSignedBlsToExecutionChangeSchema()
        .create(message.asInternalBlsToExecutionChange(spec), signature.asInternalBLSSignature());
  }
}
