/*
 * Copyright 2025 ConsenSys AG.
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
package tech.pegasys.web3signer.signing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.web3signer.BLSTestUtil;
import tech.pegasys.web3signer.signing.config.metadata.SignerOrigin;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InMemoryValidatorManagerTest {
  private static final BLSKeyPair BLS_KEY_PAIR = BLSTestUtil.randomKeyPair(1);

  @Mock private ArtifactSignerProvider artifactSignerProvider;

  @Test
  @SuppressWarnings("unchecked")
  void addsValidatorToMemory() throws ExecutionException, InterruptedException {
    final Future<Void> futureAddSigner = Mockito.mock(Future.class);
    when(artifactSignerProvider.addSigner(any())).thenReturn(futureAddSigner);

    final BlsArtifactSigner signer =
        new BlsArtifactSigner(BLS_KEY_PAIR, SignerOrigin.FILE_KEYSTORE);

    final InMemoryValidatorManager inMemoryValidatorManager =
        new InMemoryValidatorManager(artifactSignerProvider);
    inMemoryValidatorManager.addValidator(signer, null);

    verify(artifactSignerProvider)
        .addSigner(
            argThat(
                s ->
                    s instanceof BlsArtifactSigner blsSigner
                        && s.getIdentifier().equals(BLS_KEY_PAIR.getPublicKey().toString())
                        && !blsSigner.isReadOnlyKey()));
    verify(futureAddSigner).get();
  }

  @Test
  @SuppressWarnings("unchecked")
  void deletesValidatorFromMemory() throws ExecutionException, InterruptedException {
    final Future<Void> futureDeleteSigner = Mockito.mock(Future.class);
    when(artifactSignerProvider.removeSigner(any())).thenReturn(futureDeleteSigner);

    final InMemoryValidatorManager inMemoryValidatorManager =
        new InMemoryValidatorManager(artifactSignerProvider);
    inMemoryValidatorManager.deleteValidator(BLS_KEY_PAIR.getPublicKey().toBytesCompressed());

    verify(artifactSignerProvider).removeSigner(eq(BLS_KEY_PAIR.getPublicKey().toString()));
    verify(futureDeleteSigner).get();
  }

  @Test
  @SuppressWarnings("unchecked")
  void addValidatorThrowsExceptionWhenFutureGetFails()
      throws ExecutionException, InterruptedException {
    final Future<Void> futureAddSigner = Mockito.mock(Future.class);
    when(artifactSignerProvider.addSigner(any())).thenReturn(futureAddSigner);
    when(futureAddSigner.get())
        .thenThrow(new ExecutionException(new RuntimeException("Test exception")));

    final BlsArtifactSigner signer =
        new BlsArtifactSigner(BLS_KEY_PAIR, SignerOrigin.FILE_KEYSTORE);
    final InMemoryValidatorManager inMemoryValidatorManager =
        new InMemoryValidatorManager(artifactSignerProvider);

    assertThatThrownBy(() -> inMemoryValidatorManager.addValidator(signer, null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Unable to add validator to memory");
  }

  @Test
  @SuppressWarnings("unchecked")
  void deleteValidatorThrowsExceptionWhenFutureGetFails()
      throws ExecutionException, InterruptedException {
    final Future<Void> futureDeleteSigner = Mockito.mock(Future.class);
    when(artifactSignerProvider.removeSigner(any())).thenReturn(futureDeleteSigner);
    when(futureDeleteSigner.get()).thenThrow(new InterruptedException("Test exception"));

    final InMemoryValidatorManager inMemoryValidatorManager =
        new InMemoryValidatorManager(artifactSignerProvider);

    assertThatThrownBy(
            () ->
                inMemoryValidatorManager.deleteValidator(
                    BLS_KEY_PAIR.getPublicKey().toBytesCompressed()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Unable to delete validator from memory");
  }
}
