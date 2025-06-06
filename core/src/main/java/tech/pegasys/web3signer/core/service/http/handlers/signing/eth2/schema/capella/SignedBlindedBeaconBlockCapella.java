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

import static com.google.common.base.Preconditions.checkArgument;

import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.BLSSignature;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.SignedBeaconBlock;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.interfaces.SignedBlock;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SignedBlindedBeaconBlockCapella extends SignedBeaconBlock implements SignedBlock {
  private final BlindedBlockCapella message;

  public SignedBlindedBeaconBlockCapella(
      final tech.pegasys.teku.spec.datastructures.blocks.SignedBeaconBlock internalBlock) {
    super(internalBlock);
    checkArgument(
        internalBlock.getMessage().getBody().isBlinded(), "requires a signed blinded beacon block");
    this.message = new BlindedBlockCapella(internalBlock.getMessage());
  }

  @Override
  public BlindedBlockCapella getMessage() {
    return message;
  }

  @JsonCreator
  public SignedBlindedBeaconBlockCapella(
      @JsonProperty("message") final BlindedBlockCapella message,
      @JsonProperty("signature") final BLSSignature signature) {
    super(message, signature);
    this.message = message;
  }
}
