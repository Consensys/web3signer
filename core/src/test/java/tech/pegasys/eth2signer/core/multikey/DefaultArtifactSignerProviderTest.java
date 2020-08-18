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

import tech.pegasys.eth2signer.core.signing.ArtifactSigner;
import tech.pegasys.eth2signer.core.signing.ArtifactSignerProvider;
import tech.pegasys.eth2signer.core.signing.BlsArtifactSigner;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class DefaultArtifactSignerProviderTest {

  private static final String PUBLIC_KEY1 =
      "989d34725a2bfc3f15105f3f5fc8741f436c25ee1ee4f948e425d6bcb8c56bce6e06c269635b7e985a7ffa639e2409bf";
  private static final String PUBLIC_KEY2 =
      "a99a76ed7796f7be22d5b7e85deeb7c5677e88e511e0b337618f8c4eb61349b4bf2d153f649f7b53359fe8b94a38e44c";

  @Test
  void signerReturnedWhenHasHexPrefix() {
    final BlsArtifactSigner mockSigner = mock(BlsArtifactSigner.class);
    when(mockSigner.getIdentifier()).thenReturn(PUBLIC_KEY1);

    final ArtifactSignerProvider signerProvider =
        DefaultArtifactSignerProvider.create(List.of(mockSigner));

    final Optional<ArtifactSigner> signer = signerProvider.getSigner("0x" + PUBLIC_KEY1);
    assertThat(signer).isNotEmpty();
    assertThat(signerProvider.availableIdentifiers()).containsOnly("0x" + PUBLIC_KEY1);
  }

  @Test
  void signerReturnedWhenHasUpperCaseHexPrefix() {
    final BlsArtifactSigner mockSigner = mock(BlsArtifactSigner.class);
    when(mockSigner.getIdentifier()).thenReturn(PUBLIC_KEY1);

    final ArtifactSignerProvider signerProvider =
        DefaultArtifactSignerProvider.create(List.of(mockSigner));

    final Optional<ArtifactSigner> signer = signerProvider.getSigner("0X" + PUBLIC_KEY1);
    assertThat(signer).isNotEmpty();
    assertThat(signerProvider.availableIdentifiers()).containsOnly("0x" + PUBLIC_KEY1);
  }

  @Test
  void signerReturnedWhenHasNoPrefixAndDifferentCase() {
    final BlsArtifactSigner mockSigner = mock(BlsArtifactSigner.class);
    when(mockSigner.getIdentifier()).thenReturn(PUBLIC_KEY1.toUpperCase());

    final ArtifactSignerProvider signerProvider =
        DefaultArtifactSignerProvider.create(List.of(mockSigner));

    final Optional<ArtifactSigner> signer = signerProvider.getSigner("0x" + PUBLIC_KEY1);
    assertThat(signer).isNotEmpty();
    assertThat(signerProvider.availableIdentifiers()).containsOnly("0x" + PUBLIC_KEY1);
  }

  @Test
  void signerProviderOnlyHasSingleEntryIfPassedInListHasMultipleMatchingSigners() {
    final BlsArtifactSigner mockSigner1 = mock(BlsArtifactSigner.class);
    when(mockSigner1.getIdentifier()).thenReturn(PUBLIC_KEY1.toUpperCase());
    final ArtifactSigner mockSigner2 = mock(ArtifactSigner.class);
    when(mockSigner2.getIdentifier()).thenReturn(PUBLIC_KEY1.toUpperCase());

    final ArtifactSignerProvider signerProvider =
        DefaultArtifactSignerProvider.create(List.of(mockSigner1, mockSigner2));

    assertThat(signerProvider.availableIdentifiers()).hasSize(1);
    assertThat(signerProvider.availableIdentifiers()).containsOnly("0x" + PUBLIC_KEY1);
  }

  @Test
  void signerProviderCanMapInTwoSigners() {
    final ArtifactSigner mockSigner1 = mock(ArtifactSigner.class);
    when(mockSigner1.getIdentifier()).thenReturn(PUBLIC_KEY1.toUpperCase());
    final ArtifactSigner mockSigner2 = mock(ArtifactSigner.class);
    when(mockSigner2.getIdentifier()).thenReturn(PUBLIC_KEY2.toUpperCase());

    final ArtifactSignerProvider signerProvider =
        DefaultArtifactSignerProvider.create(List.of(mockSigner1, mockSigner2));

    assertThat(signerProvider.availableIdentifiers()).hasSize(2);
    assertThat(signerProvider.availableIdentifiers())
        .containsOnly("0x" + PUBLIC_KEY1, "0x" + PUBLIC_KEY2);
  }
}
