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
package tech.pegasys.web3signer.core.service.jsonrpc;

import tech.pegasys.web3signer.core.multikey.metadata.SigningMetadataException;

import java.io.IOException;
import java.math.BigInteger;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;

public class FilecoinJsonRpcModule extends SimpleModule {

  public FilecoinJsonRpcModule() {
    super("FilecoinJsonRpcModule");
    addDeserializer(Bytes.class, new Base64Deserialiser());
    addSerializer(Bytes.class, new Base64Serialiser());
    addDeserializer(UInt64.class, new UInt64Deserializer());
    addSerializer(UInt64.class, new UInt64Serialiser());
    addDeserializer(BigInteger.class, new BigIntegerDecimalStringDeserialiser());
    addSerializer(BigInteger.class, new BigIntegerDecimalStringSerialiser());
  }

  public static class Base64Deserialiser extends JsonDeserializer<Bytes> {

    @Override
    public Bytes deserialize(final JsonParser p, final DeserializationContext ctxt) {
      try {
        return Bytes.fromBase64String(p.getValueAsString());
      } catch (final Exception e) {
        throw new SigningMetadataException("Invalid hex value for private key", e);
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

  public static class BigIntegerDecimalStringDeserialiser extends JsonDeserializer<BigInteger> {

    @Override
    public BigInteger deserialize(final JsonParser p, final DeserializationContext ctxt) {
      try {
        return new BigInteger(p.getValueAsString());
      } catch (final Exception e) {
        throw new SigningMetadataException("Invalid hex value for private key", e);
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

  public static class UInt64Deserializer extends JsonDeserializer<UInt64> {
    @Override
    public UInt64 deserialize(final JsonParser p, final DeserializationContext ctxt) {
      try {
        return UInt64.valueOf(p.getBigIntegerValue());
      } catch (final Exception e) {
        throw new SigningMetadataException("Invalid hex value for private key", e);
      }
    }
  }

  public static class UInt64Serialiser extends JsonSerializer<UInt64> {
    @Override
    public void serialize(
        final UInt64 value, final JsonGenerator gen, final SerializerProvider serializers)
        throws IOException {
      gen.writeNumber(value.toBigInteger());
    }
  }
}
