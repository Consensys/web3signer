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
package tech.pegasys.web3signer.core.service.jsonrpc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FilecoinSignedMessage {

  @JsonProperty("Message")
  private final FilecoinMessage message;

  @JsonProperty("Signature")
  private final FilecoinSignature signature;

  @JsonCreator
  public FilecoinSignedMessage(
      final @JsonProperty("Message") FilecoinMessage message,
      final @JsonProperty("Signature") FilecoinSignature signature) {
    this.message = message;
    this.signature = signature;
  }

  public FilecoinMessage getMessage() {
    return message;
  }

  public FilecoinSignature getSignature() {
    return signature;
  }
}
