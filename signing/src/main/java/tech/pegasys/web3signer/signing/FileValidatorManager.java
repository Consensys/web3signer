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

import tech.pegasys.web3signer.bls.keystore.KeyStoreValidationException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tuweni.bytes.Bytes;

public class FileValidatorManager implements ValidatorManager {

  private final ArtifactSignerProvider signerProvider;
  private final KeystoreFileManager keystoreFileManager;
  private final ObjectMapper objectMapper;

  public FileValidatorManager(
      final ArtifactSignerProvider signerProvider,
      final KeystoreFileManager keystoreFileManager,
      final ObjectMapper objectMapper) {
    this.signerProvider = signerProvider;
    this.keystoreFileManager = keystoreFileManager;
    this.objectMapper = objectMapper;
  }

  @Override
  public void deleteValidator(final Bytes publicKey) {
    try {
      // Remove active key from memory first, will stop any further signing with this key
      signerProvider.removeSigner(publicKey.toHexString()).get();
      // Then, delete the corresponding keystore files
      keystoreFileManager.deleteKeystoreFiles(publicKey.toHexString());
    } catch (IOException | InterruptedException | ExecutionException e) {
      throw new IllegalStateException("Unable to delete validator", e);
    }
  }

  @Override
  public void addValidator(final BlsArtifactSigner signer) {
    try {
      signerProvider.addSigner(signer).get();
    } catch (InterruptedException | ExecutionException e) {
      throw new IllegalStateException("Unable to add validator", e);
    }
  }

  @Override
  public void postAddValidator(
      final BlsArtifactSigner signer, final String jsonKeystoreData, final String password) {
    try {
      keystoreFileManager.createKeystoreFiles(signer.getIdentifier(), jsonKeystoreData, password);
    } catch (IOException | RuntimeException e) {
      throw new KeyStoreValidationException("Unable to create keystore file", e);
    }
  }

  @Override
  public ObjectMapper getJsonMapper() {
    return this.objectMapper;
  }
}
