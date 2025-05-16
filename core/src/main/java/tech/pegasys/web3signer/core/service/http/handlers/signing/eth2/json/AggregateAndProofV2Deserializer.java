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
package tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.json;

import tech.pegasys.teku.spec.SpecMilestone;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.AggregateAndProofV2;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.AggregateAndProof;

import java.io.IOException;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

/**
 * Custom deserializer for the AggregateAndProofV2 class. This deserializer handles both the new
 * format (with "version" and "data" fields) and the legacy format (without these fields) of
 * AggregateAndProofV2 JSON representations.
 *
 * <p>For the new format, it directly deserializes into AggregateAndProofV2. For the legacy format,
 * it deserializes the entire JSON as AggregateAndProof and wraps it in a new AggregateAndProofV2
 * instance with a null version.
 *
 * <p>This deserializer also performs null and empty checks on the input JSON, throwing an
 * InvalidFormatException if the input is null or empty.
 */
public class AggregateAndProofV2Deserializer extends JsonDeserializer<AggregateAndProofV2> {
  @Override
  public AggregateAndProofV2 deserialize(final JsonParser jp, final DeserializationContext context)
      throws IOException, JacksonException {
    final ObjectMapper mapper = (ObjectMapper) jp.getCodec();
    final JsonNode node = mapper.readTree(jp);

    if (node == null || node.isEmpty()) {
      throw new InvalidFormatException(
          jp, "Empty or null JSON node for AggregateAndProofV2", node, AggregateAndProofV2.class);
    }

    if (node.has("version") && node.has("data")) {
      // New format with version and data
      final SpecMilestone version = mapper.treeToValue(node.get("version"), SpecMilestone.class);
      final AggregateAndProof data = mapper.treeToValue(node.get("data"), AggregateAndProof.class);
      return new AggregateAndProofV2(version, data);
    } else {
      // Legacy format without version and data
      final AggregateAndProof legacyData = mapper.treeToValue(node, AggregateAndProof.class);
      return new AggregateAndProofV2(null, legacyData);
    }
  }
}
