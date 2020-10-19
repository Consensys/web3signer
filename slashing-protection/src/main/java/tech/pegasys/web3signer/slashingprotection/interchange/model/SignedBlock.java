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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SignedBlock {

  public String slot;
  public String signingRoot;

  @JsonCreator
  public SignedBlock(
      @JsonProperty(value = "slot", required = true) final String slot,
      @JsonProperty(value = "signing_root") final String signingRoot) {
    this.slot = slot;
    this.signingRoot = signingRoot;
  }

  @JsonGetter(value = "slot")
  public String getSlot() {
    return slot;
  }

  @JsonGetter(value = "signing_root")
  public String getSigningRoot() {
    return signingRoot;
  }
}
