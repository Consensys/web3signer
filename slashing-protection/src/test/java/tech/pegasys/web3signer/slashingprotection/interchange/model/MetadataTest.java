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
package tech.pegasys.web3signer.slashingprotection.interchange.model;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.web3signer.slashingprotection.interchange.InterchangeJsonProvider;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;

class MetadataTest {

  private final ObjectMapper mapper = new InterchangeJsonProvider().getJsonMapper();

  @Test
  @SuppressWarnings("unchecked")
  void metadataHasCorrectlyNamedFields() throws JsonProcessingException {
    final Metadata medataData = new Metadata("5", Bytes.fromHexString("0x123456"));
    final String jsonOutput = mapper.writeValueAsString(medataData);
    final Map<String, String> jsonContent =
        JsonMapper.builder().build().readValue(jsonOutput, Map.class);

    assertThat(jsonContent.get("interchange_format_version"))
        .isEqualTo(medataData.getFormatVersion());
    assertThat(jsonContent.get("genesis_validators_root"))
        .isEqualTo(medataData.getGenesisValidatorsRoot().toHexString());
    assertThat(jsonContent.keySet()).hasSize(2);
    assertThat(jsonContent.keySet())
        .containsOnly("interchange_format_version", "genesis_validators_root");
  }
}
