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
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;

public class AttestationTestModel {

  private final Bytes publicKey;
  private final UInt64 sourceEpoch;
  private final UInt64 targetEpoch;
  private final boolean shouldSucceed;
  private final Bytes signingRoot;

  public AttestationTestModel(
      @JsonProperty(value = "pubkey", required = true) final Bytes publicKey,
      @JsonProperty(value = "source_epoch", required = true) UInt64 sourceEpoch,
      @JsonProperty(value = "target_epoch", required = true) UInt64 targetEpoch,
      @JsonProperty(value = "should_succeed", required = true) boolean shouldSucceed,
      @JsonProperty(value = "signing_root") Bytes signingRoot) {
    this.publicKey = publicKey;
    this.sourceEpoch = sourceEpoch;
    this.targetEpoch = targetEpoch;
    this.shouldSucceed = shouldSucceed;
    this.signingRoot = signingRoot;
  }

  public Bytes getPublicKey() {
    return publicKey;
  }

  public UInt64 getSourceEpoch() {
    return sourceEpoch;
  }

  public UInt64 getTargetEpoch() {
    return targetEpoch;
  }

  public Bytes getSigningRoot() {
    return signingRoot;
  }

  public boolean isShouldSucceed() {
    return shouldSucceed;
  }
}
