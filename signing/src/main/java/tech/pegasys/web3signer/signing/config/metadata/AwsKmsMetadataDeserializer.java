/*
 * Copyright 2022 ConsenSys AG.
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
package tech.pegasys.web3signer.signing.config.metadata;

import tech.pegasys.web3signer.common.config.AwsAuthenticationMode;
import tech.pegasys.web3signer.common.config.AwsCredentials;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class AwsKmsMetadataDeserializer extends StdDeserializer<AwsKmsMetadata> {

  public static final String AUTH_MODE = "authenticationMode";
  public static final String REGION = "region";
  public static final String ACCESS_KEY_ID = "accessKeyId";
  public static final String SECRET_ACCESS_KEY = "secretAccessKey";
  public static final String SESSION_TOKEN = "sessionToken";
  public static final String KMS_KEY_ID = "kmsKeyId";
  public static final String ENDPOINT_OVERRIDE = "endpointOverride";

  @SuppressWarnings("Unused")
  public AwsKmsMetadataDeserializer() {
    this(null);
  }

  protected AwsKmsMetadataDeserializer(final Class<?> vc) {
    super(vc);
  }

  @Override
  public AwsKmsMetadata deserialize(final JsonParser parser, final DeserializationContext context)
      throws IOException {

    AwsAuthenticationMode authMode = AwsAuthenticationMode.SPECIFIED;
    String region = null;

    String accessKeyId = null;
    String secretAccessKey = null;
    String sessionToken = null;
    Optional<AwsCredentials> awsCredentials = Optional.empty();

    String kmsKeyId = null;
    Optional<URI> endpointOverride = Optional.empty();

    final JsonNode node = parser.getCodec().readTree(parser);

    if (node.get(AUTH_MODE) != null) {
      try {
        authMode = AwsAuthenticationMode.valueOf(node.get(AUTH_MODE).asText());
      } catch (final IllegalArgumentException e) {
        throw new JsonMappingException(parser, "Invalid value for parameter: " + AUTH_MODE + ".");
      }
    }

    if (node.get(REGION) != null) {
      region = node.get(REGION).asText();
    }

    if (node.get(ACCESS_KEY_ID) != null) {
      accessKeyId = node.get(ACCESS_KEY_ID).asText();
    }

    if (node.get(SECRET_ACCESS_KEY) != null) {
      secretAccessKey = node.get(SECRET_ACCESS_KEY).asText();
    }

    if (node.get(SESSION_TOKEN) != null) {
      sessionToken = node.get(SESSION_TOKEN).asText();
    }

    if (node.get(KMS_KEY_ID) != null) {
      kmsKeyId = node.get(KMS_KEY_ID).asText();
    }

    if (node.get(ENDPOINT_OVERRIDE) != null) {
      try {
        endpointOverride = Optional.of(URI.create(node.get(ENDPOINT_OVERRIDE).asText()));
      } catch (final IllegalArgumentException e) {
        throw new JsonMappingException(
            parser, "Invalid value for parameter: " + ENDPOINT_OVERRIDE + ".");
      }
    }

    // validate
    validate(parser, authMode, region, accessKeyId, secretAccessKey, kmsKeyId);

    if (authMode == AwsAuthenticationMode.SPECIFIED) {
      awsCredentials =
          Optional.of(
              AwsCredentials.builder()
                  .withAccessKeyId(accessKeyId)
                  .withSecretAccessKey(secretAccessKey)
                  .withSessionToken(sessionToken)
                  .build());
    }

    return new AwsKmsMetadata(authMode, region, awsCredentials, kmsKeyId, endpointOverride);
  }

  private void validate(
      final JsonParser parser,
      final AwsAuthenticationMode authMode,
      final String region,
      final String accessKeyId,
      final String secretAccessKey,
      final String kmsKeyId)
      throws JsonMappingException {
    final List<String> missingParameters = new ArrayList<>();

    // globally required fields
    if (region == null) {
      missingParameters.add(REGION);
    }

    if (kmsKeyId == null) {
      missingParameters.add(KMS_KEY_ID);
    }

    // Specified auth mode required fields
    if (authMode == AwsAuthenticationMode.SPECIFIED) {
      if (accessKeyId == null) {
        missingParameters.add(ACCESS_KEY_ID);
      }

      if (secretAccessKey == null) {
        missingParameters.add(SECRET_ACCESS_KEY);
      }
    }

    if (!missingParameters.isEmpty()) {
      throw new JsonMappingException(
          parser,
          "Missing values for required parameters: " + String.join(", ", missingParameters));
    }
  }
}
