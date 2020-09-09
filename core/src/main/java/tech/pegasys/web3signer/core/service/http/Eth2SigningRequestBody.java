/*
 * Copyright 2020 ConsenSys AG.
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
package tech.pegasys.web3signer.core.service.http;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;

public class Eth2SigningRequestBody {

  private final ArtifactType type;
  private final Bytes signingRoot;
  private final UInt64 slot;
  private final UInt64 sourceEpoch;
  private final UInt64 targetEpoch;

  @JsonCreator
  public Eth2SigningRequestBody(
      @JsonProperty(value = "signingRoot", required = true) final Bytes signingRoot,
      @JsonProperty(value = "type", required = true) final ArtifactType type,
      @JsonProperty(value = "slot") final UInt64 slot,
      @JsonProperty(value = "sourceEpoch") final UInt64 sourceEpoch,
      @JsonProperty(value = "targetEpoch") final UInt64 targetEpoch) {
    this.type = type;
    this.signingRoot = signingRoot;
    this.slot = slot;
    this.sourceEpoch = sourceEpoch;
    this.targetEpoch = targetEpoch;
  }

  public ArtifactType getType() {
    return type;
  }

  public Bytes getSigningRoot() {
    return signingRoot;
  }

  public UInt64 getSlot() {
    return slot;
  }

  public UInt64 getSourceEpoch() {
    return sourceEpoch;
  }

  public UInt64 getTargetEpoch() {
    return targetEpoch;
  }
}
