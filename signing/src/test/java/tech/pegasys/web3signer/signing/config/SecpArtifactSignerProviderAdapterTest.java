/*
 * Copyright 2023 ConsenSys AG.
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
import static org.mockito.Mockito.when;
import static org.web3j.crypto.Keys.getAddress;
import static tech.pegasys.web3signer.signing.util.IdentifierUtils.normaliseIdentifier;

import tech.pegasys.web3signer.signing.ArtifactSigner;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SecpArtifactSignerProviderAdapterTest {

  final String PUBLIC_KEY =
      "09b02f8a5fddd222ade4ea4528faefc399623af3f736be3c44f03e2df22fb792f3931a4d9573d333ca74343305762a753388c3422a86d98b713fc91c1ea04842";

  @Mock ArtifactSigner mockArtifactSigner;

  @Mock DefaultArtifactSignerProvider defaultArtifactSignerProvider;

  @BeforeEach
  public void setUp() {
    when(defaultArtifactSignerProvider.availableIdentifiers()).thenReturn(Set.of(PUBLIC_KEY));
    when(defaultArtifactSignerProvider.getSigner(PUBLIC_KEY))
        .thenReturn(Optional.of(mockArtifactSigner));
  }

  @Test
  public void When_LoadIsCalled_Expect_SameNumberOfEntriesAsTheSignerProvided()
      throws ExecutionException, InterruptedException {

    SecpArtifactSignerProviderAdapter adapter =
        new SecpArtifactSignerProviderAdapter(defaultArtifactSignerProvider);

    adapter.load().get();

    // after calling load(), it should contain the same amount of entries as the default
    assertThat(adapter.availableIdentifiers().size()).isEqualTo(1);
  }

  @Test
  public void When_LoadIsCalled_Expect_SignerMappedToEth1Address()
      throws ExecutionException, InterruptedException {
    final String eth1Address = normaliseIdentifier(getAddress(PUBLIC_KEY));

    SecpArtifactSignerProviderAdapter adapter =
        new SecpArtifactSignerProviderAdapter(defaultArtifactSignerProvider);

    adapter.load().get();

    assertThat(adapter.getSigner(eth1Address).get()).isEqualTo(mockArtifactSigner);
  }
}
