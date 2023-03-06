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
package tech.pegasys.web3signer.signing.config.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import tech.pegasys.web3signer.signing.KeyType;
import tech.pegasys.web3signer.signing.config.metadata.parser.YamlMapperFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;

class InterlockSigningMetadataFileParsingTest {
  private static final YAMLMapper YAML_MAPPER = YamlMapperFactory.createYamlMapper();

  @Test
  void yamlFileIsSuccessfullyParsed() throws IOException {
    final URL testFile = Resources.getResource("interlock_test.yaml");

    final SigningMetadata signingMetadata = YAML_MAPPER.readValue(testFile, SigningMetadata.class);

    assertThat(signingMetadata).isInstanceOf(InterlockSigningMetadata.class);

    final InterlockSigningMetadata metadata = (InterlockSigningMetadata) signingMetadata;
    assertThat(metadata.getInterlockUrl()).isEqualTo(URI.create("https://testhost"));
    assertThat(metadata.getKnownServersFile()).isEqualTo(Path.of("./testFile.txt"));
    assertThat(metadata.getVolume()).isEqualTo("test");
    assertThat(metadata.getPassword()).isEqualTo("test");
    assertThat(metadata.getKeyPath()).isEqualTo("/test.txt");
    assertThat(metadata.getKeyType()).isEqualTo(KeyType.BLS);
  }

  @Test
  void yamlParsingWithIncompleteContentsFails() {
    final URL testFile = Resources.getResource("interlock_incomplete.yaml");

    assertThatExceptionOfType(JsonMappingException.class)
        .isThrownBy(() -> YAML_MAPPER.readValue(testFile, SigningMetadata.class))
        .withMessageContaining("Missing required creator property 'volume'");
  }
}
