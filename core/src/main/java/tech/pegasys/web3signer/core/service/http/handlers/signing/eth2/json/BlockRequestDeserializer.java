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
package tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.json;

import tech.pegasys.teku.api.schema.BeaconBlock;
import tech.pegasys.teku.api.schema.altair.BeaconBlockAltair;
import tech.pegasys.teku.spec.SpecMilestone;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.BlockRequest;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BlockRequestDeserializer extends JsonDeserializer<BlockRequest> {
  private final ObjectMapper objectMapper;

  public BlockRequestDeserializer(final ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public BlockRequest deserialize(final JsonParser p, final DeserializationContext ctxt)
      throws IOException {
    final JsonNode node = p.getCodec().readTree(p);
    final SpecMilestone specMilestone = SpecMilestone.valueOf(node.findValue("version").asText());
    final BeaconBlock beaconBlock;
    switch (specMilestone) {
      case ALTAIR:
        beaconBlock = objectMapper.treeToValue(node.findValue("block"), BeaconBlockAltair.class);
        break;
      case PHASE0:
        beaconBlock = objectMapper.treeToValue(node.findValue("block"), BeaconBlock.class);
        break;
      default:
        throw new IOException("Unsupported Milestone during deserialization: " + specMilestone);
    }
    return new BlockRequest(specMilestone, beaconBlock);
  }
}
