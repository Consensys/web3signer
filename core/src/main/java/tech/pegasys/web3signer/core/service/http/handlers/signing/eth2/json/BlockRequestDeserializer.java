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

import tech.pegasys.teku.api.exceptions.BadRequestException;
import tech.pegasys.teku.api.schema.BeaconBlock;
import tech.pegasys.teku.api.schema.BeaconBlockHeader;
import tech.pegasys.teku.api.schema.altair.BeaconBlockAltair;
import tech.pegasys.teku.spec.SpecMilestone;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.BlockRequest;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class BlockRequestDeserializer extends JsonDeserializer<BlockRequest> {

  public BlockRequestDeserializer() {}

  @Override
  public BlockRequest deserialize(final JsonParser p, final DeserializationContext ctxt)
      throws IOException {
    final ObjectCodec codec = p.getCodec();
    final JsonNode node = codec.readTree(p);
    final SpecMilestone specMilestone = SpecMilestone.valueOf(node.findValue("version").asText());
    final BeaconBlock beaconBlock;
    final BeaconBlockHeader beaconBlockHeader;
    final BlockRequest blockRequest;
    switch (specMilestone) {
      case PHASE0:
        beaconBlock = codec.treeToValue(node.findValue("block"), BeaconBlock.class);
        if (beaconBlock == null) {
          throw new BadRequestException("No beacon block in request");
        }
        blockRequest = new BlockRequest(specMilestone, beaconBlock);
        break;
      case ALTAIR:
        beaconBlock = codec.treeToValue(node.findValue("block"), BeaconBlockAltair.class);
        if (beaconBlock == null) {
          throw new BadRequestException("No beacon block in request");
        }
        blockRequest = new BlockRequest(specMilestone, beaconBlock);
        break;
      default:
        // for BELLATRIX and onward we only need block_header instead of complete block
        beaconBlockHeader =
            codec.treeToValue(node.findValue("block_header"), BeaconBlockHeader.class);
        if (beaconBlockHeader == null) {
          throw new BadRequestException("No beacon block header in request");
        }
        blockRequest = new BlockRequest(specMilestone, beaconBlockHeader);
        break;
    }
    return blockRequest;
  }
}
