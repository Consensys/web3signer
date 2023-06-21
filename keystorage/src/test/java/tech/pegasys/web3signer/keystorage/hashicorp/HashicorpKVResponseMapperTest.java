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

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class HashicorpKVResponseMapperTest {

  private static final String VALID_JSON_RESPONSE =
      "{\n"
          + "  \"request_id\": \"39ae72e7-ef58-ab8b-9aef-4473a4ea2c46\",\n"
          + "  \"lease_id\": \"\",\n"
          + "  \"renewable\": false,\n"
          + "  \"lease_duration\": 0,\n"
          + "  \"data\": {\n"
          + "    \"data\": {\n"
          + "      \"dbKey1\": \"1c2c450cedaa416329fef5900854d55c00046224dffd0075b5057a088b48f9bf\",\n"
          + "      \"value\": \"ccccaaacedaa416329fef5900854d55c00046224dffd0075b5057a088b48f9bf\"\n"
          + "    },\n"
          + "    \"metadata\": {\n"
          + "      \"created_time\": \"2019-11-06T08:21:41.656367376Z\",\n"
          + "      \"deletion_time\": \"\",\n"
          + "      \"destroyed\": false,\n"
          + "      \"version\": 1\n"
          + "    }\n"
          + "  },\n"
          + "  \"wrap_info\": null,\n"
          + "  \"warnings\": null,\n"
          + "  \"auth\": null\n"
          + "}";

  @Test
  void exceptionThrownWhenParsingNullJsonInput() {
    assertThatThrownBy(() -> HashicorpKVResponseMapper.from(null))
        .isInstanceOf(HashicorpException.class)
        .hasMessage(HashicorpKVResponseMapper.ERROR_INVALID_JSON);
  }

  @Test
  void exceptionThrownWhenParsingInvalidJsonInput() {
    assertThatThrownBy(() -> HashicorpKVResponseMapper.from("invalidjson{"))
        .isInstanceOf(HashicorpException.class)
        .hasMessage(HashicorpKVResponseMapper.ERROR_INVALID_JSON);
  }

  @Test
  void exceptionThrownWhenParsingUnexpectedJsonInput() {
    assertThatThrownBy(() -> HashicorpKVResponseMapper.from("{\"test\":\"value\"}"))
        .isInstanceOf(HashicorpException.class)
        .hasMessage(HashicorpKVResponseMapper.ERROR_INVALID_JSON);
  }

  @Test
  void mapReturnedFromValidJsonResponse() {
    final Map<String, String> kvMap = HashicorpKVResponseMapper.from(VALID_JSON_RESPONSE);
    Assertions.assertThat(kvMap).containsOnlyKeys("value", "dbKey1").isNotNull();
  }
}
