/*
 * Copyright 2023 ConsenSys AG.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static tech.pegasys.web3signer.common.config.AwsAuthenticationMode.ENVIRONMENT;
import static tech.pegasys.web3signer.common.config.AwsAuthenticationMode.SPECIFIED;
import static tech.pegasys.web3signer.signing.config.metadata.AwsKmsMetadataDeserializer.ACCESS_KEY_ID;
import static tech.pegasys.web3signer.signing.config.metadata.AwsKmsMetadataDeserializer.AUTH_MODE;
import static tech.pegasys.web3signer.signing.config.metadata.AwsKmsMetadataDeserializer.ENDPOINT_OVERRIDE;
import static tech.pegasys.web3signer.signing.config.metadata.AwsKmsMetadataDeserializer.KMS_KEY_ID;
import static tech.pegasys.web3signer.signing.config.metadata.AwsKmsMetadataDeserializer.REGION;
import static tech.pegasys.web3signer.signing.config.metadata.AwsKmsMetadataDeserializer.SECRET_ACCESS_KEY;
import static tech.pegasys.web3signer.signing.config.metadata.AwsKmsMetadataDeserializer.SESSION_TOKEN;

import tech.pegasys.web3signer.signing.KeyType;
import tech.pegasys.web3signer.signing.config.metadata.parser.YamlMapperFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.Test;

class AwsKmsMetadataDeserializerTest {
  private static final YAMLMapper YAML_MAPPER = YamlMapperFactory.createYamlMapper();

  @Test
  void minimalRequiredFieldsAreDeserialized() throws JsonProcessingException {
    final Map<String, Object> metadataMap = new HashMap<>();
    metadataMap.put("type", "aws-kms");
    metadataMap.put(REGION, "us-east-2");
    metadataMap.put(KMS_KEY_ID, "aaabbbcccddd");
    metadataMap.put(AUTH_MODE, ENVIRONMENT);

    final AwsKmsMetadata metadata =
        YAML_MAPPER.readValue(metadataYaml(metadataMap), AwsKmsMetadata.class);

    assertThat(metadata.getType()).isEqualTo("aws-kms");
    assertThat(metadata.getKeyType()).isEqualTo(KeyType.SECP256K1);
    assertThat(metadata.getKmsKeyId()).isEqualTo("aaabbbcccddd");

    assertThat(metadata.getRegion()).isEqualTo("us-east-2");
    assertThat(metadata.getAuthenticationMode()).isEqualTo(ENVIRONMENT);
    assertThat(metadata.getAwsCredentials()).isEmpty();
    assertThat(metadata.getEndpointOverride()).isEmpty();
  }

  @Test
  void defaultAuthenticationModeIsDeserialized() throws JsonProcessingException {
    final Map<String, Object> metadataMap = new HashMap<>();
    metadataMap.put("type", "aws-kms");
    metadataMap.put(REGION, "us-east-2");
    metadataMap.put(KMS_KEY_ID, "aaabbbcccddd");
    metadataMap.put(ACCESS_KEY_ID, "acc_key_id");
    metadataMap.put(SECRET_ACCESS_KEY, "sec_acc_key");

    final AwsKmsMetadata metadata =
        YAML_MAPPER.readValue(metadataYaml(metadataMap), AwsKmsMetadata.class);

    assertThat(metadata.getType()).isEqualTo("aws-kms");
    assertThat(metadata.getKeyType()).isEqualTo(KeyType.SECP256K1);
    assertThat(metadata.getKmsKeyId()).isEqualTo("aaabbbcccddd");
    assertThat(metadata.getRegion()).isEqualTo("us-east-2");

    assertThat(metadata.getAuthenticationMode()).isEqualTo(SPECIFIED);
    assertThat(metadata.getAwsCredentials()).isNotEmpty();
    assertThat(metadata.getAwsCredentials().get().getAccessKeyId()).isEqualTo("acc_key_id");
    assertThat(metadata.getAwsCredentials().get().getSecretAccessKey()).isEqualTo("sec_acc_key");
    assertThat(metadata.getAwsCredentials().get().getSessionToken()).isEmpty();

    assertThat(metadata.getEndpointOverride()).isEmpty();
  }

  @Test
  void credentialsAreIgnoredWhenEnvironmentAuthModeIsUsed() throws JsonProcessingException {
    final Map<String, Object> metadataMap = new HashMap<>();
    metadataMap.put("type", "aws-kms");
    metadataMap.put(REGION, "us-east-2");
    metadataMap.put(KMS_KEY_ID, "aaabbbcccddd");
    metadataMap.put(AUTH_MODE, ENVIRONMENT);
    metadataMap.put(ACCESS_KEY_ID, "acc_key_id");
    metadataMap.put(SECRET_ACCESS_KEY, "sec_acc_key");

    final AwsKmsMetadata metadata =
        YAML_MAPPER.readValue(metadataYaml(metadataMap), AwsKmsMetadata.class);

    assertThat(metadata.getType()).isEqualTo("aws-kms");
    assertThat(metadata.getKeyType()).isEqualTo(KeyType.SECP256K1);
    assertThat(metadata.getKmsKeyId()).isEqualTo("aaabbbcccddd");
    assertThat(metadata.getRegion()).isEqualTo("us-east-2");

    assertThat(metadata.getAuthenticationMode()).isEqualTo(ENVIRONMENT);
    assertThat(metadata.getAwsCredentials()).isEmpty();
    assertThat(metadata.getEndpointOverride()).isEmpty();
  }

  @Test
  void allOptionalValuesAreDeserialized() throws JsonProcessingException {
    final Map<String, Object> metadataMap = new HashMap<>();
    metadataMap.put("type", "aws-kms");
    metadataMap.put(REGION, "us-east-2");
    metadataMap.put(KMS_KEY_ID, "aaabbbcccddd");
    metadataMap.put(AUTH_MODE, SPECIFIED);
    metadataMap.put(ACCESS_KEY_ID, "acc_key_id");
    metadataMap.put(SECRET_ACCESS_KEY, "sec_acc_key");
    metadataMap.put(SESSION_TOKEN, "sess_token");
    metadataMap.put(ENDPOINT_OVERRIDE, "http://localhost:4566");
    metadataMap.put("extraField", "shouldBeIgnored");

    final AwsKmsMetadata metadata =
        YAML_MAPPER.readValue(metadataYaml(metadataMap), AwsKmsMetadata.class);

    assertThat(metadata.getType()).isEqualTo("aws-kms");
    assertThat(metadata.getKeyType()).isEqualTo(KeyType.SECP256K1);
    assertThat(metadata.getRegion()).isEqualTo("us-east-2");
    assertThat(metadata.getAuthenticationMode()).isEqualTo(SPECIFIED);
    assertThat(metadata.getAwsCredentials()).isNotEmpty();
    assertThat(metadata.getAwsCredentials().get().getAccessKeyId()).isEqualTo("acc_key_id");
    assertThat(metadata.getAwsCredentials().get().getSecretAccessKey()).isEqualTo("sec_acc_key");
    assertThat(metadata.getAwsCredentials().get().getSessionToken()).get().isEqualTo("sess_token");
    assertThat(metadata.getEndpointOverride()).get().isEqualTo(URI.create("http://localhost:4566"));
    assertThat(metadata.getKmsKeyId()).isEqualTo("aaabbbcccddd");
  }

  @Test
  void missingRequiredFieldsThrowsException() {
    final Map<String, Object> metadataMap = new HashMap<>();
    metadataMap.put("type", "aws-kms");

    assertThatExceptionOfType(JsonProcessingException.class)
        .isThrownBy(() -> YAML_MAPPER.readValue(metadataYaml(metadataMap), AwsKmsMetadata.class))
        .withMessageContaining(
            "Missing values for required parameters: region, kmsKeyId, accessKeyId, secretAccessKey");
  }

  @Test
  void invalidAuthTypeThrowsException() {
    final Map<String, Object> metadataMap = new HashMap<>();
    metadataMap.put("type", "aws-kms");
    metadataMap.put(REGION, "us-east-2");
    metadataMap.put(AUTH_MODE, "unknown_mode");

    assertThatExceptionOfType(JsonProcessingException.class)
        .isThrownBy(() -> YAML_MAPPER.readValue(metadataYaml(metadataMap), AwsKmsMetadata.class))
        .withMessageContaining("Invalid value for parameter: " + AUTH_MODE + ".");
  }

  @Test
  void invalidEndpointURIThrowsException() {
    final Map<String, Object> metadataMap = new HashMap<>();
    metadataMap.put("type", "aws-kms");
    metadataMap.put(REGION, "us-east-2");
    metadataMap.put(KMS_KEY_ID, "aaabbbcccddd");
    metadataMap.put(ACCESS_KEY_ID, "acc_key_id");
    metadataMap.put(SECRET_ACCESS_KEY, "sec_acc_key");
    metadataMap.put(SESSION_TOKEN, "sess_token");
    metadataMap.put(ENDPOINT_OVERRIDE, "invalid_url:80:80");

    assertThatExceptionOfType(JsonProcessingException.class)
        .isThrownBy(() -> YAML_MAPPER.readValue(metadataYaml(metadataMap), AwsKmsMetadata.class))
        .withMessageContaining("Invalid value for parameter: " + ENDPOINT_OVERRIDE + ".");
  }

  private static String metadataYaml(Map<String, Object> map) throws JsonProcessingException {
    return YAML_MAPPER.writeValueAsString(map);
  }
}
