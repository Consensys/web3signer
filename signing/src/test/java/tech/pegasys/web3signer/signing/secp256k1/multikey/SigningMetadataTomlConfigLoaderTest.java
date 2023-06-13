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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static tech.pegasys.web3signer.signing.secp256k1.multikey.MetadataFileFixture.CONFIG_FILE_EXTENSION;
import static tech.pegasys.web3signer.signing.secp256k1.multikey.MetadataFileFixture.KEY_FILE;
import static tech.pegasys.web3signer.signing.secp256k1.multikey.MetadataFileFixture.KEY_FILE_2;
import static tech.pegasys.web3signer.signing.secp256k1.multikey.MetadataFileFixture.LOWERCASE_ADDRESS;
import static tech.pegasys.web3signer.signing.secp256k1.multikey.MetadataFileFixture.MISSING_KEY_AND_PASSWORD_PATH_ADDRESS;
import static tech.pegasys.web3signer.signing.secp256k1.multikey.MetadataFileFixture.MISSING_KEY_AND_PASSWORD_PATH_FILENAME;
import static tech.pegasys.web3signer.signing.secp256k1.multikey.MetadataFileFixture.MISSING_KEY_PATH_ADDRESS;
import static tech.pegasys.web3signer.signing.secp256k1.multikey.MetadataFileFixture.MISSING_KEY_PATH_FILENAME;
import static tech.pegasys.web3signer.signing.secp256k1.multikey.MetadataFileFixture.MISSING_PASSWORD_PATH_FILENAME;
import static tech.pegasys.web3signer.signing.secp256k1.multikey.MetadataFileFixture.PASSWORD_FILE;
import static tech.pegasys.web3signer.signing.secp256k1.multikey.MetadataFileFixture.PASSWORD_FILE_2;
import static tech.pegasys.web3signer.signing.secp256k1.multikey.MetadataFileFixture.PREFIX_ADDRESS;
import static tech.pegasys.web3signer.signing.secp256k1.multikey.MetadataFileFixture.PREFIX_LOWERCASE_DUPLICATE_ADDRESS;
import static tech.pegasys.web3signer.signing.secp256k1.multikey.MetadataFileFixture.PREFIX_LOWERCASE_DUPLICATE_FILENAME_1;
import static tech.pegasys.web3signer.signing.secp256k1.multikey.MetadataFileFixture.PREFIX_LOWERCASE_DUPLICATE_FILENAME_2;
import static tech.pegasys.web3signer.signing.secp256k1.multikey.MetadataFileFixture.SUFFIX_ADDRESS;
import static tech.pegasys.web3signer.signing.secp256k1.multikey.MetadataFileFixture.UNKNOWN_TYPE_SIGNER_FILENAME;
import static tech.pegasys.web3signer.signing.secp256k1.multikey.MetadataFileFixture.copyMetadataFileToDirectory;

import tech.pegasys.web3signer.signing.secp256k1.multikey.metadata.AzureSigningMetadataFile;
import tech.pegasys.web3signer.signing.secp256k1.multikey.metadata.FileBasedSigningMetadataFile;
import tech.pegasys.web3signer.signing.secp256k1.multikey.metadata.SigningMetadataFile;

import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SigningMetadataTomlConfigLoaderTest {

  @TempDir Path configsDirectory;

  private SigningMetadataTomlConfigLoader loader;

  @BeforeEach
  void beforeEach() {
    loader = new SigningMetadataTomlConfigLoader(configsDirectory);
  }

  @Test
  void singleValidFilesWhichMatchFilterAreLoaded() {
    final Filter<Path> filter =
        entry -> entry.toString().endsWith(LOWERCASE_ADDRESS + CONFIG_FILE_EXTENSION);
    final FileBasedSigningMetadataFile fileBasedSigningMetadataFile =
        copyMetadataFileToDirectory(
            configsDirectory, LOWERCASE_ADDRESS + CONFIG_FILE_EXTENSION, KEY_FILE, PASSWORD_FILE);

    final Optional<SigningMetadataFile> loadedMetadataFile = loader.loadMetadata(filter);

    assertThat(loadedMetadataFile).isNotEmpty();
    final FileBasedSigningMetadataFile fileBasedSigningMetadata =
        (FileBasedSigningMetadataFile) loadedMetadataFile.get();
    assertThat(fileBasedSigningMetadata.getConfig().getKeystoreFile())
        .isEqualTo(fileBasedSigningMetadataFile.getConfig().getKeystoreFile());

    assertThat(fileBasedSigningMetadata.getConfig().getKeystorePasswordFile())
        .isEqualTo(fileBasedSigningMetadataFile.getConfig().getKeystorePasswordFile());
  }

  @Test
  void loadMetadataFileWithUnknownTypeSignerFails() {
    final Filter<Path> filter = entry -> entry.toString().endsWith(UNKNOWN_TYPE_SIGNER_FILENAME);
    copyMetadataFileToDirectory(
        configsDirectory, UNKNOWN_TYPE_SIGNER_FILENAME, KEY_FILE, PASSWORD_FILE);
    final Optional<SigningMetadataFile> loadedMetadataFile = loader.loadMetadata(filter);

    assertThat(loadedMetadataFile).isEmpty();
  }

  @Test
  void loadMetadataFileWithMissingKeyPathIsEmpty() {
    final Filter<Path> filter = entry -> entry.toString().endsWith(MISSING_KEY_PATH_ADDRESS);
    copyMetadataFileToDirectory(
        configsDirectory, MISSING_KEY_PATH_FILENAME, KEY_FILE, PASSWORD_FILE);
    final Optional<SigningMetadataFile> loadedMetadataFile = loader.loadMetadata(filter);

    assertThat(loadedMetadataFile).isEmpty();
  }

  @Test
  void loadMetadataFileWithMissingPasswordPathIsEmpty() {
    final Filter<Path> filter = entry -> entry.toString().endsWith(MISSING_PASSWORD_PATH_FILENAME);
    copyMetadataFileToDirectory(
        configsDirectory, MISSING_PASSWORD_PATH_FILENAME, KEY_FILE, PASSWORD_FILE);
    final Optional<SigningMetadataFile> loadedMetadataFile = loader.loadMetadata(filter);

    assertThat(loadedMetadataFile).isEmpty();
  }

  @Test
  void loadMetadataFileWithMissingKeyAndPasswordPathIsEmpty() {
    final Filter<Path> filter =
        entry -> entry.toString().endsWith(MISSING_KEY_AND_PASSWORD_PATH_ADDRESS);
    copyMetadataFileToDirectory(
        configsDirectory, MISSING_KEY_AND_PASSWORD_PATH_FILENAME, KEY_FILE, PASSWORD_FILE);
    final Optional<SigningMetadataFile> loadedMetadataFile = loader.loadMetadata(filter);

    assertThat(loadedMetadataFile).isEmpty();
  }

  @Test
  void multipleMatchesForSameAddressReturnsEmpty() {
    final Filter<Path> filter =
        entry -> entry.toString().endsWith(PREFIX_LOWERCASE_DUPLICATE_ADDRESS);
    copyMetadataFileToDirectory(
        configsDirectory, PREFIX_LOWERCASE_DUPLICATE_FILENAME_2, KEY_FILE, PASSWORD_FILE);
    copyMetadataFileToDirectory(
        configsDirectory, PREFIX_LOWERCASE_DUPLICATE_FILENAME_1, KEY_FILE, PASSWORD_FILE);
    final Optional<SigningMetadataFile> loadedMetadataFile = loader.loadMetadata(filter);

    assertThat(loadedMetadataFile).isEmpty();
  }

  @Test
  void loadKeyPasswordNotEndingWithAddressReturnsEmpty() {
    final Filter<Path> filter = entry -> entry.toString().endsWith(SUFFIX_ADDRESS);
    final Optional<SigningMetadataFile> loadedMetadataFile = loader.loadMetadata(filter);

    assertThat(loadedMetadataFile).isEmpty();
  }

  @Test
  void loadAvailableConfigsReturnsAllValidMetadataFilesInDirectory() {
    final Filter<Path> filter = entry -> true;
    final FileBasedSigningMetadataFile metadataFile1 =
        copyMetadataFileToDirectory(
            configsDirectory, LOWERCASE_ADDRESS + CONFIG_FILE_EXTENSION, KEY_FILE, PASSWORD_FILE);
    final FileBasedSigningMetadataFile metadataFile2 =
        copyMetadataFileToDirectory(
            configsDirectory,
            "bar_" + PREFIX_ADDRESS + CONFIG_FILE_EXTENSION,
            KEY_FILE_2,
            PASSWORD_FILE_2);
    final FileBasedSigningMetadataFile metadataFile3 =
        copyMetadataFileToDirectory(
            configsDirectory, PREFIX_LOWERCASE_DUPLICATE_FILENAME_2, KEY_FILE_2, PASSWORD_FILE_2);
    final FileBasedSigningMetadataFile metadataFile4 =
        copyMetadataFileToDirectory(
            configsDirectory, PREFIX_LOWERCASE_DUPLICATE_FILENAME_1, KEY_FILE, PASSWORD_FILE);

    // duplicate files are loaded at this stage since addresses aren't checked until signers are
    // created
    final Collection<SigningMetadataFile> metadataFiles =
        loader.loadAvailableSigningMetadataTomlConfigs(filter);

    assertThat(metadataFiles).hasSize(4);
    assertThat(metadataFiles)
        .containsOnly(metadataFile1, metadataFile2, metadataFile3, metadataFile4);
  }

  private void copyFileIntoConfigDirectory(final String filename) {
    final Path newMetadataFile = configsDirectory.resolve(filename);

    try {
      Files.copy(
          Path.of(Resources.getResource("metadata-toml-configs").toURI()).resolve(filename),
          newMetadataFile);
    } catch (Exception e) {
      fail("Error copying metadata files", e);
    }
  }

  @Test
  void azureConfigIsLoadedIfAzureMetadataFileInDirectory() {
    copyFileIntoConfigDirectory("azureconfig.toml");

    final Collection<SigningMetadataFile> metadataFiles =
        loader.loadAvailableSigningMetadataTomlConfigs(entry -> true);

    assertThat(metadataFiles.size()).isOne();
    assertThat(metadataFiles.toArray()[0]).isInstanceOf(AzureSigningMetadataFile.class);
    final AzureSigningMetadataFile metadataFile =
        (AzureSigningMetadataFile) metadataFiles.toArray()[0];

    assertThat(metadataFile.getConfig().getClientId()).isEqualTo("theClientId");
    assertThat(metadataFile.getConfig().getClientSecret()).isEqualTo("theClientSecret");
    assertThat(metadataFile.getConfig().getKeyVaultName()).isEqualTo("testkey");
    assertThat(metadataFile.getConfig().getKeyName()).isEqualTo("TestKey");
    assertThat(metadataFile.getConfig().getKeyVersion())
        .isEqualTo("7c01fe58d68148bba5824ce418241092");
    assertThat(metadataFile.getConfig().getTenantId()).isEqualTo("theTenantId");
  }

  @Test
  void azureConfigWithIllegalValueTypeFailsToLoad() {
    copyFileIntoConfigDirectory("azureconfig_illegalValueType.toml");
    final Collection<SigningMetadataFile> metadataFiles =
        loader.loadAvailableSigningMetadataTomlConfigs(entry -> true);

    assertThat(metadataFiles.size()).isZero();
  }

  @Test
  void azureConfigWithMissingFieldFailsToLoad() {
    copyFileIntoConfigDirectory("azureconfig_missingField.toml");

    final Collection<SigningMetadataFile> metadataFiles =
        loader.loadAvailableSigningMetadataTomlConfigs(entry -> true);

    assertThat(metadataFiles.size()).isZero();
  }

  @Test
  void relativeKeyAndPasswordFilesAreResolveRelativeToLibraryRoot() {
    copyFileIntoConfigDirectory("key_password_relative_path.toml");

    final Collection<SigningMetadataFile> metadataFiles =
        loader.loadAvailableSigningMetadataTomlConfigs(entry -> true);

    assertThat(metadataFiles.size()).isOne();
    assertThat(metadataFiles.toArray()[0]).isInstanceOf(FileBasedSigningMetadataFile.class);
    final FileBasedSigningMetadataFile metadataFile =
        (FileBasedSigningMetadataFile) metadataFiles.toArray()[0];

    assertThat(metadataFile.getConfig().getKeystoreFile())
        .isEqualTo(configsDirectory.resolve("./path/to/k.key"));
    assertThat(metadataFile.getConfig().getKeystorePasswordFile())
        .isEqualTo(configsDirectory.resolve("./path/to/p.password"));
  }
}
