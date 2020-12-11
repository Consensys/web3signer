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
package tech.pegasys.web3signer.core.multikey.metadata;

import static tech.pegasys.web3signer.core.config.AzureAuthenticationMode.CLIENT_SECRET;

import tech.pegasys.web3signer.core.config.AzureAuthenticationMode;
import tech.pegasys.web3signer.core.signing.KeyType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class AzureSecretSigningMetadataDeserializer
    extends StdDeserializer<AzureSecretSigningMetadata> {
  private static final String CLIENT_ID = "clientId";
  private static final String CLIENT_SECRET = "clientSecret";
  private static final String TENANT_ID = "tenantId";
  private static final String VAULT_NAME = "vaultName";
  private static final String SECRET_NAME = "secretName";
  private static final String AUTH_MODE = "authenticationMode";
  private static final String KEY_TYPE = "keyType";

  private KeyType keyType;
  private String clientId;
  private String clientSecret;
  private String tenantId;
  private String vaultName;
  private String secretName;
  private AzureAuthenticationMode authenticationMode;

  @SuppressWarnings("Unused")
  public AzureSecretSigningMetadataDeserializer() {
    this(null);
  }

  protected AzureSecretSigningMetadataDeserializer(final Class<?> vc) {
    super(vc);
  }

  @Override
  public AzureSecretSigningMetadata deserialize(
      final JsonParser parser, final DeserializationContext ctxt) throws IOException {
    final JsonNode node = parser.getCodec().readTree(parser);

    if (node.get(KEY_TYPE) != null) {
      try {
        keyType = KeyType.valueOf(node.get(KEY_TYPE).asText());
      } catch (final IllegalArgumentException e) {
        throw new JsonMappingException(
            parser, "Error converting " + KEY_TYPE + ": " + e.getMessage());
      }
    }

    if (node.get(AUTH_MODE) != null) {
      try {
        authenticationMode = AzureAuthenticationMode.valueOf(node.get(AUTH_MODE).asText());
      } catch (final IllegalArgumentException e) {
        throw new JsonMappingException(
            parser, "Error converting " + AUTH_MODE + ": " + e.getMessage());
      }
    }

    if (node.get(VAULT_NAME) != null) {
      vaultName = node.get(VAULT_NAME).asText();
    }

    if (node.get(SECRET_NAME) != null) {
      secretName = node.get(SECRET_NAME).asText();
    }

    if (node.get(CLIENT_ID) != null) {
      clientId = node.get(CLIENT_ID).asText();
    }
    if (node.get(TENANT_ID) != null) {
      tenantId = node.get(TENANT_ID).asText();
    }
    if (node.get(CLIENT_SECRET) != null) {
      clientSecret = node.get(CLIENT_SECRET).asText();
    }

    validate(parser);

    return new AzureSecretSigningMetadata(
        clientId, clientSecret, tenantId, vaultName, secretName, authenticationMode, keyType);
  }

  // keyType and authMode can be null (for backward compatibility)
  // vaultName and secretName are REQUIRED.
  // clientId, clientSecret, tenantId are REQUIRED only if authMode is null OR CLIENT_SECRET
  private void validate(final JsonParser parser) throws JsonMappingException {
    final List<String> missingParameters = new ArrayList<>();
    if (vaultName == null) {
      missingParameters.add(VAULT_NAME);
    }
    if (secretName == null) {
      missingParameters.add(SECRET_NAME);
    }
    if (authenticationMode == null || authenticationMode == AzureAuthenticationMode.CLIENT_SECRET) {
      if (clientId == null) {
        missingParameters.add(CLIENT_ID);
      }
      if (clientSecret == null) {
        missingParameters.add(CLIENT_SECRET);
      }
      if (tenantId == null) {
        missingParameters.add(TENANT_ID);
      }
    }

    if (!missingParameters.isEmpty()) {
      throw new JsonMappingException(
          parser, "Missing values for required parameters: " + String.join(",", missingParameters));
    }
  }
}
