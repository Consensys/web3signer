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
package tech.pegasys.web3signer.slashingprotection.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AttestionTestModel {

  private final String publickKey;
  private final int sourceEpoch;
  private final int targetEpoch;
  private final boolean shouldSucceed;
  private final String signingRoot;

  public AttestionTestModel(
      @JsonProperty(value = "pubkey", required = true) final String publickKey,
      @JsonProperty(value = "source_epoch", required = true) int sourceEpoch,
      @JsonProperty(value = "target_epoch", required = true) int targetEpoch,
      @JsonProperty(value = "should_succeed", required = true) boolean shouldSucceed,
      @JsonProperty(value = "signing_root", required = true) String signingRoot) {
    this.publickKey = publickKey;
    this.sourceEpoch = sourceEpoch;
    this.targetEpoch = targetEpoch;
    this.shouldSucceed = shouldSucceed;
    this.signingRoot = signingRoot;
  }

  public String getPublickKey() {
    return publickKey;
  }

  public int getSourceEpoch() {
    return sourceEpoch;
  }

  public int getTargetEpoch() {
    return targetEpoch;
  }

  public boolean isShouldSucceed() {
    return shouldSucceed;
  }
}
