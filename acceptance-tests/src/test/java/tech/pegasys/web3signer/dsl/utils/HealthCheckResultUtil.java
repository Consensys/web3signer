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
  public static int getHealthCheckKeysCheckData(
      String healthCheckJsonBody, final String healthCheckKeyName, final String dataKey)
      throws JsonProcessingException {
    final List<String> keyCheckIds = Splitter.on('/').splitToList(healthCheckKeyName);
    int dataKeyValue = -1;
    final JsonNode jsonNode = OBJECT_MAPPER.readTree(healthCheckJsonBody);
    for (JsonNode checksNode : jsonNode.get("checks")) {
      // id = keys-check
      if (checksNode.get("id").asText().equals(keyCheckIds.get(0))) {
        for (JsonNode keyChecksNode : checksNode.get("checks")) {
          // id = See HealthCheckNames.java
          if (keyChecksNode.get("id").asText().equals(keyCheckIds.get(1))) {
            dataKeyValue = keyChecksNode.get("data").get(dataKey).asInt();
            break;
          }
        }
      }
    }

    return dataKeyValue;
  }
}
