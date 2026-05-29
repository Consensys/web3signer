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
package tech.pegasys.web3signer.bls.keystore.model;

import tech.pegasys.web3signer.bls.keystore.KeyStoreValidationException;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.tuweni.bytes.Bytes;

public record CipherParam(@JsonProperty(value = "iv") Bytes iv) {
  public CipherParam {
    if (iv == null) {
      throw new KeyStoreValidationException(
          "Invalid KeyStore: Missing 'crypto.cipher.params.iv' property");
    }

    // In case of CTR/SIC, the size of IV is between 8 bytes and 16 bytes
    if (iv.size() < 8 || iv.size() > 16) {
      throw new KeyStoreValidationException("iv size must be >= 8 and <= 16");
    }
  }
}
