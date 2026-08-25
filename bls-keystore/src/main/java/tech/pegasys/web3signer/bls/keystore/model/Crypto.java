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

public record Crypto(
    @JsonProperty(value = "kdf") Kdf kdf,
    @JsonProperty(value = "checksum") Checksum checksum,
    @JsonProperty(value = "cipher") Cipher cipher) {

  /**
   * Compact Constructor: Automatically executes validation for BOTH Jackson deserialization and
   * manually invoked constructors.
   */
  public Crypto {
    if (kdf == null) {
      throw new KeyStoreValidationException("Invalid KeyStore: Missing 'crypto.kdf' property");
    }

    if (checksum == null) {
      throw new KeyStoreValidationException("Invalid KeyStore: Missing 'crypto.checksum' property");
    }

    if (cipher == null) {
      throw new KeyStoreValidationException("Invalid KeyStore: Missing 'crypto.cipher' property");
    }
  }
}
