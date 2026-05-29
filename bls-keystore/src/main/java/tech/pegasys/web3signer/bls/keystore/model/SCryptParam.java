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
import org.bouncycastle.crypto.generators.SCrypt;

/**
 * SCrypt Key Derivation Function parameters (EIP-2335).
 *
 * <p>This is an immutable record. All parameter validation — including the common {@code dklen} and
 * {@code salt} checks that previously lived in the abstract {@code KdfParam} base class — is
 * performed eagerly in the compact constructor. Instances are therefore always valid; no separate
 * {@code validate()} call is needed.
 *
 * @param dklen desired key length in bytes; must be &gt;= 32
 * @param n CPU/memory cost parameter; must be &gt; 1, a power of 2, and &lt; 2^(128*r/8)
 * @param p parallelisation parameter; must be &gt;= 1 and &lt;= {@link Integer#MAX_VALUE} / (128 *
 *     r * 8)
 * @param r block size; must be &gt;= 1
 * @param salt KDF salt; must not be null
 */
public record SCryptParam(
    @JsonProperty("dklen") int dklen,
    @JsonProperty("n") int n,
    @JsonProperty("p") int p,
    @JsonProperty("r") int r,
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
  public SCryptParam {
    if (salt == null) {
      throw new KeyStoreValidationException(
          "Invalid KeyStore: Missing 'crypto.kdf.params.salt' property");
    }

    if (dklen < 32) {
      throw new KeyStoreValidationException("Generated key length parameter dklen must be >= 32.");
    }

    if (n <= 1 || !isPowerOf2(n)) {
      throw new KeyStoreValidationException("Cost parameter n must be > 1 and a power of 2");
    }
    // When r == 1 the internal cost expression (128 * r * N) overflows an int for N >= 65536.
    if (r == 1 && n >= 65536) {
      throw new KeyStoreValidationException("Cost parameter n must be > 1 and < 65536");
    }
    if (r < 1) {
      throw new KeyStoreValidationException("Block size r must be >= 1");
    }
    final int maxParallel = Integer.MAX_VALUE / (128 * r * 8);
    if (p < 1 || p > maxParallel) {
      throw new KeyStoreValidationException(
          String.format(
              "Parallelization parameter p must be >= 1 and <= %d (based on block size r of %d)",
              maxParallel, r));
    }
  }

  /**
   * Convenience constructor using the EIP-2335 recommended defaults: N = 2^18 (262 144), r = 8, p =
   * 1.
   *
   * @param dklen desired key length in bytes
   * @param salt KDF salt
   */
  public SCryptParam(final int dklen, final Bytes salt) {
    this(dklen, 262_144, 1, 8, salt);
  }

  @Override
  @JsonIgnore
  public KdfFunction kdfFunction() {
    return KdfFunction.SCRYPT;
  }

  @Override
  public Bytes generateDecryptionKey(final Bytes password) {
    if (password == null) {
      throw new KeyStoreValidationException("Password cannot be null");
    }
    return Bytes.wrap(
        SCrypt.generate(password.toArrayUnsafe(), salt().toArrayUnsafe(), n(), r(), p(), dklen()));
  }

  private static boolean isPowerOf2(final int x) {
    return (x & (x - 1)) == 0;
  }
}
