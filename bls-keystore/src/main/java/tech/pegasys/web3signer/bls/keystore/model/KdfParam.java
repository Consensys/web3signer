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

import tech.pegasys.web3signer.bls.keystore.PasswordUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.tuweni.bytes.Bytes;

/**
 * Sealed interface representing the parameter set for a Key Derivation Function used in an EIP-2335
 * BLS keystore. Only {@link SCryptParam} and {@link Pbkdf2Param} are permitted implementations,
 * reflecting the two KDF functions defined by the spec.
 *
 * <p>Validation is enforced eagerly in each implementation's compact record constructor,
 * eliminating the need for a separate {@code validate()} call chain.
 */
public sealed interface KdfParam permits SCryptParam, Pbkdf2Param {

  /** The desired length (in bytes) of the derived key. Must be &gt;= 32 per EIP-2335. */
  int dklen();

  /** The KDF salt. */
  Bytes salt();

  /**
   * Returns the KDF function identifier for this parameter set.
   *
   * <p>Excluded from JSON serialisation: the discriminator is already carried by the {@code
   * function} property on the enclosing {@link Kdf} object, so emitting it again here would produce
   * a duplicate field.
   */
  @JsonIgnore
  KdfFunction kdfFunction();

  /**
   * Derives the decryption key from a raw UTF-8 password string.
   *
   * <p>The string is normalised (NFKD decomposition + control-code stripping) per EIP-2335 before
   * being passed to the underlying KDF.
   */
  default Bytes generateDecryptionKey(final String password) {
    return generateDecryptionKey(PasswordUtils.normalizePassword(password));
  }

  /**
   * Derives the decryption key from a pre-normalised password byte sequence.
   *
   * @param password normalised password bytes produced by {@link PasswordUtils#normalizePassword}
   * @return the derived key of length {@link #dklen()} bytes
   */
  Bytes generateDecryptionKey(Bytes password);
}
