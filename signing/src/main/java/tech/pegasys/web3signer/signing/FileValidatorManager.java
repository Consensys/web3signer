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
package tech.pegasys.web3signer.signing;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.ExecutionException;

import org.apache.tuweni.bytes.Bytes;

public class FileValidatorManager implements ValidatorManager {

  private final ArtifactSignerProvider signerProvider;
  private final KeystoreFileManager keystoreFileManager;

  public FileValidatorManager(
      final ArtifactSignerProvider signerProvider, final KeystoreFileManager keystoreFileManager) {
    this.signerProvider = signerProvider;
    this.keystoreFileManager = keystoreFileManager;
  }

  @Override
  public void deleteValidator(final Bytes publicKey) {
    try {
      // Remove active key from memory first, will stop any further signing with this key
      signerProvider.removeSigner(publicKey.toHexString()).get();
      // Then, delete the corresponding keystore files
      keystoreFileManager.deleteKeystoreFiles(publicKey.toHexString());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Unable to delete validator", e);
    } catch (IOException | ExecutionException e) {
      throw new IllegalStateException("Unable to delete validator", e);
    }
  }

  @Override
  public void addValidator(
      final BlsArtifactSigner signer, final KeystoreFileRecord keystoreFileRecord) {
    try {
      // write keystore to file - allows to bail out in case of failure
      keystoreFileManager.createKeystoreFiles(keystoreFileRecord);
      // Then, add it in memory to make it available for signing ...
      signerProvider.addSigner(signer).get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Unable to add validator", e);
    } catch (ExecutionException e) {
      throw new IllegalStateException("Unable to add validator", e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
