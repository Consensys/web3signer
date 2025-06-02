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

import tech.pegasys.teku.spec.datastructures.execution.ExecutionPayloadHeaderSchema;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.bellatrix.ExecutionPayloadHeaderBellatrix;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.capella.ExecutionPayloadHeaderCapella;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.deneb.ExecutionPayloadHeaderDeneb;

import java.util.Optional;

public interface ExecutionPayloadHeader {
  tech.pegasys.teku.spec.datastructures.execution.ExecutionPayloadHeader
      asInternalExecutionPayloadHeader(ExecutionPayloadHeaderSchema<?> schema);

  default Optional<ExecutionPayloadHeaderBellatrix> toVersionBellatrix() {
    return Optional.empty();
  }

  default Optional<ExecutionPayloadHeaderCapella> toVersionCapella() {
    return Optional.empty();
  }

  default Optional<ExecutionPayloadHeaderDeneb> toVersionDeneb() {
    return Optional.empty();
  }
}
