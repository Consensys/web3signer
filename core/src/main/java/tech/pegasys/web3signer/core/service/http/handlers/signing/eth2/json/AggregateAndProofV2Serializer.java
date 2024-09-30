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

import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.AggregateAndProofV2;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Custom serializer for the AggregateAndProofV2 class. This serializer handles both the new format
 * (with "version" and "data" fields) and the legacy format (without the "version" field) of
 * AggregateAndProofV2 JSON representations.
 *
 * <p>Serialization behavior:
 *
 * <ul>
 *   <li>If the version is not null: Serializes with explicit "version" and "data" fields.
 *   <li>If the version is null: Serializes all fields of the data object at the top level.
 * </ul>
 *
 * <p>This serializer also performs a null check on the data field, throwing a JsonMappingException
 * if the data is null.
 */
public class AggregateAndProofV2Serializer extends StdSerializer<AggregateAndProofV2> {

  public AggregateAndProofV2Serializer() {
    this(null);
  }

  public AggregateAndProofV2Serializer(final Class<AggregateAndProofV2> t) {
    super(t);
  }

  @Override
  public void serialize(
      final AggregateAndProofV2 value, final JsonGenerator gen, final SerializerProvider provider)
      throws IOException {
    if (value.data() == null) {
      throw new JsonMappingException(
          gen, "Cannot serialize AggregateAndProofV2: data field is null");
    }

    if (value.version() == null) {
      // Serialize data fields as top level
      provider.defaultSerializeValue(value.data(), gen);
    } else {
      // Serialize with version and data fields
      gen.writeStartObject();
      gen.writeObjectField("version", value.version());
      gen.writeObjectField("data", value.data());
      gen.writeEndObject();
    }
  }
}
