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

public class BlockTestModel {

  private final Bytes publicKey;
  private final UInt64 slot;
  private final Bytes signingRoot;
  private final boolean shouldSucceed;

  public BlockTestModel(
      @JsonProperty(value = "pubkey", required = true) final Bytes publicKey,
      @JsonProperty(value = "slot", required = true) UInt64 slot,
      @JsonProperty(value = "should_succeed", required = true) boolean shouldSucceed,
      @JsonProperty(value = "signing_root") Bytes signingRoot) {
    this.publicKey = publicKey;
    this.slot = slot;
    this.shouldSucceed = shouldSucceed;
    this.signingRoot = signingRoot;
  }

  public Bytes getPublicKey() {
    return publicKey;
  }

  public UInt64 getSlot() {
    return slot;
  }

  public Bytes getSigningRoot() {
    return signingRoot;
  }

  public boolean isShouldSucceed() {
    return shouldSucceed;
  }
}
