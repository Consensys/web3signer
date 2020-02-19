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
package tech.pegasys.eth2signer.core.signers.hashicorp;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import io.vertx.core.json.JsonObject;

class HashicorpKVResponseMapper {
  protected static final String ERROR_INVALID_JSON =
      "Invalid response returned from Hashicorp Vault";

  /**
   * Convert Hashicorp KV Version 2 Secret Engine JSON response to map of key/values.
   *
   * @param jsonResponse response from Hashicorp Vault wrapped in io.vertx.core.json.JsonObject
   * @return Map of all key/value pairs returned from particular secret
   */
  static Map<String, String> extractKeyValues(final JsonObject jsonResponse) {
    final JsonObject outerDataJsonObject = getJsonObject(jsonResponse);
    final JsonObject dataJsonObject = getJsonObject(outerDataJsonObject);

    // second level data JSON structure contains multiple key/value pairs.
    return Collections.unmodifiableMap(
        dataJsonObject.stream()
            .filter(entry -> Objects.nonNull(entry.getValue()))
            .collect(
                Collectors.toMap(Map.Entry::getKey, HashicorpKVResponseMapper::mapValueToString)));
  }

  private static JsonObject getJsonObject(final JsonObject jsonResponse) {
    final Object dataObject = jsonResponse.getValue("data");
    if (!(dataObject instanceof JsonObject)) {
      throw new RuntimeException(ERROR_INVALID_JSON);
    }
    return (JsonObject) dataObject;
  }

  private static String mapValueToString(final Map.Entry<String, Object> v) {
    return v.getValue().toString();
  }
}
