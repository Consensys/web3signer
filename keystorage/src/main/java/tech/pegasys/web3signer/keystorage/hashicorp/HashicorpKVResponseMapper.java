/*
 * Copyright 2023 ConsenSys AG.
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
package tech.pegasys.web3signer.keystorage.hashicorp;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;

public class HashicorpKVResponseMapper {
  public static final String ERROR_INVALID_JSON = "Invalid response returned from Hashicorp Vault";

  /**
   * Convert Hashicorp KV Version 2 Secret Engine JSON response to map of key/values
   *
   * @param json response from Hashicorp Vault
   * @return All key/value pairs
   */
  public static Map<String, String> from(final String json) {
    if (json == null) {
      throw new HashicorpException(ERROR_INVALID_JSON);
    }
    final JsonObject jsonResponse;
    try {
      jsonResponse = new JsonObject(json);
    } catch (final DecodeException de) {
      throw new HashicorpException(ERROR_INVALID_JSON, de);
    }

    // expecting Hashicorp kv-v2 secret engine compatible JSON json
    final JsonObject keyData =
        Optional.ofNullable(jsonResponse.getJsonObject("data"))
            .map(jo -> jo.getJsonObject("data"))
            .orElseThrow(() -> new HashicorpException(ERROR_INVALID_JSON));
    return Collections.unmodifiableMap(
        keyData.stream()
            .filter(entry -> Objects.nonNull(entry.getValue()))
            .collect(
                Collectors.toMap(Map.Entry::getKey, HashicorpKVResponseMapper::getValueToString)));
  }

  private static String getValueToString(final Map.Entry<String, Object> v) {
    return v.getValue().toString();
  }
}
