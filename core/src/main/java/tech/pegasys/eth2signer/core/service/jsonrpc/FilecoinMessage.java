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
package tech.pegasys.eth2signer.core.service.jsonrpc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("UnusedVariable")
public class FilecoinMessage {

  @JsonProperty("Version")
  private final Integer version;

  @JsonProperty("To")
  private final String to;

  @JsonProperty("From")
  private final String from;

  @JsonProperty("Nonce")
  private final Integer nonce;

  @JsonProperty("Value")
  private final String value;

  @JsonProperty("GasPrice")
  private final String gasPrice;

  @JsonProperty("GasLimit")
  private final Integer gasLimit;

  @JsonProperty("Method")
  private final Integer method;

  @JsonProperty("Params")
  private final String params;

  @JsonCreator
  public FilecoinMessage(
      final @JsonProperty("Version") Integer version,
      final @JsonProperty("To") String to,
      final @JsonProperty("From") String from,
      final @JsonProperty("Nonce") Integer nonce,
      final @JsonProperty("Value") String value,
      final @JsonProperty("GasPrice") String gasPrice,
      final @JsonProperty("GasLimit") Integer gasLimit,
      final @JsonProperty("Method") Integer method,
      final @JsonProperty("Params") String params) {
    this.version = version;
    this.to = to;
    this.from = from;
    this.nonce = nonce;
    this.value = value;
    this.gasPrice = gasPrice;
    this.gasLimit = gasLimit;
    this.method = method;
    this.params = params;
  }
}
