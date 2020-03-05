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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static tech.pegasys.eth2signer.core.multikey.metadata.YamlSignerParser.YAML_FILE_EXTENSION;

import tech.pegasys.eth2signer.core.multikey.SignerType;
import tech.pegasys.eth2signer.core.signing.ArtifactSigner;
import tech.pegasys.eth2signer.crypto.KeyPair;
import tech.pegasys.eth2signer.crypto.SecretKey;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class YamlSignerParserTest {
  private static ObjectMapper YAML_OBJECT_MAPPER = new ObjectMapper(new YAMLFactory());
  private static final String PRIVATE_KEY =
      "000000000000000000000000000000003ee2224386c82ffea477e2adf28a2929f5c349165a4196158c7f3a2ecca40f35";
  private static final String PUBLIC_KEY =
      "989d34725a2bfc3f15105f3f5fc8741f436c25ee1ee4f948e425d6bcb8c56bce6e06c269635b7e985a7ffa639e2409bf";

  @Mock ArtifactSignerFactory artifactSignerFactory;
  @TempDir Path configDir;

  private YamlSignerParser signerParser;

  @BeforeEach
  public void setup() {
    signerParser = new YamlSignerParser(artifactSignerFactory);
  }

  @Test
  void metaDataInfoWithNonExistingFileReturnsEmpty() {
    final Optional<ArtifactSigner> metadataInfo =
        signerParser.parse(configDir.resolve("does_not_exist"));

    assertThat(metadataInfo).isEmpty();
  }

  @Test
  void metaDataInfoWithUnknownTypeReturnsEmpty() throws IOException {
    final Path filename = configDir.resolve("unknownType");
    YAML_OBJECT_MAPPER.writeValue(
        filename.toFile(), Map.of("type", SignerType.UNKNOWN_TYPE_SIGNER.name()));

    final Optional<ArtifactSigner> metadataInfo = signerParser.parse(filename);

    assertThat(metadataInfo).isEmpty();
  }

  @Test
  void metaDataInfoWithMissingTypeReturnsEmpty() throws IOException {
    final Path filename = configDir.resolve("empty");
    YAML_OBJECT_MAPPER.writeValue(filename.toFile(), Map.of());

    final Optional<ArtifactSigner> metadataInfo = signerParser.parse(filename);

    assertThat(metadataInfo).isEmpty();
  }

  @Test
  void unencryptedMetaDataInfoWithMissingPrivateKeyReturnsEmpty() throws IOException {
    final Path filename = configDir.resolve("unencryptedNoKey." + YAML_FILE_EXTENSION);
    YAML_OBJECT_MAPPER.writeValue(filename.toFile(), Map.of("type", SignerType.FILE_RAW.name()));

    final Optional<ArtifactSigner> metadataInfo = signerParser.parse(filename);

    assertThat(metadataInfo).isEmpty();
  }

  @Test
  void unencryptedMetaDataInfoWithPrivateKeyReturnsMetadata() throws IOException {
    final ArtifactSigner artifactSigner =
        new ArtifactSigner(new KeyPair(SecretKey.fromBytes(Bytes.fromHexString(PRIVATE_KEY))));
    when(artifactSignerFactory.createSigner(any())).thenReturn(artifactSigner);

    final Path filename = configDir.resolve("unencrypted." + YAML_FILE_EXTENSION);
    final Map<String, String> unencryptedKeyMetadataFile = new HashMap<>();
    unencryptedKeyMetadataFile.put("type", "FILE_RAW");
    unencryptedKeyMetadataFile.put("privateKey", PRIVATE_KEY);
    YAML_OBJECT_MAPPER.writeValue(filename.toFile(), unencryptedKeyMetadataFile);

    final Optional<ArtifactSigner> result = signerParser.parse(filename);

    assertThat(result).isNotEmpty();
    assertThat(result.get().getIdentifier()).isEqualTo("0x" + PUBLIC_KEY);
    verify(artifactSignerFactory)
        .createSigner(argThat(metaData -> metaData.getPrivateKey().equals(PRIVATE_KEY)));
  }
}
