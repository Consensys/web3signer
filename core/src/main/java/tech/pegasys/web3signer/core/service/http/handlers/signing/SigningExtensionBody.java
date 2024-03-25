/*
 * Copyright 2024 ConsenSys AG.
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
package tech.pegasys.web3signer.core.service.http.handlers.signing;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.tuweni.bytes.Bytes;

public record SigningExtensionBody(
    @JsonProperty(value = "type", required = true) SigningExtensionType type,
    @JsonProperty(value = "platform", required = true) String platform,
    @JsonProperty(value = "timestamp", required = true) String timestamp,
    @JsonProperty(value = "pubkey", required = true) String pubkey) {

  public Bytes signingData() {
    return Bytes.wrap(
        String.format("ext%s%s%s%s", type, platform, timestamp, pubkey)
            .getBytes(StandardCharsets.UTF_8));
  }
}
