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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static tech.pegasys.eth2signer.core.multikey.MetadataFileFixture.CONFIG_FILE_EXTENSION;
import static tech.pegasys.eth2signer.core.multikey.metadata.YamlSigningMetadataFileProvider.YAML_FILE_EXTENSION;

import tech.pegasys.eth2signer.core.multikey.metadata.SigningMetadataFile;
import tech.pegasys.eth2signer.core.multikey.metadata.UnencryptedKeyMetadataFile;
import tech.pegasys.eth2signer.core.signing.ArtifactSigner;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MultiKeyArtifactSignerProviderTest {

  private SigningMetadataConfigLoader loader = mock(SigningMetadataConfigLoader.class);
  private MultiKeyArtifactSignerProvider signerFactory = new MultiKeyArtifactSignerProvider(loader);
  private static final String PUBLIC_KEY =
      "989d34725a2bfc3f15105f3f5fc8741f436c25ee1ee4f948e425d6bcb8c56bce6e06c269635b7e985a7ffa639e2409bf";
  private static final String PRIVATE_KEY =
      "000000000000000000000000000000003ee2224386c82ffea477e2adf28a2929f5c349165a4196158c7f3a2ecca40f35";
  private SigningMetadataFile metadataFile;

  @BeforeEach
  void setup() {
    metadataFile =
        new UnencryptedKeyMetadataFile(
            PUBLIC_KEY + CONFIG_FILE_EXTENSION,
            YAML_FILE_EXTENSION,
            Bytes.fromHexString(PRIVATE_KEY));
  }

  @Test
  void getSignerForAvailableMetadataReturnsSigner() {
    when(loader.loadMetadataForAddress(PUBLIC_KEY)).thenReturn(Optional.of(metadataFile));

    final Optional<ArtifactSigner> signer = signerFactory.getSigner(PUBLIC_KEY);
    assertThat(signer).isNotEmpty();
    assertThat(signer.get().getIdentifier()).isEqualTo("0x" + PUBLIC_KEY);
  }

  @Test
  void getAddresses() {
    final Collection<SigningMetadataFile> files = Collections.singleton(metadataFile);
    when(loader.loadAvailableSigningMetadataConfigs()).thenReturn(files);
    assertThat(signerFactory.availableSigners()).containsExactly("0x" + PUBLIC_KEY);
  }

  @Test
  void signerIsLoadedSuccessfullyWhenAddressHasCaseMismatchToFilename() {
    final UnencryptedKeyMetadataFile capitalisedMetadata =
        new UnencryptedKeyMetadataFile(
            PUBLIC_KEY.toUpperCase() + CONFIG_FILE_EXTENSION,
            YAML_FILE_EXTENSION,
            Bytes.fromHexString(PRIVATE_KEY));

    final ArtifactSigner signer = signerFactory.createSigner(capitalisedMetadata);
    assertThat(signer).isNotNull();
    assertThat(capitalisedMetadata.getBaseFilename())
        .isNotEqualTo(signer.getIdentifier().substring(2));
    assertThat(signer.getIdentifier()).isEqualTo("0x" + PUBLIC_KEY);
  }
}
