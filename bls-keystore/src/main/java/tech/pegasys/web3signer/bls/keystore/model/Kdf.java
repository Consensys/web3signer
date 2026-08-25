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
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

public record Kdf(
    @JsonProperty(value = "function") KdfFunction kdfFunction,
    @JsonProperty(value = "params")
        @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
            property = "function")
        @JsonSubTypes({
          @JsonSubTypes.Type(value = SCryptParam.class, name = "scrypt"),
          @JsonSubTypes.Type(value = Pbkdf2Param.class, name = "pbkdf2")
        })
        KdfParam param,
    @JsonProperty(value = "message") String message) {

  /**
   * Compact Constructor: Automatically executes validation for BOTH Jackson deserialization and
   * manually invoked constructors.
   */
  public Kdf {
    if (kdfFunction == null) {
      throw new KeyStoreValidationException(
          "Invalid KeyStore: Missing 'crypto.kdf.function' property");
    }

    if (param == null) {
      throw new KeyStoreValidationException(
          "Invalid KeyStore: Missing 'crypto.kdf.params' property");
    }

    if (message == null) {
      throw new KeyStoreValidationException(
          "Invalid KeyStore: Missing 'crypto.kdf.message' property");
    }
  }

  /**
   * Convenience constructor for programmatic creation from a {@link KdfParam} instance. The {@code
   * function} discriminator is derived from the param itself, and {@code message} defaults to an
   * empty string.
   *
   * @param kdfParam kdf parameter
   */
  public Kdf(final KdfParam kdfParam) {
    this(kdfParam.kdfFunction(), kdfParam, "");
  }
}
