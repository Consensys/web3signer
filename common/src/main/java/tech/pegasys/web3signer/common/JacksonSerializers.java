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
package tech.pegasys.web3signer.common;

import java.io.IOException;
import java.math.BigInteger;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;

public class JacksonSerializers {

  public static class Base64Deserialiser extends JsonDeserializer<Bytes> {

    @Override
    public Bytes deserialize(final JsonParser p, final DeserializationContext ctxt) {
      try {
        return Bytes.fromBase64String(p.getValueAsString());
      } catch (final Exception e) {
        throw new RuntimeException("Failed to decode as a Base64 string.", e);
      }
    }
  }

  public static class Base64Serialiser extends JsonSerializer<Bytes> {
    @Override
    public void serialize(
        final Bytes value, final JsonGenerator gen, final SerializerProvider serializers)
        throws IOException {
      gen.writeString(value.toBase64String());
    }
  }

  public static class HexDeserialiser extends JsonDeserializer<Bytes> {

    @Override
    public Bytes deserialize(final JsonParser p, final DeserializationContext ctxt) {
      try {
        return Bytes.fromHexString(p.getValueAsString());
      } catch (final Exception e) {
        throw new RuntimeException("Failed to decode as a hex string", e);
      }
    }
  }

  public static class HexSerialiser extends JsonSerializer<Bytes> {
    @Override
    public void serialize(
        final Bytes value, final JsonGenerator gen, final SerializerProvider serializers)
        throws IOException {
      gen.writeString(value.toHexString());
    }
  }

  public static class BigIntegerDecimalStringDeserialiser extends JsonDeserializer<BigInteger> {

    @Override
    public BigInteger deserialize(final JsonParser p, final DeserializationContext ctxt) {
      try {
        return new BigInteger(p.getValueAsString());
      } catch (final Exception e) {
        throw new RuntimeException("Failed to parse string as a BigInteger", e);
      }
    }
  }

  public static class BigIntegerDecimalStringSerialiser extends JsonSerializer<BigInteger> {
    @Override
    public void serialize(
        final BigInteger value, final JsonGenerator gen, final SerializerProvider serializers)
        throws IOException {
      gen.writeString(value.toString(10));
    }
  }

  public static class NumberUInt64Deserializer extends JsonDeserializer<UInt64> {
    @Override
    public UInt64 deserialize(final JsonParser p, final DeserializationContext ctxt) {
      try {
        return UInt64.valueOf(p.getBigIntegerValue());
      } catch (final Exception e) {
        throw new RuntimeException("Failed to parse integer as a UInt64", e);
      }
    }
  }

  public static class NumberUInt64Serialiser extends JsonSerializer<UInt64> {
    @Override
    public void serialize(
        final UInt64 value, final JsonGenerator gen, final SerializerProvider serializers)
        throws IOException {
      gen.writeNumber(value.toBigInteger());
    }
  }

  public static class StringUInt64Deserializer extends JsonDeserializer<UInt64> {
    @Override
    public UInt64 deserialize(final JsonParser p, final DeserializationContext ctxt) {
      try {
        return UInt64.valueOf(new BigInteger(p.getValueAsString()));
      } catch (final Exception e) {
        throw new RuntimeException("Failed to parse string as a UInt64", e);
      }
    }
  }

  public static class StringUInt64Serialiser extends JsonSerializer<UInt64> {
    @Override
    public void serialize(
        final UInt64 value, final JsonGenerator gen, final SerializerProvider serializers)
        throws IOException {
      gen.writeString(value.toString());
    }
  }
}
