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
package tech.pegasys.web3signer.core.service.http.handlers.keymanager.delete;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import tech.pegasys.web3signer.BLSTestUtil;
import tech.pegasys.web3signer.core.signing.ArtifactSigner;
import tech.pegasys.web3signer.core.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.slashingprotection.SlashingProtection;
import tech.pegasys.web3signer.slashingprotection.interchange.IncrementalExporter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteKeystoresProcessorTest {

  static final String PUBLIC_KEY1 = BLSTestUtil.randomKeyPair(1).getPublicKey().toString();
  static final String PUBLIC_KEY2 = BLSTestUtil.randomKeyPair(2).getPublicKey().toString();

  @Mock KeystoreFileManager keystoreFileManager;
  @Mock SlashingProtection slashingProtection;
  @Mock ArtifactSignerProvider artifactSignerProvider;
  @Mock ArtifactSigner signer;
  @Mock IncrementalExporter incrementalExporter;

  DeleteKeystoresProcessor processor;

  @BeforeEach
  void setup() {
    processor =
        new DeleteKeystoresProcessor(
            keystoreFileManager, Optional.of(slashingProtection), artifactSignerProvider);
    lenient()
        .when(slashingProtection.createIncrementalExporter(any()))
        .thenReturn(incrementalExporter);
  }

  @Test
  void testSuccess() throws IOException {
    when(artifactSignerProvider.getSigner(any())).thenReturn(Optional.of(signer));
    when(artifactSignerProvider.removeSigner(any()))
        .thenReturn(CompletableFuture.completedFuture(null));
    doNothing().when(keystoreFileManager).deleteKeystoreFiles(any());

    final DeleteKeystoresRequestBody requestBody =
        new DeleteKeystoresRequestBody(List.of(PUBLIC_KEY1));
    final DeleteKeystoresResponse response = processor.process(requestBody);
    assertThat(response.getData().size()).isEqualTo(1);
    assertThat(response.getData().get(0).getMessage()).isEqualTo("");
    assertThat(response.getData().get(0).getStatus()).isEqualTo(DeleteKeystoreStatus.DELETED);
  }

  @Test
  void testSignerNotFound() {
    when(artifactSignerProvider.getSigner(any())).thenReturn(Optional.empty());
    when(slashingProtection.isRegisteredValidator(any())).thenReturn(false);

    final DeleteKeystoresRequestBody requestBody =
        new DeleteKeystoresRequestBody(List.of(PUBLIC_KEY1));
    final DeleteKeystoresResponse response = processor.process(requestBody);
    assertThat(response.getData().size()).isEqualTo(1);
    assertThat(response.getData().get(0).getMessage()).isEqualTo("");
    assertThat(response.getData().get(0).getStatus()).isEqualTo(DeleteKeystoreStatus.NOT_FOUND);
  }

  @Test
  void testSignerNotActive() {
    when(artifactSignerProvider.getSigner(any())).thenReturn(Optional.empty());
    when(slashingProtection.isRegisteredValidator(any())).thenReturn(true);

    final DeleteKeystoresRequestBody requestBody =
        new DeleteKeystoresRequestBody(List.of(PUBLIC_KEY1));
    final DeleteKeystoresResponse response = processor.process(requestBody);
    assertThat(response.getData().size()).isEqualTo(1);
    assertThat(response.getData().get(0).getMessage()).isEqualTo("");
    assertThat(response.getData().get(0).getStatus()).isEqualTo(DeleteKeystoreStatus.NOT_ACTIVE);
  }

  @Test
  void testIOException() throws IOException {
    when(artifactSignerProvider.getSigner(any())).thenReturn(Optional.of(signer));
    when(artifactSignerProvider.removeSigner(any()))
        .thenReturn(CompletableFuture.completedFuture(null));
    doThrow(new IOException("io error")).when(keystoreFileManager).deleteKeystoreFiles(any());

    final DeleteKeystoresRequestBody requestBody =
        new DeleteKeystoresRequestBody(List.of(PUBLIC_KEY1));
    final DeleteKeystoresResponse response = processor.process(requestBody);
    assertThat(response.getData().size()).isEqualTo(1);
    assertThat(response.getData().get(0).getMessage())
        .isEqualTo("Error deleting keystore file: io error");
    assertThat(response.getData().get(0).getStatus()).isEqualTo(DeleteKeystoreStatus.ERROR);
  }

  @Test
  void testInterruptedException() {
    when(artifactSignerProvider.getSigner(any())).thenReturn(Optional.of(signer));
    when(artifactSignerProvider.removeSigner(any()))
        .thenReturn(CompletableFuture.failedFuture(new InterruptedException("interrupted")));

    final DeleteKeystoresRequestBody requestBody =
        new DeleteKeystoresRequestBody(List.of(PUBLIC_KEY1));
    final DeleteKeystoresResponse response = processor.process(requestBody);
    assertThat(response.getData().size()).isEqualTo(1);
    assertThat(response.getData().get(0).getMessage())
        .isEqualTo("Error deleting keystore file: java.lang.InterruptedException: interrupted");
    assertThat(response.getData().get(0).getStatus()).isEqualTo(DeleteKeystoreStatus.ERROR);
  }

  @Test
  void testSlashingExportFailsAllDeletesWhenIncrementalExportFinaliseFails() throws IOException {
    when(artifactSignerProvider.getSigner(any())).thenReturn(Optional.of(signer));
    when(artifactSignerProvider.removeSigner(any()))
        .thenReturn(CompletableFuture.completedFuture(null));
    doNothing().when(keystoreFileManager).deleteKeystoreFiles(any());
    doThrow(new IOException("db error")).when(incrementalExporter).finalise();

    final DeleteKeystoresRequestBody requestBody =
        new DeleteKeystoresRequestBody(List.of(PUBLIC_KEY1, PUBLIC_KEY2));
    final DeleteKeystoresResponse response = processor.process(requestBody);
    assertThat(response.getData().size()).isEqualTo(2);

    // assert that all results have an error
    for (final DeleteKeystoreResult result : response.getData()) {
      assertThat(result.getMessage()).isEqualTo("Error exporting slashing data: db error");
      assertThat(result.getStatus()).isEqualTo(DeleteKeystoreStatus.ERROR);
    }
  }

  @Test
  void testSlashingExportRethrowsExceptionWhenIncrementalExportCreateFails() {
    final RuntimeException exception = new RuntimeException("db error");
    doThrow(exception).when(slashingProtection).createIncrementalExporter(any());

    final DeleteKeystoresRequestBody requestBody =
        new DeleteKeystoresRequestBody(List.of(PUBLIC_KEY1));
    assertThatThrownBy(() -> processor.process(requestBody))
        .hasMessage("Error deleting keystores")
        .hasCause(exception);
  }

  @Test
  void slashingExportFailOnlyAffectsIndividualKeyStore() throws IOException {
    when(artifactSignerProvider.getSigner(any())).thenReturn(Optional.of(signer));
    when(artifactSignerProvider.removeSigner(any()))
        .thenReturn(CompletableFuture.completedFuture(null));
    doNothing().when(keystoreFileManager).deleteKeystoreFiles(any());
    doThrow(new RuntimeException("db error")).when(incrementalExporter).export(PUBLIC_KEY1);

    final DeleteKeystoresRequestBody requestBody =
        new DeleteKeystoresRequestBody(List.of(PUBLIC_KEY1, PUBLIC_KEY2));
    final DeleteKeystoresResponse response = processor.process(requestBody);

    final List<DeleteKeystoreResult> results = response.getData();
    assertThat(results.size()).isEqualTo(2);
    assertThat(results.get(0).getMessage()).isEqualTo("Error exporting slashing data: db error");
    assertThat(results.get(0).getStatus()).isEqualTo(DeleteKeystoreStatus.ERROR);
    assertThat(results.get(1).getMessage()).isEqualTo("");
    assertThat(results.get(1).getStatus()).isEqualTo(DeleteKeystoreStatus.DELETED);
  }
}
