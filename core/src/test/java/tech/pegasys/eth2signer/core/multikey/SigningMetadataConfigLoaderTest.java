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
package tech.pegasys.eth2signer.core.multikey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static tech.pegasys.eth2signer.core.multikey.MetadataFileFixture.CONFIG_FILE_EXTENSION;
import static tech.pegasys.eth2signer.core.multikey.MetadataFileFixture.LOWERCASE_ADDRESS;
import static tech.pegasys.eth2signer.core.multikey.MetadataFileFixture.PREFIX_LOWERCASE_DUPLICATE_ADDRESS;
import static tech.pegasys.eth2signer.core.multikey.MetadataFileFixture.PREFIX_LOWERCASE_DUPLICATE_FILENAME_1;
import static tech.pegasys.eth2signer.core.multikey.MetadataFileFixture.PREFIX_LOWERCASE_DUPLICATE_FILENAME_2;
import static tech.pegasys.eth2signer.core.multikey.MetadataFileFixture.PREFIX_MIXEDCASE_ADDRESS;
import static tech.pegasys.eth2signer.core.multikey.MetadataFileFixture.PREFIX_MIXEDCASE_FILENAME;
import static tech.pegasys.eth2signer.core.multikey.MetadataFileFixture.SUFFIX_ADDRESS;
import static tech.pegasys.eth2signer.core.multikey.MetadataFileFixture.copyMetadataFileToDirectory;
import static tech.pegasys.eth2signer.core.multikey.metadata.YamlSigningMetadataFileProvider.YAML_FILE_EXTENSION;

import tech.pegasys.eth2signer.core.multikey.metadata.SigningMetadataFile;
import tech.pegasys.eth2signer.core.multikey.metadata.SigningMetadataFileProvider;
import tech.pegasys.eth2signer.core.multikey.metadata.UnencryptedKeyMetadataFile;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SigningMetadataConfigLoaderTest {

  @TempDir Path configsDirectory;

  @Mock private SigningMetadataFileProvider signingMetadataFileProvider;

  private SigningMetadataConfigLoader loader;

  @BeforeEach
  void beforeEach() {
    loader =
        new SigningMetadataConfigLoader(
            configsDirectory, YAML_FILE_EXTENSION, signingMetadataFileProvider);
  }

  @Test
  void loadMetadataFileMatchingAddress() {
    final String metadataFilename = LOWERCASE_ADDRESS + CONFIG_FILE_EXTENSION;
    copyMetadataFileToDirectory(configsDirectory, metadataFilename);
    when(signingMetadataFileProvider.getMetadataInfo(any()))
        .thenReturn(Optional.of(unencryptedMetadata(metadataFilename)));

    final Optional<SigningMetadataFile> loadedMetadataFile =
        loader.loadMetadataForAddress(LOWERCASE_ADDRESS);

    assertThat(loadedMetadataFile).isNotEmpty();
    verify(signingMetadataFileProvider).getMetadataInfo(pathEndsWith(metadataFilename));
  }

  @Test
  void loadMetadataFileWithMixedCaseFilename() {
    copyMetadataFileToDirectory(configsDirectory, PREFIX_MIXEDCASE_FILENAME);
    when(signingMetadataFileProvider.getMetadataInfo(any()))
        .thenReturn(Optional.of(unencryptedMetadata(PREFIX_MIXEDCASE_FILENAME)));

    final Optional<SigningMetadataFile> loadedMetadataFile =
        loader.loadMetadataForAddress(PREFIX_MIXEDCASE_ADDRESS);

    assertThat(loadedMetadataFile).isNotEmpty();
    verify(signingMetadataFileProvider).getMetadataInfo(pathEndsWith(PREFIX_MIXEDCASE_FILENAME));
  }

  @Test
  void loadMetadataFileWithHexPrefixReturnsFile() {
    final String metadataFilename = LOWERCASE_ADDRESS + CONFIG_FILE_EXTENSION;
    copyMetadataFileToDirectory(configsDirectory, metadataFilename);
    when(signingMetadataFileProvider.getMetadataInfo(any()))
        .thenReturn(Optional.of(unencryptedMetadata(metadataFilename)));

    final Optional<SigningMetadataFile> loadedMetadataFile =
        loader.loadMetadataForAddress("0x" + LOWERCASE_ADDRESS);

    assertThat(loadedMetadataFile).isNotEmpty();
    verify(signingMetadataFileProvider).getMetadataInfo(pathEndsWith(metadataFilename));
  }

  @Test
  void multipleMatchesForSameAddressReturnsEmpty() {
    copyMetadataFileToDirectory(configsDirectory, PREFIX_LOWERCASE_DUPLICATE_FILENAME_2);
    copyMetadataFileToDirectory(configsDirectory, PREFIX_LOWERCASE_DUPLICATE_FILENAME_1);

    when(signingMetadataFileProvider.getMetadataInfo(
            pathEndsWith(PREFIX_LOWERCASE_DUPLICATE_FILENAME_1)))
        .thenReturn(Optional.of(unencryptedMetadata(PREFIX_LOWERCASE_DUPLICATE_FILENAME_1)));

    when(signingMetadataFileProvider.getMetadataInfo(
            pathEndsWith(PREFIX_LOWERCASE_DUPLICATE_FILENAME_2)))
        .thenReturn(Optional.of(unencryptedMetadata(PREFIX_LOWERCASE_DUPLICATE_FILENAME_2)));

    final Optional<SigningMetadataFile> loadedMetadataFile =
        loader.loadMetadataForAddress(PREFIX_LOWERCASE_DUPLICATE_ADDRESS);

    assertThat(loadedMetadataFile).isEmpty();
  }

  @Test
  void loadKeyPasswordNotEndingWithAddressReturnsEmpty() {
    final Optional<SigningMetadataFile> loadedMetadataFile =
        loader.loadMetadataForAddress(SUFFIX_ADDRESS);

    assertThat(loadedMetadataFile).isEmpty();
  }

  @Test
  void loadAvailableConfigsReturnsAllValidMetadataFilesInDirectory() {
    copyMetadataFileToDirectory(configsDirectory, LOWERCASE_ADDRESS + CONFIG_FILE_EXTENSION);
    final SigningMetadataFile metadataFile1 =
        unencryptedMetadata(LOWERCASE_ADDRESS + CONFIG_FILE_EXTENSION);
    when(signingMetadataFileProvider.getMetadataInfo(
            pathEndsWith(LOWERCASE_ADDRESS + CONFIG_FILE_EXTENSION)))
        .thenReturn(Optional.of(metadataFile1));

    copyMetadataFileToDirectory(configsDirectory, PREFIX_MIXEDCASE_FILENAME);
    final SigningMetadataFile metadataFile2 = unencryptedMetadata(PREFIX_MIXEDCASE_FILENAME);
    when(signingMetadataFileProvider.getMetadataInfo(pathEndsWith(PREFIX_MIXEDCASE_FILENAME)))
        .thenReturn(Optional.of(metadataFile2));

    copyMetadataFileToDirectory(configsDirectory, PREFIX_LOWERCASE_DUPLICATE_FILENAME_2);
    final SigningMetadataFile metadataFile3 =
        unencryptedMetadata(PREFIX_LOWERCASE_DUPLICATE_FILENAME_2);
    when(signingMetadataFileProvider.getMetadataInfo(
            pathEndsWith(PREFIX_LOWERCASE_DUPLICATE_FILENAME_2)))
        .thenReturn(Optional.of(metadataFile3));

    copyMetadataFileToDirectory(configsDirectory, PREFIX_LOWERCASE_DUPLICATE_FILENAME_1);
    final SigningMetadataFile metadataFile4 =
        unencryptedMetadata(PREFIX_LOWERCASE_DUPLICATE_FILENAME_1);
    when(signingMetadataFileProvider.getMetadataInfo(
            pathEndsWith(PREFIX_LOWERCASE_DUPLICATE_FILENAME_1)))
        .thenReturn(Optional.of(metadataFile4));

    // duplicate files are loaded at this stage since addresses aren't checked until signers are
    // created
    final Collection<SigningMetadataFile> metadataFiles =
        loader.loadAvailableSigningMetadataConfigs();

    assertThat(metadataFiles).hasSize(4);
    assertThat(metadataFiles)
        .containsOnly(metadataFile1, metadataFile2, metadataFile3, metadataFile4);
  }

  private Path pathEndsWith(final String endsWith) {
    return argThat((Path path) -> path != null && path.endsWith(endsWith));
  }

  private SigningMetadataFile unencryptedMetadata(final String filename) {
    return new UnencryptedKeyMetadataFile(filename, YAML_FILE_EXTENSION, Bytes.EMPTY);
  }
}
