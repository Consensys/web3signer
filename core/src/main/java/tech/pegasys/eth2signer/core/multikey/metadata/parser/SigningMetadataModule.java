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

import tech.pegasys.artemis.util.bls.BLSSecretKey;
import tech.pegasys.artemis.util.mikuli.SecretKey;
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

public class SigningMetadataModule extends SimpleModule {

  public SigningMetadataModule() {
    super("SigningMetadata");
    addDeserializer(BLSSecretKey.class, new PrivateKeyDeserializer());
    addSerializer(BLSSecretKey.class, new PrivateKeySerializer());
  }

  private static class PrivateKeyDeserializer extends JsonDeserializer<BLSSecretKey> {

    @Override
    public BLSSecretKey deserialize(final JsonParser p, final DeserializationContext ctxt) {
      try {
        final Bytes privateKeyBytes = Bytes.fromHexString(p.getValueAsString());
        return BLSSecretKey.fromBytes(privateKeyBytes);
      } catch (Exception e) {
        throw new SigningMetadataException("Invalid hex value for private key", e);
      }
    }
  }

  private static class PrivateKeySerializer extends JsonSerializer<BLSSecretKey> {

    @Override
    public void serialize(
        final BLSSecretKey value, final JsonGenerator gen, final SerializerProvider serializers)
        throws IOException {
      gen.writeString(value.getSecretKey().toBytes().toString());
    }
  }
}
