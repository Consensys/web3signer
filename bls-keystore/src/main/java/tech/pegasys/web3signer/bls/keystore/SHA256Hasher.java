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

public class SHA256Hasher {
  // Thread-local MessageDigest for thread safety and performance
  private static final ThreadLocal<MessageDigest> digestThreadLocal =
      ThreadLocal.withInitial(
          () -> {
            try {
              return MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
              throw new RuntimeException("Failed to initialize SHA-256 MessageDigest", e);
            }
          });

  /**
   * Computes the SHA-256 hash of the input byte array.
   *
   * @param input The Bytes to hash (non-null).
   * @return The SHA-256 hash in Bytes.
   * @throws NullPointerException if the input is {@code null}.
   */
  public static Bytes calculateSHA256(final Bytes input) {
    checkNotNull(input, "Input Bytes must not be null");
    MessageDigest digest = digestThreadLocal.get();
      digest.reset(); // Reset before reuse (important!)
      return Bytes.wrap(digest.digest(input.toArrayUnsafe()));
  }
}
