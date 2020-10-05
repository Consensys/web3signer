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
package tech.pegasys.web3signer.slashingprotection.interchange.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SignedAttestation {

  @JsonProperty("source_epoch")
  private final String sourceEpoch;

  @JsonProperty("target_epoch")
  private final String targetEpoch;

  @JsonProperty("signing_root")
  public String signingRoot;

  public SignedAttestation(
      final String sourceEpoch, final String targetEpoch, final String signingRoot) {
    this.sourceEpoch = sourceEpoch;
    this.targetEpoch = targetEpoch;
    this.signingRoot = signingRoot;
  }

  public String getSourceEpoch() {
    return sourceEpoch;
  }

  public String getTargetEpoch() {
    return targetEpoch;
  }

  public String getSigningRoot() {
    return signingRoot;
  }
}
