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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.tuweni.bytes.Bytes;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.util.DigestFactory;

/**
 * PBKDF2 Key Derivation Function parameters (EIP-2335).
 *
 * <p>This is an immutable record. All parameter validation — including the common {@code dklen} and
 * {@code salt} checks that previously lived in the abstract {@code KdfParam} base class - is
 * performed eagerly in the compact constructor. Instances are therefore always valid; no separate
 * {@code validate()} call is needed.
 *
 * @param dklen desired key length in bytes; must be &gt;= 32
 * @param c iteration count; must be &gt;= 1
 * @param prf pseudo-random function (hash digest) to use; must not be null
 * @param salt KDF salt; must not be null
 */
public record Pbkdf2Param(
    @JsonProperty("dklen") int dklen,
    @JsonProperty("c") int c,
    @JsonProperty("prf") Pbkdf2PseudoRandomFunction prf,
    @JsonProperty("salt") Bytes salt)
    implements KdfParam {

  /**
   * Compact constructor: validates all parameters eagerly for both Jackson deserialisation and
   * programmatic construction, producing clear {@link KeyStoreValidationException} messages in both
   * cases.
   *
   * <p>Absent JSON fields for primitives default to {@code 0} when {@code required} is not set; the
   * range checks below catch those cases with the same clear messages.
   */
  public Pbkdf2Param {
    if (salt == null) {
      throw new KeyStoreValidationException(
          "Invalid KeyStore: Missing 'crypto.kdf.params.salt' property");
    }
    if (prf == null) {
      throw new KeyStoreValidationException(
          "Invalid KeyStore: Missing 'crypto.kdf.params.prf' property");
    }

    if (dklen < 32) {
      throw new KeyStoreValidationException("Generated key length parameter dklen must be >= 32.");
    }

    if (c < 1) {
      throw new KeyStoreValidationException("Iteration Count parameter c must be >= 1");
    }
  }

  @Override
  @JsonIgnore
  public KdfFunction kdfFunction() {
    return KdfFunction.PBKDF2;
  }

  @Override
  public Bytes generateDecryptionKey(final Bytes password) {
    if (password == null) {
      throw new KeyStoreValidationException("Password cannot be null");
    }
    final PKCS5S2ParametersGenerator gen =
        new PKCS5S2ParametersGenerator(DigestFactory.createSHA256());
    gen.init(password.toArrayUnsafe(), salt().toArrayUnsafe(), c());
    final byte[] key = ((KeyParameter) gen.generateDerivedParameters(dklen() * 8)).getKey();
    return Bytes.wrap(key);
  }
}
