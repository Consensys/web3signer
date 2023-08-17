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

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.teku.bls.keystore.KeyStore;
import tech.pegasys.teku.bls.keystore.KeyStoreValidationException;
import tech.pegasys.teku.bls.keystore.model.KeyStoreData;
import tech.pegasys.web3signer.signing.config.metadata.SignerOrigin;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

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
  public void addValidator(final Bytes publicKey, final String keystore, final String password) {
    try {
      // new keystore to import
      // 1. validate and decrypt the keystore
      final BlsArtifactSigner signer = decryptKeystoreAndCreateSigner(keystore, password);
      // 2. write keystore file to disk
      keystoreFileManager.createKeystoreFiles(signer.getIdentifier(), keystore, password);
      // 3. add the new signer to the provider to make it available for signing
      signerProvider.addSigner(signer).get();
    } catch (IOException
        | InterruptedException
        | ExecutionException
        | KeyStoreValidationException e) {
      throw new IllegalStateException("Unable to add validator", e);
    }
  }

  private BlsArtifactSigner decryptKeystoreAndCreateSigner(
      final String jsonKeystoreData, final String password)
      throws JsonProcessingException, KeyStoreValidationException {
    final KeyStoreData keyStoreData = objectMapper.readValue(jsonKeystoreData, KeyStoreData.class);
    final Bytes privateKey = KeyStore.decrypt(password, keyStoreData);
    final BLSKeyPair keyPair = new BLSKeyPair(BLSSecretKey.fromBytes(Bytes32.wrap(privateKey)));
    return new BlsArtifactSigner(
        keyPair, SignerOrigin.FILE_KEYSTORE, Optional.ofNullable(keyStoreData.getPath()));
  }
}
