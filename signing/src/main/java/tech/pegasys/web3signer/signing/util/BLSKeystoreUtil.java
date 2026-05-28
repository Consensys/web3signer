/*
 * Copyright 2026 ConsenSys AG.
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
package tech.pegasys.web3signer.signing.util;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.web3signer.bls.keystore.KeyStore;
import tech.pegasys.web3signer.bls.keystore.KeyStoreValidationException;
import tech.pegasys.web3signer.bls.keystore.model.KeyStoreData;
import tech.pegasys.web3signer.signing.BlsArtifactSigner;
import tech.pegasys.web3signer.signing.config.metadata.SignerOrigin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/// Utility class for decrypting keystores
public final class BLSKeystoreUtil {
  private BLSKeystoreUtil() {}

  public static BlsArtifactSigner decryptKeystore(
      final ObjectMapper jsonMapper, final String jsonKeystoreData, final String password) {
    final KeyStoreData keyStoreData = parseKeystoreJson(jsonMapper, jsonKeystoreData);

    final BLSKeyPair keyPair = KeyStore.decrypt(password, keyStoreData);

    return new BlsArtifactSigner(keyPair, SignerOrigin.FILE_KEYSTORE, keyStoreData.path());
  }

  private static KeyStoreData parseKeystoreJson(ObjectMapper jsonMapper, String jsonKeystoreData) {
    try {
      return jsonMapper.readValue(jsonKeystoreData, KeyStoreData.class);
    } catch (final JsonProcessingException e) {
      throw new KeyStoreValidationException("Failed to parse keystore JSON", e);
    }
  }
}
