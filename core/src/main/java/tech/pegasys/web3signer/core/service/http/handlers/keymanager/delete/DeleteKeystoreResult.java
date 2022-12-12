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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DeleteKeystoreResult {
  private final DeleteKeystoreStatus status;
  private final String message;

  @JsonCreator
  public DeleteKeystoreResult(
      @JsonProperty("status") final DeleteKeystoreStatus status,
      @JsonProperty("message") final String message) {
    this.status = status;
    this.message = message;
  }

  @JsonProperty("status")
  public DeleteKeystoreStatus getStatus() {
    return status;
  }

  @JsonProperty("message")
  public String getMessage() {
    return message;
  }
}
