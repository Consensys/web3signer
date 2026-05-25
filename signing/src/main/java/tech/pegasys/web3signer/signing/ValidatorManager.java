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
import tech.pegasys.web3signer.bls.keystore.KeyStore;
import tech.pegasys.web3signer.bls.keystore.KeyStoreValidationException;
import tech.pegasys.web3signer.bls.keystore.model.KeyStoreData;
import tech.pegasys.web3signer.signing.config.metadata.SignerOrigin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public interface ValidatorManager {
  ObjectMapper getJsonMapper();

  void deleteValidator(final Bytes publicKey);

  void addValidator(final BlsArtifactSigner signer);

  void postAddValidator(
      final BlsArtifactSigner signer, final String jsonKeystoreData, final String password);

  default BlsArtifactSigner decryptKeystore(final String jsonKeystoreData, final String password) {
    try {
      final KeyStoreData keyStoreData =
          getJsonMapper().readValue(jsonKeystoreData, KeyStoreData.class);
      final Bytes privateKey = KeyStore.decrypt(password, keyStoreData);
      final BLSKeyPair keyPair = new BLSKeyPair(BLSSecretKey.fromBytes(Bytes32.wrap(privateKey)));

      return new BlsArtifactSigner(keyPair, SignerOrigin.FILE_KEYSTORE);
    } catch (final JsonProcessingException e) {
      throw new KeyStoreValidationException("Failed to parse keystore JSON", e);
    }
  }
}
