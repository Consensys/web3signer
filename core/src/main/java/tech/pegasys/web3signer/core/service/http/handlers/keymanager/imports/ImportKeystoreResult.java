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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ImportKeystoreResult {
  private ImportKeystoreStatus status;
  private String message;

  @JsonCreator
  public ImportKeystoreResult(
      @JsonProperty("status") final ImportKeystoreStatus status,
      @JsonProperty("message") final String message) {
    this.status = status;
    this.message = message;
  }

  @JsonProperty("status")
  public ImportKeystoreStatus getStatus() {
    return status;
  }

  @JsonProperty("message")
  public String getMessage() {
    return message;
  }

  public void setStatus(final ImportKeystoreStatus status) {
    this.status = status;
  }

  public void setMessage(final String message) {
    this.message = message;
  }
}
