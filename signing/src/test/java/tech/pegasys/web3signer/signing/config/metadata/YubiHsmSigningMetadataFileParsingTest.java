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
import java.net.URL;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;

class YubiHsmSigningMetadataFileParsingTest {
  private static final short EXPECTED_AUTH_ID = (short) 1;
  private static final short EXPECTED_OPAQUE_ID = (short) 1;

  private static final YAMLMapper YAML_MAPPER = YamlMapperFactory.createYamlMapper();

  @Test
  void yamlFileWithRequiredValuesIsSuccessfullyParsed() throws IOException {
    final URL testFile = Resources.getResource("yubihsm_required.yaml");

    final SigningMetadata signingMetadata = YAML_MAPPER.readValue(testFile, SigningMetadata.class);

    assertThat(signingMetadata).isInstanceOf(YubiHsmSigningMetadata.class);

    final YubiHsmSigningMetadata metadata = (YubiHsmSigningMetadata) signingMetadata;
    assertThat(metadata.getPkcs11ModulePath()).isEqualTo("/path/to/yubihsm_pkcs11.so");
    assertThat(metadata.getConnectorUrl()).isEqualTo("http://localhost:12345");
    assertThat(metadata.getAdditionalInitConfig()).isNull();
    assertThat(metadata.getAuthId()).isEqualTo(EXPECTED_AUTH_ID);
    assertThat(metadata.getOpaqueDataId()).isEqualTo(EXPECTED_OPAQUE_ID);
    assertThat(metadata.getKeyType()).isEqualTo(KeyType.BLS);
  }

  @Test
  void yamlFileWithCompleteValuesIsSuccessfullyParsed() throws IOException {
    final URL testFile = Resources.getResource("yubihsm_complete.yaml");

    final SigningMetadata signingMetadata = YAML_MAPPER.readValue(testFile, SigningMetadata.class);

    assertThat(signingMetadata).isInstanceOf(YubiHsmSigningMetadata.class);

    final YubiHsmSigningMetadata metadata = (YubiHsmSigningMetadata) signingMetadata;
    assertThat(metadata.getPkcs11ModulePath()).isEqualTo("/path/to/yubihsm_pkcs11.so");
    assertThat(metadata.getConnectorUrl()).isEqualTo("http://localhost:12345");
    assertThat(metadata.getAdditionalInitConfig()).isEqualTo("debug libdebug");
    assertThat(metadata.getAuthId()).isEqualTo(EXPECTED_AUTH_ID);
    assertThat(metadata.getOpaqueDataId()).isEqualTo(EXPECTED_OPAQUE_ID);
    assertThat(metadata.getKeyType()).isEqualTo(KeyType.SECP256K1);
  }

  @Test
  void yamlParsingWithIncompleteContentsFails() {
    final URL testFile = Resources.getResource("yubihsm_incomplete.yaml");

    assertThatExceptionOfType(JsonMappingException.class)
        .isThrownBy(() -> YAML_MAPPER.readValue(testFile, SigningMetadata.class))
        .withMessageContaining("Missing required creator property 'connectorUrl'");
  }
}
