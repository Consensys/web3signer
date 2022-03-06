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
package tech.pegasys.web3signer.core.service.http.handlers.keymanager.imports.util;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.web3signer.core.service.http.handlers.keymanager.imports.util.KeystoreConfigurationFilesCreator.YAML_MAPPER;

import tech.pegasys.web3signer.core.multikey.metadata.FileKeyStoreMetadata;
import tech.pegasys.web3signer.core.multikey.metadata.SigningMetadata;

import java.nio.file.Path;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KeystoreConfigurationFilesCreatorTest {

  @Test
  void configurationFilesAreCreated(@TempDir Path parentDir) throws Exception {
    new KeystoreConfigurationFilesCreator(parentDir, "filename", "{\"test\":true}")
        .createFiles("password".toCharArray());

    final Path metadataYamlFile = parentDir.resolve("filename.yaml");
    final Path keystoreJsonFile = parentDir.resolve("filename.json");
    final Path keystorePasswordFile = parentDir.resolve("filename.password");

    assertThat(metadataYamlFile).exists();
    assertThat(keystoreJsonFile).exists();
    assertThat(keystorePasswordFile).exists();
  }

  @Test
  void yamlContentIsValidFileKeyStoreMetadata(@TempDir Path parentDir) throws Exception {
    new KeystoreConfigurationFilesCreator(parentDir, "filename", "{\"test\":true}")
        .createFiles("password".toCharArray());

    final Path metadataYamlFile = parentDir.resolve("filename.yaml");

    assertThat(metadataYamlFile).exists();

    SigningMetadata signingMetadata =
        YAML_MAPPER.readValue(metadataYamlFile.toFile(), SigningMetadata.class);
    assertThat(signingMetadata).isExactlyInstanceOf(FileKeyStoreMetadata.class);
  }

  @Test
  void yamlContentIsNotConverted(@TempDir Path parentDir) throws Exception {
    new KeystoreConfigurationFilesCreator(parentDir, "filename", "{\"test\":true}")
        .createFiles("password".toCharArray());

    final Path metadataYamlFile = parentDir.resolve("filename.yaml");
    final Path keystoreJsonFile = parentDir.resolve("filename.json");
    final Path keystorePasswordFile = parentDir.resolve("filename.password");

    // read raw values from Yaml
    Map<String, String> deserializedYamlMap =
        YAML_MAPPER.readValue(metadataYamlFile.toFile(), new TypeReference<>() {});

    assertThat(deserializedYamlMap.get("type")).isEqualTo("file-keystore");
    assertThat(deserializedYamlMap.get("keystoreFile")).isEqualTo(keystoreJsonFile.toString());
    assertThat(deserializedYamlMap.get("keystorePasswordFile"))
        .isEqualTo(keystorePasswordFile.toString());
    assertThat(deserializedYamlMap.get("keyType")).isEqualTo("BLS");
  }

  @Test
  void passwordContentsAreWritten(@TempDir Path parentDir) throws Exception {
    new KeystoreConfigurationFilesCreator(parentDir, "filename", "{\"test\":true}")
        .createFiles("password".toCharArray());

    final Path keystorePasswordFile = parentDir.resolve("filename.password");

    assertThat(keystorePasswordFile).hasContent("password");
  }

  @Test
  void jsonDataIsWritten(@TempDir Path parentDir) throws Exception {
    new KeystoreConfigurationFilesCreator(parentDir, "filename", "{\"test\":true}")
        .createFiles("password".toCharArray());

    final Path keystoreJsonFile = parentDir.resolve("filename.json");

    assertThat(keystoreJsonFile).hasContent("{\"test\":true}");
  }
}
