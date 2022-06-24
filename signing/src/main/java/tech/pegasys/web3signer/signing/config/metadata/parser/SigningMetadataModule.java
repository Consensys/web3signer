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
package tech.pegasys.web3signer.signing.config.metadata.parser;

import tech.pegasys.teku.infrastructure.bytes.Bytes20;
import tech.pegasys.web3signer.signing.config.metadata.SigningMetadataException;

import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public class SigningMetadataModule extends SimpleModule {

  public SigningMetadataModule() {
    super("SigningMetadata");

    addDeserializer(Bytes32.class, new Bytes32Deserialiser());
    addSerializer(Bytes32.class, new Bytes32Serializer());

    addDeserializer(Bytes.class, new BytesDeserialiser());
    addSerializer(Bytes.class, new BytesSerializer());

    // serialize Path without uri (Jackson's implementation uses uri)
    addSerializer(Path.class, new ToStringSerializer());
  }

  public static class BytesDeserialiser extends JsonDeserializer<Bytes> {

    @Override
    public Bytes deserialize(final JsonParser p, final DeserializationContext ctxt) {
      try {
        return Bytes.fromHexString(p.getValueAsString());
      } catch (final Exception e) {
        throw new SigningMetadataException("Invalid hex value for private key", e);
      }
    }
  }

  public static class BytesSerializer extends JsonSerializer<Bytes> {

    @Override
    public void serialize(
        final Bytes value, final JsonGenerator gen, final SerializerProvider serializers)
        throws IOException {
      gen.writeString(value.toString());
    }
  }

  public static class Bytes32Deserialiser extends JsonDeserializer<Bytes32> {

    @Override
    public Bytes32 deserialize(final JsonParser p, final DeserializationContext ctxt) {
      try {
        return Bytes32.fromHexString(p.getValueAsString());
      } catch (final Exception e) {
        throw new SigningMetadataException("Invalid hex value for private key", e);
      }
    }
  }

  public static class Bytes32Serializer extends JsonSerializer<Bytes32> {

    @Override
    public void serialize(
        final Bytes32 value, final JsonGenerator gen, final SerializerProvider serializers)
        throws IOException {
      gen.writeString(value.toString());
    }
  }

  public static class Bytes20Deserializer extends JsonDeserializer<Bytes20> {

    @Override
    public Bytes20 deserialize(final JsonParser p, final DeserializationContext ctxt) {
      try {
        return Bytes20.fromHexString(p.getValueAsString());
      } catch (final Exception e) {
        throw new SigningMetadataException("Invalid hex value for address", e);
      }
    }
  }

  public static class Bytes20Serializer extends JsonSerializer<Bytes20> {

    @Override
    public void serialize(
        final Bytes20 value, final JsonGenerator gen, final SerializerProvider serializers)
        throws IOException {
      gen.writeString(value.toHexString());
    }
  }
}
