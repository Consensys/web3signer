/*
 * Copyright 2021 ConsenSys AG.
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
package tech.pegasys.web3signer.core.service.http.handlers.keymanager.list;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class KeystoreInfo {
  private final String validatingPubkey;
  private final String derivationPath;
  private final boolean readOnly;

  @JsonCreator
  public KeystoreInfo(
      @JsonProperty("validating_pubkey") final String validatingPubkey,
      @JsonProperty("derivation_path") final String derivationPath,
      @JsonProperty("readonly") final boolean readOnly) {
    this.validatingPubkey = validatingPubkey;
    this.derivationPath = derivationPath;
    this.readOnly = readOnly;
  }

  @JsonProperty("validating_pubkey")
  public String getValidatingPubkey() {
    return validatingPubkey;
  }

  @JsonProperty("derivation_path")
  public String getDerivationPath() {
    return derivationPath;
  }

  @JsonProperty("readonly")
  public boolean isReadOnly() {
    return readOnly;
  }
}
