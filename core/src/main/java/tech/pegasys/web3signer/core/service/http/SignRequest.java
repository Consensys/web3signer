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

public class SignRequest {

  private final String type;
  private final Bytes signingRoot;
  private UInt64 slot;
  private UInt64 sourceEpoch;
  private UInt64 targetEpoch;

  @JsonCreator
  public SignRequest(
      @JsonProperty(value = "type", required = true) final String type,
      @JsonProperty(value = "signingRoot", required = true) final Bytes signingRoot) {
    this.type = type;
    this.signingRoot = signingRoot;
  }

  public String getType() {
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
