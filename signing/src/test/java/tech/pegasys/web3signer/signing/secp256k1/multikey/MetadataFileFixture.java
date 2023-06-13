/*
 * Copyright 2019 ConsenSys AG.
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
package tech.pegasys.web3signer.signing.secp256k1.multikey;

import static org.junit.jupiter.api.Assertions.fail;

import tech.pegasys.web3signer.signing.secp256k1.filebased.FileSignerConfig;
import tech.pegasys.web3signer.signing.secp256k1.multikey.metadata.FileBasedSigningMetadataFile;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.io.Resources;

public class MetadataFileFixture {

  public static final String CONFIG_FILE_EXTENSION = ".toml";
  public static String LOWER_CASE_PUBLIC_KEY =
      "af80b90d25145da28c583359beb47b21796b2fe1a23c1511e443e7a64dfdb27d7434c380f0aa4c500e220aa1a9d068514b1ff4d5019e624e7ba1efe82b340a59";
  public static String LOWERCASE_ADDRESS = "627306090abab3a6e1400e9345bc60c78a8bef57";
  public static String PREFIX_ADDRESS = "fe3b557e8fb62b89f4916b721be55ceb828dbd73";
  public static String UNKNOWN_TYPE_SIGNER_ADDRESS = "fe3b557e8fb62b89f4916b721be55ceb828dbd75";
  public static String UNKNOWN_TYPE_SIGNER_FILENAME =
      "unknown_type_signer_" + UNKNOWN_TYPE_SIGNER_ADDRESS + CONFIG_FILE_EXTENSION;
  public static String MISSING_KEY_PATH_ADDRESS = "fe3b557e8fb62b89f4916b721be55ceb828dbd76";
  public static String MISSING_PASSWORD_PATH_ADDRESS = "fe3b557e8fb62b89f4916b721be55ceb828dbd77";
  public static String MISSING_KEY_AND_PASSWORD_PATH_ADDRESS =
      "fe3b557e8fb62b89f4916b721be55ceb828dbd78";
  public static String MISSING_KEY_PATH_FILENAME =
      "missing_key_file_" + MISSING_KEY_PATH_ADDRESS + CONFIG_FILE_EXTENSION;
  public static String MISSING_PASSWORD_PATH_FILENAME =
      "missing_password_file_" + MISSING_PASSWORD_PATH_ADDRESS + CONFIG_FILE_EXTENSION;
  public static String MISSING_KEY_AND_PASSWORD_PATH_FILENAME =
      "missing_key_and_password_file_"
          + MISSING_KEY_AND_PASSWORD_PATH_ADDRESS
          + CONFIG_FILE_EXTENSION;

  public static String SUFFIX_ADDRESS = "627306090abab3a6e1400e9345bc60c78a8bef60";
  public static String PREFIX_LOWERCASE_DUPLICATE_ADDRESS =
      "627306090abab3a6e1400e9345bc60c78a8bef61";
  public static String PREFIX_LOWERCASE_DUPLICATE_FILENAME_1 =
      "duplicate_foo_" + PREFIX_LOWERCASE_DUPLICATE_ADDRESS + CONFIG_FILE_EXTENSION;
  public static String PREFIX_LOWERCASE_DUPLICATE_FILENAME_2 =
      "duplicate_bar_" + PREFIX_LOWERCASE_DUPLICATE_ADDRESS + CONFIG_FILE_EXTENSION;

  public static String KEY_FILE = "/path/to/k.key";
  public static String PASSWORD_FILE = "/path/to/p.password";
  public static String KEY_FILE_2 = "/path/to/k2.key";
  public static String PASSWORD_FILE_2 = "/path/to/p2.password";

  private static final URL metadataTomlConfigPathUrl =
      Resources.getResource("metadata-toml-configs");

  static FileBasedSigningMetadataFile load(
      final String metadataFilename, final String keyFilename, final String passwordFilename) {
    try {
      final Path metadataTomlConfigsDirectory = Path.of(metadataTomlConfigPathUrl.toURI());
      final Path metadataPath = metadataTomlConfigsDirectory.resolve(metadataFilename);
      if (!metadataPath.toFile().exists()) {
        fail("Missing metadata TOML file " + metadataPath.getFileName().toString());
        return null;
      }

      return new FileBasedSigningMetadataFile(
          metadataPath.getFileName().toString(),
          new FileSignerConfig(
              new File(keyFilename).toPath(), new File(passwordFilename).toPath()));

    } catch (URISyntaxException e) {
      fail("URI Syntax Exception" + metadataFilename);
      return null;
    }
  }

  static FileBasedSigningMetadataFile copyMetadataFileToDirectory(
      final Path configsDirectory,
      final String metadataFilename,
      final String keyFilename,
      final String passwordFilename) {

    final FileBasedSigningMetadataFile metadataFile =
        load(metadataFilename, keyFilename, passwordFilename);
    final Path newMetadataFile = configsDirectory.resolve(metadataFilename);

    try {
      Files.copy(
          Path.of(Resources.getResource("metadata-toml-configs").toURI()).resolve(metadataFilename),
          newMetadataFile);
    } catch (Exception e) {
      fail("Error copying metadata files", e);
    }

    return metadataFile;
  }
}
