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
package tech.pegasys.web3signer.keystorage.azure;

import java.util.Map;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;

public class AzureKVResponseMapper {
  public static final String ERROR_INVALID_JSON = "Invalid response returned from Azure Vault";

  /**
   * Convert Azure Sign HTTP JSON response to map of key/values
   *
   * @param json response from Azure REST API
   * @return All key/value pairs
   */
  public static Map<String, Object> from(final String json) {
    if (json == null) {
      throw new RuntimeException(ERROR_INVALID_JSON);
    }
    final JsonObject jsonResponse;
    try {
      jsonResponse = new JsonObject(json);
    } catch (final DecodeException de) {
      throw new RuntimeException(ERROR_INVALID_JSON, de);
    }

    return jsonResponse.getMap();
  }
}
