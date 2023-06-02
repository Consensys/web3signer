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
package tech.pegasys.web3signer.core.service.http.handlers.keymanager.imports;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ImportKeystoresRequestBody {
  private final List<String> keystores;
  private final List<String> passwords;
  private final String slashingProtection;

  @JsonCreator
  public ImportKeystoresRequestBody(
      @JsonProperty(value = "keystores", required = true) final List<String> keystores,
      @JsonProperty(value = "passwords", required = true) final List<String> passwords,
      @JsonProperty("slashing_protection") final String slashingProtection) {
    this.keystores = keystores;
    this.passwords = passwords;
    this.slashingProtection = slashingProtection;
  }

  @JsonProperty("keystores")
  public List<String> getKeystores() {
    return keystores;
  }

  @JsonProperty("passwords")
  public List<String> getPasswords() {
    return passwords;
  }

  @JsonProperty("slashing_protection")
  public String getSlashingProtection() {
    return slashingProtection;
  }
}
