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
package tech.pegasys.eth2signer.core.multikey.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import tech.pegasys.eth2signer.core.multikey.metadata.parser.YamlSignerParser;
import tech.pegasys.eth2signer.core.signing.ArtifactSigner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class YamlSignerParserTest {

  private static final String YAML_FILE_EXTENSION = "yaml";
  private static ObjectMapper YAML_OBJECT_MAPPER = new ObjectMapper(new YAMLFactory());
  private static final String PRIVATE_KEY =
      "000000000000000000000000000000003ee2224386c82ffea477e2adf28a2929f5c349165a4196158c7f3a2ecca40f35";
  private static final String PUBLIC_KEY =
      "989d34725a2bfc3f15105f3f5fc8741f436c25ee1ee4f948e425d6bcb8c56bce6e06c269635b7e985a7ffa639e2409bf";

  @TempDir Path configDir;

  private YamlSignerParser signerParser;

  @BeforeEach
  public void setup() {
    signerParser = new YamlSignerParser(Path.of("."), new NoOpMetricsSystem());
  }

  @Test
  void metaDataInfoWithNonExistingFileFails() {
    assertThatThrownBy(() -> signerParser.parse(configDir.resolve("does_not_exist")))
        .isInstanceOf(SigningMetadataException.class)
        .hasMessageStartingWith("Signing metadata file not found");
  }

  @Test
  void metaDataInfoWithUnknownTypeFails() throws IOException {
    final Path filename = configDir.resolve("unknownType");
    YAML_OBJECT_MAPPER.writeValue(filename.toFile(), Map.of("type", "unknown"));

    assertThatThrownBy(() -> signerParser.parse(filename))
        .isInstanceOf(SigningMetadataException.class)
        .hasMessageStartingWith("Invalid signing metadata file");
  }

  @Test
  void metaDataInfoWithMissingTypeFails() throws IOException {
    final Path filename = configDir.resolve("empty");
    YAML_OBJECT_MAPPER.writeValue(filename.toFile(), Map.of());

    assertThatThrownBy(() -> signerParser.parse(filename))
        .isInstanceOf(SigningMetadataException.class)
        .hasMessageStartingWith("Invalid signing metadata file");
  }

  @Test
  void unencryptedMetaDataInfoWithMissingPrivateKeyFails() throws IOException {
    final Path filename = configDir.resolve("unencryptedNoKey." + YAML_FILE_EXTENSION);
    YAML_OBJECT_MAPPER.writeValue(filename.toFile(), Map.of("type", "file-raw"));

    assertThatThrownBy(() -> signerParser.parse(filename))
        .isInstanceOf(SigningMetadataException.class)
        .hasMessageStartingWith(
            "Invalid signing metadata file format: Missing required creator property 'privateKey'");
  }

  @Test
  void unencryptedMetaDataInfoWithInvalidHexEncodingForPrivateKeyFails() throws IOException {
    final Path filename = configDir.resolve("unencrypted." + YAML_FILE_EXTENSION);
    final Map<String, String> unencryptedKeyMetadataFile = new HashMap<>();
    unencryptedKeyMetadataFile.put("type", "file-raw");
    unencryptedKeyMetadataFile.put("privateKey", "NO_HEX_VALUE");
    YAML_OBJECT_MAPPER.writeValue(filename.toFile(), unencryptedKeyMetadataFile);

    assertThatThrownBy(() -> signerParser.parse(filename))
        .isInstanceOf(SigningMetadataException.class)
        .hasMessageStartingWith(
            "Invalid signing metadata file format: Invalid hex value for private key");
  }

  @Test
  void unencryptedMetaDataInfoWithPrivateKeyReturnsMetadata() throws IOException {
    final Path filename = configDir.resolve("unencrypted." + YAML_FILE_EXTENSION);
    final Map<String, String> unencryptedKeyMetadataFile = new HashMap<>();
    unencryptedKeyMetadataFile.put("type", "file-raw");
    unencryptedKeyMetadataFile.put("privateKey", PRIVATE_KEY);
    YAML_OBJECT_MAPPER.writeValue(filename.toFile(), unencryptedKeyMetadataFile);

    final ArtifactSigner result = signerParser.parse(filename);

    assertThat(result.getIdentifier()).isEqualTo("0x" + PUBLIC_KEY);
  }

  @Test
  void unencryptedMetaDataInfoWith0xPrefixPrivateKeyReturnsMetadata() throws IOException {
    final Path filename = configDir.resolve("unencrypted." + YAML_FILE_EXTENSION);
    final Map<String, String> unencryptedKeyMetadataFile = new HashMap<>();
    unencryptedKeyMetadataFile.put("type", "file-raw");
    unencryptedKeyMetadataFile.put("privateKey", "0x" + PRIVATE_KEY);
    YAML_OBJECT_MAPPER.writeValue(filename.toFile(), unencryptedKeyMetadataFile);

    final ArtifactSigner result = signerParser.parse(filename);

    assertThat(result.getIdentifier()).isEqualTo("0x" + PUBLIC_KEY);
  }
}
