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
package tech.pegasys.web3signer.signing.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.ArtifactSignerProvider;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DefaultArtifactSignerProviderTest {

  private static final String PUBLIC_KEY1 =
      "989d34725a2bfc3f15105f3f5fc8741f436c25ee1ee4f948e425d6bcb8c56bce6e06c269635b7e985a7ffa639e2409bf";
  private static final String PUBLIC_KEY2 =
      "a99a76ed7796f7be22d5b7e85deeb7c5677e88e511e0b337618f8c4eb61349b4bf2d153f649f7b53359fe8b94a38e44c";

  private ArtifactSignerProvider signerProvider;

  @AfterEach
  void cleanup() {
    if (signerProvider != null) {
      signerProvider.close();
    }
  }

  @Test
  void signerReturnedForMatchingIdentifier() {
    final ArtifactSigner mockSigner = mock(ArtifactSigner.class);
    when(mockSigner.getIdentifier()).thenReturn(PUBLIC_KEY1);

    signerProvider = new DefaultArtifactSignerProvider(() -> List.of(mockSigner));
    assertThatCode(() -> signerProvider.load().get()).doesNotThrowAnyException();

    final Optional<ArtifactSigner> signer = signerProvider.getSigner(PUBLIC_KEY1);
    assertThat(signer).isNotEmpty();
    assertThat(signerProvider.availableIdentifiers()).containsOnly(PUBLIC_KEY1);
  }

  @Test
  void signerProviderOnlyHasSingleEntryIfPassedInListHasMultipleMatchingSigners() {
    final ArtifactSigner mockSigner1 = mock(ArtifactSigner.class);
    when(mockSigner1.getIdentifier()).thenReturn(PUBLIC_KEY1);
    final ArtifactSigner mockSigner2 = mock(ArtifactSigner.class);
    when(mockSigner2.getIdentifier()).thenReturn(PUBLIC_KEY1);

    signerProvider = new DefaultArtifactSignerProvider(() -> List.of(mockSigner1, mockSigner2));
    assertThatCode(() -> signerProvider.load().get()).doesNotThrowAnyException();

    assertThat(signerProvider.availableIdentifiers()).hasSize(1);
    assertThat(signerProvider.availableIdentifiers()).containsOnly(PUBLIC_KEY1);
  }

  @Test
  void signerProviderCanMapInTwoSigners() {
    final ArtifactSigner mockSigner1 = mock(ArtifactSigner.class);
    when(mockSigner1.getIdentifier()).thenReturn(PUBLIC_KEY1);
    final ArtifactSigner mockSigner2 = mock(ArtifactSigner.class);
    when(mockSigner2.getIdentifier()).thenReturn(PUBLIC_KEY2);

    signerProvider = new DefaultArtifactSignerProvider(() -> List.of(mockSigner1, mockSigner2));
    assertThatCode(() -> signerProvider.load().get()).doesNotThrowAnyException();
    assertThat(signerProvider.availableIdentifiers()).hasSize(2);
    assertThat(signerProvider.availableIdentifiers()).containsOnly(PUBLIC_KEY1, PUBLIC_KEY2);
  }
}
