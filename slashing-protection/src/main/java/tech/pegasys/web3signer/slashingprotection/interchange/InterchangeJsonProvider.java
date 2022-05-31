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
package tech.pegasys.web3signer.slashingprotection.interchange;

import static com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS;
import static com.fasterxml.jackson.databind.SerializationFeature.FLUSH_AFTER_WRITE_VALUE;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;

import tech.pegasys.web3signer.common.JacksonSerializers.HexDeserialiser;
import tech.pegasys.web3signer.common.JacksonSerializers.HexSerialiser;
import tech.pegasys.web3signer.common.JacksonSerializers.StringUInt64Deserializer;
import tech.pegasys.web3signer.common.JacksonSerializers.StringUInt64Serialiser;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;

public class InterchangeJsonProvider {
  private final JsonMapper jsonMapper;

  public InterchangeJsonProvider() {
    jsonMapper =
        JsonMapper.builder()
            .configure(FLUSH_AFTER_WRITE_VALUE, true)
            .addModule(buildInterchangeJsonModule())
            .enable(ACCEPT_CASE_INSENSITIVE_ENUMS)
            .enable(INDENT_OUTPUT)
            .build();
  }

  private static Module buildInterchangeJsonModule() {
    final SimpleModule module =
        new SimpleModule("InterchangeJsonModule", new Version(1, 0, 0, null, null, null));
    module.addDeserializer(Bytes.class, new HexDeserialiser());
    module.addSerializer(Bytes.class, new HexSerialiser());
    module.addDeserializer(UInt64.class, new StringUInt64Deserializer());
    module.addSerializer(UInt64.class, new StringUInt64Serialiser());

    return module;
  }

  public JsonMapper getJsonMapper() {
    return jsonMapper;
  }
}
