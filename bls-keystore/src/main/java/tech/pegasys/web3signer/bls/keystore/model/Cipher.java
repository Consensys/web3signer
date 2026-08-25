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

public record Cipher(
    @JsonProperty(value = "function") CipherFunction function,
    @JsonProperty(value = "params") CipherParam params,
    @JsonProperty(value = "message") Bytes message) {

  /**
   * Compact constructor: validates all parameters eagerly for both Jackson deserialisation and
   * programmatic construction, producing clear {@link
   * tech.pegasys.web3signer.bls.keystore.KeyStoreValidationException} messages in both cases.
   */
  public Cipher {
    if (function == null) {
      throw new KeyStoreValidationException(
          "Invalid KeyStore: Missing 'crypto.cipher.function' property");
    }

    if (params == null) {
      throw new KeyStoreValidationException(
          "Invalid KeyStore: Missing 'crypto.cipher.params' property");
    }
    if (message == null) {
      throw new KeyStoreValidationException(
          "Invalid KeyStore: Missing 'crypto.cipher.message' property");
    }
  }
}
