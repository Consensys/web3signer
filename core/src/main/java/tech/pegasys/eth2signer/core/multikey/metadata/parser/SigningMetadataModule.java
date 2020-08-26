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
package tech.pegasys.eth2signer.core.multikey.metadata.parser;

import org.apache.tuweni.units.bigints.UInt64;
import org.web3j.abi.datatypes.generated.Uint64;
import tech.pegasys.eth2signer.core.multikey.metadata.SigningMetadataException;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.tuweni.bytes.Bytes;
import tech.pegasys.eth2signer.core.service.jsonrpc.CborModule.UInt64Deserializer;
import tech.pegasys.eth2signer.core.service.jsonrpc.CborModule.UInt64Serialiser;

public class SigningMetadataModule extends SimpleModule {

  public SigningMetadataModule() {
    super("SigningMetadata");
    addDeserializer(Bytes.class, new HexStringDeserialiser());
    addSerializer(Bytes.class, new HexStringSerializer());
    addSerializer(UInt64.class, new UInt64Serialiser());
    addDeserializer(UInt64.class, new UInt64Deserializer());
  }

  public static class HexStringDeserialiser extends JsonDeserializer<Bytes> {

    @Override
    public Bytes deserialize(final JsonParser p, final DeserializationContext ctxt) {
      try {
        return Bytes.fromHexString(p.getValueAsString());
      } catch (final Exception e) {
        throw new SigningMetadataException("Invalid hex value for private key", e);
      }
    }
  }

  public static class HexStringSerializer extends JsonSerializer<Bytes> {

    @Override
    public void serialize(
        final Bytes value, final JsonGenerator gen, final SerializerProvider serializers)
        throws IOException {
      gen.writeString(value.toString());
    }
  }
}
