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
import static tech.pegasys.eth2signer.core.multikey.SignerType.FILE_RAW;
import static tech.pegasys.eth2signer.core.multikey.SignerType.UNKNOWN_TYPE_SIGNER;
import static tech.pegasys.eth2signer.core.multikey.metadata.YamlSigningMetadataFileProvider.YAML_FILE_EXTENSION;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlSigningMetadataFileProviderTest {
  final YamlSigningMetadataFileProvider signingMetadataFileProvider =
      new YamlSigningMetadataFileProvider();
  private static ObjectMapper YAML_OBJECT_MAPPER = new ObjectMapper(new YAMLFactory());
  private static final String PRIVATE_KEY =
      "000000000000000000000000000000003ee2224386c82ffea477e2adf28a2929f5c349165a4196158c7f3a2ecca40f35";
  @TempDir Path configDir;

  @Test
  void metaDataInfoWithNonExistingFileReturnsEmpty() {
    final Optional<SigningMetadataFile> metadataInfo =
        signingMetadataFileProvider.getMetadataInfo(configDir.resolve("does_not_exist"));

    assertThat(metadataInfo).isEmpty();
  }

  @Test
  void metaDataInfoWithUnknownTypeReturnsEmpty() throws IOException {
    final Path filename = configDir.resolve("unknownType");
    final MetadataFileBody metadataFileBody = new MetadataFileBody(UNKNOWN_TYPE_SIGNER);
    YAML_OBJECT_MAPPER.writeValue(filename.toFile(), metadataFileBody);

    final Optional<SigningMetadataFile> metadataInfo =
        signingMetadataFileProvider.getMetadataInfo(filename);

    assertThat(metadataInfo).isEmpty();
  }

  @Test
  void metaDataInfoWithMissingTypeReturnsEmpty() throws IOException {
    final Path filename = configDir.resolve("empty");
    YAML_OBJECT_MAPPER.writeValue(filename.toFile(), Map.of());

    final Optional<SigningMetadataFile> metadataInfo =
        signingMetadataFileProvider.getMetadataInfo(filename);

    assertThat(metadataInfo).isEmpty();
  }

  @Test
  void unencryptedMetaDataInfoWithMissingPrivateKeyReturnsEmpty() throws IOException {
    final Path filename = configDir.resolve("unencryptedNoKey." + YAML_FILE_EXTENSION);
    final MetadataFileBody unencryptedKeyMetadataFile = new MetadataFileBody(FILE_RAW);
    YAML_OBJECT_MAPPER.writeValue(filename.toFile(), unencryptedKeyMetadataFile);

    final Optional<SigningMetadataFile> metadataInfo =
        signingMetadataFileProvider.getMetadataInfo(filename);

    assertThat(metadataInfo).isEmpty();
  }

  @Test
  void unencryptedMetaDataInfoWithPrivateKeyReturnsMetadata() throws IOException {
    final Path filename = configDir.resolve("unencryptedNoKey." + YAML_FILE_EXTENSION);
    final MetadataFileBody unencryptedKeyMetadataFile = new MetadataFileBody(FILE_RAW);
    unencryptedKeyMetadataFile.setParam("privateKey", PRIVATE_KEY);
    YAML_OBJECT_MAPPER.writeValue(filename.toFile(), unencryptedKeyMetadataFile);

    final Optional<SigningMetadataFile> metadataInfoResult =
        signingMetadataFileProvider.getMetadataInfo(filename);

    assertThat(metadataInfoResult).isNotEmpty();
    final SigningMetadataFile signingMetadataFileResult = metadataInfoResult.get();
    assertThat(signingMetadataFileResult).isInstanceOf(UnencryptedKeyMetadataFile.class);
    UnencryptedKeyMetadataFile unencryptedKeyMetadataResult =
        (UnencryptedKeyMetadataFile) signingMetadataFileResult;
    assertThat(unencryptedKeyMetadataResult.getPrivateKeyBytes())
        .isEqualTo(Bytes.fromHexString(PRIVATE_KEY));
    assertThat(unencryptedKeyMetadataResult.getBaseFilename()).isEqualTo("unencryptedNoKey");
  }
}
