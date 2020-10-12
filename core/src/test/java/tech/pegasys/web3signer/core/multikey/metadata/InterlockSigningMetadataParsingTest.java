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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static tech.pegasys.web3signer.core.multikey.metadata.parser.YamlSignerParser.OBJECT_MAPPER;

import tech.pegasys.web3signer.core.signing.KeyType;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.junit.jupiter.api.Test;

class InterlockSigningMetadataParsingTest {
  private static final String RET = System.lineSeparator();

  @Test
  void yamlParsingWithRequiredValuesWorks() throws IOException {
    StringBuilder yaml = new StringBuilder("type: interlock").append(RET);
    yaml.append("interlockUrl: https://10.0.0.1").append(RET);
    yaml.append("knownServersFile: ./interlockKnownServersFile.txt").append(RET);
    yaml.append("volume: armory").append(RET);
    yaml.append("password: usbarmory").append(RET);
    yaml.append("keyPath: /bls/key1.txt").append(RET);

    final SigningMetadata signingMetadata =
        OBJECT_MAPPER.readValue(yaml.toString(), SigningMetadata.class);
    assertThat(signingMetadata).isInstanceOf(InterlockSigningMetadata.class);

    final InterlockSigningMetadata metadata = (InterlockSigningMetadata) signingMetadata;
    assertThat(metadata.getInterlockUrl()).isEqualTo(URI.create("https://10.0.0.1"));
    assertThat(metadata.getKnownServersFile())
        .isEqualTo(Path.of("./interlockKnownServersFile.txt"));
    assertThat(metadata.getVolume()).isEqualTo("armory");
    assertThat(metadata.getPassword()).isEqualTo("usbarmory");
    assertThat(metadata.getKeyPath()).isEqualTo(Path.of("/bls/key1.txt"));
    assertThat(metadata.getKeyType()).isEqualTo(KeyType.BLS);
  }

  @Test
  void yamlParsingWithAllValuesWorks() throws IOException {
    StringBuilder yaml = new StringBuilder("type: interlock").append(RET);
    yaml.append("interlockUrl: https://10.0.0.1").append(RET);
    yaml.append("knownServersFile: ./interlockKnownServersFile.txt").append(RET);
    yaml.append("volume: armory").append(RET);
    yaml.append("password: usbarmory").append(RET);
    yaml.append("keyPath: /bls/key1.txt").append(RET);
    yaml.append("keyType: SECP256K1").append(RET);

    final SigningMetadata signingMetadata =
        OBJECT_MAPPER.readValue(yaml.toString(), SigningMetadata.class);
    assertThat(signingMetadata).isInstanceOf(InterlockSigningMetadata.class);

    final InterlockSigningMetadata metadata = (InterlockSigningMetadata) signingMetadata;
    assertThat(metadata.getInterlockUrl()).isEqualTo(URI.create("https://10.0.0.1"));
    assertThat(metadata.getKnownServersFile())
        .isEqualTo(Path.of("./interlockKnownServersFile.txt"));
    assertThat(metadata.getVolume()).isEqualTo("armory");
    assertThat(metadata.getPassword()).isEqualTo("usbarmory");
    assertThat(metadata.getKeyPath()).isEqualTo(Path.of("/bls/key1.txt"));
    assertThat(metadata.getKeyType()).isEqualTo(KeyType.SECP256K1);
  }

  @Test
  void yamlParsingWithoutRequiredOptionsFails() {
    StringBuilder yaml = new StringBuilder("type: interlock").append(RET);
    yaml.append("interlockUrl: https://10.0.0.1").append(RET);

    assertThatExceptionOfType(JsonMappingException.class)
        .isThrownBy(() -> OBJECT_MAPPER.readValue(yaml.toString(), SigningMetadata.class))
        .withMessageContaining("Missing required creator property 'knownServersFile'");
  }
}
