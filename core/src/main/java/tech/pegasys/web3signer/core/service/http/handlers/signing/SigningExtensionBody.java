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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.BaseEncoding;

public record SigningExtensionBody(
    @JsonProperty(value = "type", required = true) SigningExtensionType type,
    @JsonProperty(value = "platform", required = true) String platform,
    @JsonProperty(value = "timestamp", required = true) String timestamp,
    @JsonProperty(value = "pubkey", required = true) String pubkey) {

  public String signingDataBase64(final ObjectMapper jsonMapper) {
    final String payloadBase64;
    try {
      payloadBase64 = BaseEncoding.base64().encode(jsonMapper.writeValueAsBytes(this));
    } catch (final JsonProcessingException e) {
      throw new IllegalStateException("Unexpected error serializing signing extension body", e);
    }
    return String.format("%s.%s", headerBase64(), payloadBase64);
  }

  private static String headerBase64() {
    return BaseEncoding.base64().encode("{\"alg\": \"BLS\", \"typ\": \"BLS_SIG\"}".getBytes(UTF_8));
  }
}
