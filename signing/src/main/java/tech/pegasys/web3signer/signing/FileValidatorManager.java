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
import java.util.concurrent.ExecutionException;

public class FileValidatorManager implements ValidatorManager {

  private final ArtifactSignerProvider signerProvider;
  private final KeystoreFileManager keystoreFileManager;

  public FileValidatorManager(
      final ArtifactSignerProvider signerProvider, final KeystoreFileManager keystoreFileManager) {
    this.signerProvider = signerProvider;
    this.keystoreFileManager = keystoreFileManager;
  }

  @Override
  public void deleteValidator(final String publicKey)
      throws ExecutionException, InterruptedException, IOException {
    // Remove active key from memory first, will stop any further signing with this key
    signerProvider.removeSigner(publicKey).get();
    // Then, delete the corresponding keystore files
    keystoreFileManager.deleteKeystoreFiles(publicKey);
  }

  @Override
  public void addValidator() {}
}
