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
package tech.pegasys.web3signer.bls.keystore;

import static com.google.common.base.Preconditions.checkNotNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.tuweni.bytes.Bytes;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class SHA256Hasher {
  private static final BouncyCastleProvider BC = new BouncyCastleProvider();
  private static final String SHA_256 = "SHA-256";

  /**
   * Computes the SHA-256 hash of the input bytes using BouncyCastle provider.
   *
   * @param input The Bytes to hash (non-null)
   * @return 32-byte SHA-256 hash
   * @throws NullPointerException if input is null
   * @throws RuntimeException if SHA-256 algorithm is unavailable (unlikely)
   */
  public static Bytes calculateSHA256(final Bytes input) {
    checkNotNull(input, "Input Bytes must not be null");

    try {
      return Bytes.wrap(MessageDigest.getInstance(SHA_256, BC).digest(input.toArrayUnsafe()));
    } catch (NoSuchAlgorithmException e) {
      // This should never happen since SHA-256 is a standard algorithm
      throw new RuntimeException(e);
    }
  }
}
