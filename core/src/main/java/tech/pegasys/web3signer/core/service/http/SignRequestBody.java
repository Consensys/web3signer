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

public class SignRequestBody {

  final String artifactType;
  final UInt64 sourceEpoch;
  final UInt64 targetEpoch;
  final UInt64 blockSlot;
  final Bytes dataToSign;

  @JsonCreator
  public SignRequestBody(
      @JsonProperty("artifact_type") final String artifactType,
      @JsonProperty("source_epoch") final UInt64 sourceEpoch,
      @JsonProperty("target_epoch") final UInt64 targetEpoch,
      @JsonProperty("block_slot ") final UInt64 blockSlot,
      @JsonProperty("data") final Bytes dataToSign) {
    this.artifactType = artifactType;
    this.sourceEpoch = sourceEpoch;
    this.targetEpoch = targetEpoch;
    this.blockSlot = blockSlot;
    this.dataToSign = dataToSign;
  }

  public String getArtifactType() {
    return artifactType;
  }

  public UInt64 getSourceEpoch() {
    return sourceEpoch;
  }

  public UInt64 getTargetEpoch() {
    return targetEpoch;
  }

  public UInt64 getBlockSlot() {
    return blockSlot;
  }

  public Bytes getDataToSign() {
    return dataToSign;
  }
}
