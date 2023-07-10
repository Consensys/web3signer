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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import tech.pegasys.web3signer.common.config.AwsAuthenticationMode;
import tech.pegasys.web3signer.signing.KeyType;
import tech.pegasys.web3signer.signing.config.metadata.parser.YamlMapperFactory;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.Test;

class AwsKeySigningMetadataDeserializerTest {

  private static final String AWS_VALID_CONFIG_ENVIRONMENT_AUTH_MODE_PATH =
      "src/test/resources/aws/aws_valid_config_environment.yaml";
  private static final String AWS_VALID_CONFIG_SPECIFIED_AUTH_MODE_PATH =
      "src/test/resources/aws/aws_valid_config_specified.yaml";
  private static final YAMLMapper YAML_MAPPER = YamlMapperFactory.createYamlMapper();

  @Test
  public void deserializeValidAwsConfigWithEnvironmentAuthMode() throws IOException {
    final AwsKeySigningMetadata deserializedMetadata =
        YAML_MAPPER.readValue(
            new File(AWS_VALID_CONFIG_ENVIRONMENT_AUTH_MODE_PATH), AwsKeySigningMetadata.class);

    assertThat(deserializedMetadata.getAuthenticationMode())
        .isEqualTo(AwsAuthenticationMode.ENVIRONMENT);
    assertThat(deserializedMetadata.getKeyType()).isEqualTo(KeyType.BLS);
    assertThat(deserializedMetadata.getRegion()).isEqualTo("ap-southeast-2");
    assertThat(deserializedMetadata.getSecretName()).isEqualTo("NewSuperSecret");
    assertThat(deserializedMetadata.getAccessKeyId()).isNull();
    assertThat(deserializedMetadata.getSecretAccessKey()).isNull();
  }

  @Test
  public void deserializeAwsConfigWithEnvironmentAuthModeMissingSecretName() throws IOException {
    final File configFile = new File(AWS_VALID_CONFIG_ENVIRONMENT_AUTH_MODE_PATH);
    final String configWithoutRequiredField =
        stripField(configFile, AwsKeySigningMetadataDeserializer.SECRET_NAME);

    assertThatThrownBy(
            () -> YAML_MAPPER.readValue(configWithoutRequiredField, AwsKeySigningMetadata.class))
        .isInstanceOf(JsonMappingException.class)
        .hasMessageContaining("Missing values for required parameters: secretName");
  }

  @Test
  public void deserializeAwsConfigWithEnvironmentAuthModeMissingRegion() throws IOException {
    final File configFile = new File(AWS_VALID_CONFIG_ENVIRONMENT_AUTH_MODE_PATH);
    final String configWithoutRequiredField =
        stripField(configFile, AwsKeySigningMetadataDeserializer.REGION);

    assertThatThrownBy(
            () -> YAML_MAPPER.readValue(configWithoutRequiredField, AwsKeySigningMetadata.class))
        .isInstanceOf(JsonMappingException.class)
        .hasMessageContaining("Missing values for required parameters: region");
  }

  @Test
  public void deserializeValidAwsConfigWithSpecifiedAuthMode() throws IOException {
    final AwsKeySigningMetadata deserializedMetadata =
        YAML_MAPPER.readValue(
            new File(AWS_VALID_CONFIG_SPECIFIED_AUTH_MODE_PATH), AwsKeySigningMetadata.class);

    assertThat(deserializedMetadata.getAuthenticationMode())
        .isEqualTo(AwsAuthenticationMode.SPECIFIED);
    assertThat(deserializedMetadata.getKeyType()).isEqualTo(KeyType.BLS);
    assertThat(deserializedMetadata.getRegion()).isEqualTo("ap-southeast-2");
    assertThat(deserializedMetadata.getSecretName()).isEqualTo("NewSuperSecret");
    assertThat(deserializedMetadata.getAccessKeyId()).isEqualTo("foo");
    assertThat(deserializedMetadata.getSecretAccessKey()).isEqualTo("bar");
  }

  @Test
  public void deserializeAwsConfigWithSpecifiedAuthModeMissingSecretName() throws IOException {
    final File configFile = new File(AWS_VALID_CONFIG_SPECIFIED_AUTH_MODE_PATH);
    final String configWithoutRequiredField =
        stripField(configFile, AwsKeySigningMetadataDeserializer.SECRET_NAME);

    assertThatThrownBy(
            () -> YAML_MAPPER.readValue(configWithoutRequiredField, AwsKeySigningMetadata.class))
        .isInstanceOf(JsonMappingException.class)
        .hasMessageContaining("Missing values for required parameters: secretName");
  }

  @Test
  public void deserializeAwsConfigWithSpecifiedAuthModeMissingRegion() throws IOException {
    final File configFile = new File(AWS_VALID_CONFIG_SPECIFIED_AUTH_MODE_PATH);
    final String configWithoutRequiredField =
        stripField(configFile, AwsKeySigningMetadataDeserializer.REGION);

    assertThatThrownBy(
            () -> YAML_MAPPER.readValue(configWithoutRequiredField, AwsKeySigningMetadata.class))
        .isInstanceOf(JsonMappingException.class)
        .hasMessageContaining("Missing values for required parameters: region");
  }

  @Test
  public void deserializeAwsConfigWithSpecifiedAuthModeMissingAccessKeyId() throws IOException {
    final File configFile = new File(AWS_VALID_CONFIG_SPECIFIED_AUTH_MODE_PATH);
    final String configWithoutRequiredField =
        stripField(configFile, AwsKeySigningMetadataDeserializer.ACCESS_KEY_ID);

    assertThatThrownBy(
            () -> YAML_MAPPER.readValue(configWithoutRequiredField, AwsKeySigningMetadata.class))
        .isInstanceOf(JsonMappingException.class)
        .hasMessageContaining("Missing values for required parameters: accessKeyId");
  }

  @Test
  public void deserializeAwsConfigWithSpecifiedAuthModeMissingSecretAccessKey() throws IOException {
    final File configFile = new File(AWS_VALID_CONFIG_SPECIFIED_AUTH_MODE_PATH);
    final String configWithoutRequiredField =
        stripField(configFile, AwsKeySigningMetadataDeserializer.SECRET_ACCESS_KEY);

    assertThatThrownBy(
            () -> YAML_MAPPER.readValue(configWithoutRequiredField, AwsKeySigningMetadata.class))
        .isInstanceOf(JsonMappingException.class)
        .hasMessageContaining("Missing values for required parameters: secretAccessKey");
  }

  @Test
  public void deserializeAwsConfigWithInvalidAuthMode() throws IOException {
    final File configFile = new File(AWS_VALID_CONFIG_SPECIFIED_AUTH_MODE_PATH);
    final String configWithoutRequiredField =
        overrideField(configFile, AwsKeySigningMetadataDeserializer.AUTH_MODE, "foo");

    assertThatThrownBy(
            () -> YAML_MAPPER.readValue(configWithoutRequiredField, AwsKeySigningMetadata.class))
        .isInstanceOf(JsonMappingException.class)
        .hasMessageContaining("Error converting authenticationMode");
  }

  private String stripField(final File file, final String fieldName) throws IOException {
    final ObjectNode node = (ObjectNode) YAML_MAPPER.readTree(file);
    node.remove(fieldName);
    return node.toString();
  }

  private String overrideField(final File file, final String fieldName, final String newValue)
      throws IOException {
    final ObjectNode node = (ObjectNode) YAML_MAPPER.readTree(file);
    node.remove(fieldName);
    node.put(fieldName, newValue);
    return node.toString();
  }
}
