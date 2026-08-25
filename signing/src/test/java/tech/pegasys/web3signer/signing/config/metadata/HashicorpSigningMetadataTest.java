/*
 * Copyright 2024 ConsenSys AG.
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

import tech.pegasys.web3signer.keystorage.hashicorp.VaultAuthMethod;

import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.Test;

class HashicorpSigningMetadataTest {

  private final YAMLMapper yamlMapper = new YAMLMapper();

  @Test
  void deserializesTokenAuthMethodByDefault() throws IOException {
    final String yaml =
        """
        type: "hashicorp"
        serverHost: "localhost"
        serverPort: 8200
        keyPath: "/v1/secret/data/mykey"
        token: "my-token"
        """;

    final HashicorpSigningMetadata metadata =
        yamlMapper.readValue(yaml, HashicorpSigningMetadata.class);

    assertThat(metadata.getAuthMethod()).isEqualTo(VaultAuthMethod.TOKEN);
    assertThat(metadata.getToken()).isEqualTo("my-token");
  }

  @Test
  void deserializesKubernetesAuthMethod() throws IOException {
    final String yaml =
        """
        type: "hashicorp"
        serverHost: "localhost"
        serverPort: 8200
        keyPath: "/v1/secret/data/mykey"
        authMethod: "KUBERNETES"
        kubernetesRole: "my-role"
        kubernetesAuthPath: "my-auth-path"
        kubernetesServiceAccountTokenPath: "/tmp/token"
        """;

    final HashicorpSigningMetadata metadata =
        yamlMapper.readValue(yaml, HashicorpSigningMetadata.class);

    assertThat(metadata.getAuthMethod()).isEqualTo(VaultAuthMethod.KUBERNETES);
    assertThat(metadata.getKubernetesRole()).isEqualTo("my-role");
    assertThat(metadata.getKubernetesAuthPath()).isEqualTo("my-auth-path");
    assertThat(metadata.getKubernetesServiceAccountTokenPath()).isEqualTo(Path.of("/tmp/token"));
    assertThat(metadata.getToken()).isNull();
  }

  @Test
  void deserializesWithoutAuthMethodDefaultsToToken() throws IOException {
    final String yaml =
        """
        type: "hashicorp"
        serverHost: "localhost"
        keyPath: "/v1/secret/data/mykey"
        token: "my-token"
        """;

    final HashicorpSigningMetadata metadata =
        yamlMapper.readValue(yaml, HashicorpSigningMetadata.class);

    assertThat(metadata.getAuthMethod()).isEqualTo(VaultAuthMethod.TOKEN);
    assertThat(metadata.getToken()).isEqualTo("my-token");
  }

  @Test
  void deserializesWithNullAuthMethodDefaultsToToken() throws IOException {
    final String yaml =
        """
        type: "hashicorp"
        serverHost: "localhost"
        keyPath: "/v1/secret/data/mykey"
        token: "my-token"
        authMethod: null
        """;

    final HashicorpSigningMetadata metadata =
        yamlMapper.readValue(yaml, HashicorpSigningMetadata.class);

    assertThat(metadata.getAuthMethod()).isEqualTo(VaultAuthMethod.TOKEN);
    assertThat(metadata.getToken()).isEqualTo("my-token");
  }
}
