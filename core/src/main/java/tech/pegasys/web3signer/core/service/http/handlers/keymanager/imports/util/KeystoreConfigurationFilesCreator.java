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

import static com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS;

import tech.pegasys.web3signer.core.multikey.metadata.FileKeyStoreMetadata;
import tech.pegasys.web3signer.core.signing.KeyType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

public class KeystoreConfigurationFilesCreator {
  private static final String YAML_EXTENSION = ".yaml";
  private static final String JSON_EXTENSION = ".json";
  private static final String PASSWORD_EXTENSION = ".password";

  static final YAMLMapper YAML_MAPPER =
      YAMLMapper.builder()
          .addModule(new YamlPathModule())
          .enable(ACCEPT_CASE_INSENSITIVE_ENUMS)
          .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
          .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID)
          .build();

  private final Path parentDir;
  private final String fileNameWithoutExtension;
  private final String jsonData;

  public KeystoreConfigurationFilesCreator(
      final Path parentDir, final String fileNameWithoutExtension, final String jsonData) {
    this.parentDir = parentDir;
    this.fileNameWithoutExtension = fileNameWithoutExtension;
    this.jsonData = jsonData;
  }

  /**
   * Create Keystore metadata, json and password files
   *
   * @param password password char[]. All the values in the array will be reset after password is
   *     written.
   * @throws IOException In case file write operations fail
   */
  public void createFiles(final char[] password) throws IOException {
    final Path metadataYamlFile = parentDir.resolve(fileNameWithoutExtension + YAML_EXTENSION);
    final Path keystoreJsonFile = parentDir.resolve(fileNameWithoutExtension + JSON_EXTENSION);
    final Path keystorePasswordFile =
        parentDir.resolve(fileNameWithoutExtension + PASSWORD_EXTENSION);

    // create yaml file first (so that if it fails we haven't written password file before it)
    final FileKeyStoreMetadata data =
        new FileKeyStoreMetadata(keystoreJsonFile, keystorePasswordFile, KeyType.BLS);
    createYamlFile(metadataYamlFile, data);

    // keystore json file
    Files.writeString(keystoreJsonFile, jsonData, StandardCharsets.UTF_8);

    // password file
    Files.writeString(keystorePasswordFile, new String(password), StandardCharsets.UTF_8);

    // reset password array argument
    Arrays.fill(password, ' ');
  }

  private void createYamlFile(final Path filePath, final FileKeyStoreMetadata signingMetadata)
      throws IOException {
    final String yamlContent = YAML_MAPPER.writeValueAsString(signingMetadata);
    Files.writeString(filePath, yamlContent);
  }

  static class YamlPathModule extends SimpleModule {
    public YamlPathModule() {
      super("yamlpathmodule");

      // use custom serializer instead of Jackson default because that generates file:/// and we
      // don't want it
      addSerializer(
          Path.class,
          new JsonSerializer<>() {
            @Override
            public void serialize(Path path, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
              gen.writeString(path.toString());
            }
          });
    }
  }
}
