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
import static tech.pegasys.web3signer.signing.secp256k1.multikey.MetadataFileFixture.CONFIG_FILE_EXTENSION;
import static tech.pegasys.web3signer.signing.secp256k1.multikey.MetadataFileFixture.KEY_FILE;
import static tech.pegasys.web3signer.signing.secp256k1.multikey.MetadataFileFixture.LOWERCASE_ADDRESS;
import static tech.pegasys.web3signer.signing.secp256k1.multikey.MetadataFileFixture.PASSWORD_FILE;
import static tech.pegasys.web3signer.signing.secp256k1.multikey.MetadataFileFixture.PREFIX_ADDRESS;
import static tech.pegasys.web3signer.signing.secp256k1.multikey.MetadataFileFixture.load;

import tech.pegasys.web3signer.signing.secp256k1.multikey.metadata.FileBasedSigningMetadataFile;

import org.junit.jupiter.api.Test;

class FileBasedSigningMetadataFileTest {

  @Test
  void matchingMetadataFileWithoutPrefixShouldHaveExpectedName() {
    final FileBasedSigningMetadataFile fileBasedSigningMetadataFile =
        load(LOWERCASE_ADDRESS + CONFIG_FILE_EXTENSION, KEY_FILE, PASSWORD_FILE);

    assertThat(fileBasedSigningMetadataFile.getFilename())
        .matches(LOWERCASE_ADDRESS + CONFIG_FILE_EXTENSION);
    assertThat(fileBasedSigningMetadataFile.getConfig().getKeystoreFile().toFile().toString())
        .matches(KEY_FILE);
    assertThat(
            fileBasedSigningMetadataFile.getConfig().getKeystorePasswordFile().toFile().toString())
        .matches(PASSWORD_FILE);
  }

  @Test
  void matchingMetadataFileWithPrefixShouldHaveExpectedName() {
    final String prefix = "bar_";
    final FileBasedSigningMetadataFile fileBasedSigningMetadataFile =
        load(prefix + PREFIX_ADDRESS + CONFIG_FILE_EXTENSION, KEY_FILE, PASSWORD_FILE);

    assertThat(fileBasedSigningMetadataFile.getFilename())
        .matches(prefix + PREFIX_ADDRESS + CONFIG_FILE_EXTENSION);
    assertThat(fileBasedSigningMetadataFile.getConfig().getKeystoreFile().toFile().toString())
        .matches(KEY_FILE);
    assertThat(
            fileBasedSigningMetadataFile.getConfig().getKeystorePasswordFile().toFile().toString())
        .matches(PASSWORD_FILE);
  }
}
