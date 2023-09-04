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
package tech.pegasys.web3signer.dsl.utils;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;

public class HealthCheckResultUtil {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /*
        Healthcheck Json looks like:
        {
          "status": "UP",
          "checks": [
              {
                  "id": "default-check",
                  "status": "UP"
              },
              {
                  "id": "keys-check",
                  "status": "UP",
                  "checks": [
                      {
                          "id": "wallet-bulk-loading",
                          "status": "UP",
                          "data": {
                              "keys-loaded": 4,
                              "error-count": 0
                          }
                      },
                      {
                          "id": "config-files-loading",
                          "status": "UP",
                          "data": {
                              "keys-loaded": 4,
                              "error-count": 0
                          }
                      }
                  ]
              }
          ],
          "outcome": "UP"
          }
  */

  /**
   * Returns value of "status" property
   *
   * @param jsonBody The healthcheck Json body
   * @return value of status property. Can be "UP" or "DOWN".
   */
  public static String getHealthcheckStatusValue(final String jsonBody) {
    try {
      final JsonNode jsonNode = OBJECT_MAPPER.readTree(jsonBody);
      return jsonNode.get("status").asText();
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static int getHealtcheckKeysLoaded(final String jsonBody, final String healthcheckKey) {
    try {
      return getHealthcheckDataPropertyValue(jsonBody, healthcheckKey, "keys-loaded");
    } catch (final JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static int getHealthcheckErrorCount(final String jsonBody, final String healthcheckKey) {
    try {
      return getHealthcheckDataPropertyValue(jsonBody, healthcheckKey, "error-count");
    } catch (final JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Parses Healthcheck JSON body and returns value of "data" properties.
   *
   * @param jsonBody Healthcheck JSON body
   * @param healthCheckKeyName Healthcheck keyname such as "keys-check/config-files-loading".
   * @see tech.pegasys.web3signer.core.config.HealthCheckNames
   * @param dataProperty Data property name such as keys-loaded, error-count.
   * @return The int value associated with data property. -1 if the property doesn't exist.
   * @throws JsonProcessingException In case of invalid Json
   */
  private static int getHealthcheckDataPropertyValue(
      String jsonBody, final String healthCheckKeyName, final String dataProperty)
      throws JsonProcessingException {
    final List<String> keyCheckIds = Splitter.on('/').splitToList(healthCheckKeyName);
    int dataKeyValue = -1;
    final JsonNode jsonNode = OBJECT_MAPPER.readTree(jsonBody);
    for (JsonNode checksNode : jsonNode.get("checks")) {
      // id = keys-check
      if (checksNode.get("id").asText().equals(keyCheckIds.get(0))) {
        for (JsonNode keyChecksNode : checksNode.get("checks")) {
          // id = See HealthCheckNames.java
          if (keyChecksNode.get("id").asText().equals(keyCheckIds.get(1))) {
            dataKeyValue = keyChecksNode.get("data").get(dataProperty).asInt();
            break;
          }
        }
      }
    }

    return dataKeyValue;
  }
}
