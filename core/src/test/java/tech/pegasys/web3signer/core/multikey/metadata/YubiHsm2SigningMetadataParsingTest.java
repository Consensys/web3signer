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

import tech.pegasys.signers.yubihsm2.OutputFormat;
import tech.pegasys.web3signer.core.signing.KeyType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.junit.jupiter.api.Test;

class YubiHsm2SigningMetadataParsingTest {
  private static final String RET = System.lineSeparator();

  @Test
  void yamlParsingWithRequiredValuesWorks() throws IOException {
    StringBuilder yaml = new StringBuilder("type: yubihsm2").append(RET);
    yaml.append("yubiShellBinaryPath: /some/path/bin/yubihsm-shell").append(RET);
    yaml.append("connectorUrl: http://localhost:12345").append(RET);
    yaml.append("authKey: 1").append(RET);
    yaml.append("password: password").append(RET);
    yaml.append("opaqueObjId: 5").append(RET);
    yaml.append("keyType: BLS").append(RET);

    final SigningMetadata signingMetadata =
        OBJECT_MAPPER.readValue(
            yaml.toString().getBytes(StandardCharsets.UTF_8), SigningMetadata.class);
    assertThat(signingMetadata).isInstanceOf(YubiHsm2SigningMetadata.class);

    final YubiHsm2SigningMetadata metadata = (YubiHsm2SigningMetadata) signingMetadata;
    assertThat(metadata.getYubiShellBinaryPath()).isEqualTo("/some/path/bin/yubihsm-shell");
    assertThat(metadata.getConnectorUrl()).isEqualTo("http://localhost:12345");
    assertThat(metadata.getAuthKey()).isEqualTo((short) 1);
    assertThat(metadata.getOpaqueObjId()).isEqualTo((short) 5);
    assertThat(metadata.getKeyType()).isEqualTo(KeyType.BLS);
    assertThat(metadata.getOutformat()).isEmpty();
    assertThat(metadata.getCaCertPath()).isEmpty();
    assertThat(metadata.getProxyUrl()).isEmpty();
  }

  @Test
  void yamlParsingWithAllValuesWorks() throws IOException {
    StringBuilder yaml = new StringBuilder("type: yubihsm2").append(RET);
    yaml.append("yubiShellBinaryPath: /some/path/bin/yubihsm-shell").append(RET);
    yaml.append("connectorUrl: http://localhost:12345").append(RET);
    yaml.append("authKey: 1").append(RET);
    yaml.append("password: password").append(RET);
    yaml.append("opaqueObjId: 5").append(RET);
    yaml.append("keyType: BLS").append(RET);
    yaml.append("outformat: hex").append(RET);
    yaml.append("caCertPath: /some/path/cert.crt").append(RET);
    yaml.append("proxyUrl: http://proxy:8080/").append(RET);

    final SigningMetadata signingMetadata =
        OBJECT_MAPPER.readValue(
            yaml.toString().getBytes(StandardCharsets.UTF_8), SigningMetadata.class);
    assertThat(signingMetadata).isInstanceOf(YubiHsm2SigningMetadata.class);

    final YubiHsm2SigningMetadata metadata = (YubiHsm2SigningMetadata) signingMetadata;
    assertThat(metadata.getYubiShellBinaryPath()).isEqualTo("/some/path/bin/yubihsm-shell");
    assertThat(metadata.getConnectorUrl()).isEqualTo("http://localhost:12345");
    assertThat(metadata.getAuthKey()).isEqualTo((short) 1);
    assertThat(metadata.getOpaqueObjId()).isEqualTo((short) 5);
    assertThat(metadata.getKeyType()).isEqualTo(KeyType.BLS);
    assertThat(metadata.getOutformat().get()).isEqualTo(OutputFormat.HEX);
    assertThat(metadata.getCaCertPath().get()).isEqualTo("/some/path/cert.crt");
    assertThat(metadata.getProxyUrl().get()).isEqualTo("http://proxy:8080/");
  }

  @Test
  void yamlParsingWithSomeOptionalWorks() throws IOException {
    StringBuilder yaml = new StringBuilder("type: yubihsm2").append(RET);
    yaml.append("yubiShellBinaryPath: /some/path/bin/yubihsm-shell").append(RET);
    yaml.append("connectorUrl: http://localhost:12345").append(RET);
    yaml.append("authKey: 1").append(RET);
    yaml.append("password: password").append(RET);
    yaml.append("opaqueObjId: 5").append(RET);
    yaml.append("keyType: BLS").append(RET);
    yaml.append("outformat: ASCII").append(RET);

    final SigningMetadata signingMetadata =
        OBJECT_MAPPER.readValue(
            yaml.toString().getBytes(StandardCharsets.UTF_8), SigningMetadata.class);
    assertThat(signingMetadata).isInstanceOf(YubiHsm2SigningMetadata.class);

    final YubiHsm2SigningMetadata metadata = (YubiHsm2SigningMetadata) signingMetadata;
    assertThat(metadata.getYubiShellBinaryPath()).isEqualTo("/some/path/bin/yubihsm-shell");
    assertThat(metadata.getConnectorUrl()).isEqualTo("http://localhost:12345");
    assertThat(metadata.getAuthKey()).isEqualTo((short) 1);
    assertThat(metadata.getOpaqueObjId()).isEqualTo((short) 5);
    assertThat(metadata.getKeyType()).isEqualTo(KeyType.BLS);
    assertThat(metadata.getOutformat().get()).isEqualTo(OutputFormat.ASCII);
    assertThat(metadata.getCaCertPath()).isEmpty();
    assertThat(metadata.getProxyUrl()).isEmpty();
  }

  @Test
  void yamlParsingWithoutRequiredOptionsFails() {
    StringBuilder yaml = new StringBuilder("type: yubihsm2").append(RET);
    yaml.append("connectorUrl: http://localhost:12345").append(RET);
    yaml.append("authKey: 1").append(RET);
    yaml.append("password: password").append(RET);
    yaml.append("opaqueObjId: 5").append(RET);
    yaml.append("keyType: BLS").append(RET);
    yaml.append("outformat: ASCII").append(RET);

    assertThatExceptionOfType(JsonMappingException.class)
        .isThrownBy(
            () ->
                OBJECT_MAPPER.readValue(
                    yaml.toString().getBytes(StandardCharsets.UTF_8), SigningMetadata.class))
        .withMessageContaining("yubiShellBinaryPath is required");
  }
}
