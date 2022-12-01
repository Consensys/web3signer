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
package tech.pegasys.web3signer.core.service.http.handlers.keymanager.delete;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DeleteKeystoresResponse {
  private final List<DeleteKeystoreResult> data;
  private final String slashingProtection;

  @JsonCreator
  public DeleteKeystoresResponse(
      @JsonProperty("data") final List<DeleteKeystoreResult> data,
      @JsonProperty("slashing_protection") final String slashingProtection) {
    this.data = data;
    this.slashingProtection = slashingProtection;
  }

  @JsonProperty("data")
  public List<DeleteKeystoreResult> getData() {
    return data;
  }

  @JsonProperty("slashing_protection")
  public String getSlashingProtection() {
    return slashingProtection;
  }
}
