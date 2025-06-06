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
package tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;

import org.apache.tuweni.bytes.Bytes;

public class KZGProof {
  /** The number of bytes in this value - i.e. 48 */
  private static final int SIZE = 48;

  private final Bytes bytes;

  public KZGProof(final Bytes bytes) {
    checkArgument(
        bytes.size() == SIZE,
        "Bytes%s should be %s bytes, but was %s bytes.",
        SIZE,
        SIZE,
        bytes.size());
    this.bytes = bytes;
  }

  public KZGProof(final tech.pegasys.teku.kzg.KZGProof kzgProof) {
    this(kzgProof.toSSZBytes());
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final KZGProof other = (KZGProof) o;
    return bytes.equals(other.bytes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bytes);
  }

  @Override
  public String toString() {
    return bytes.toString();
  }

  public static KZGProof fromHexString(final String value) {
    try {
      return new KZGProof(tech.pegasys.teku.kzg.KZGProof.fromHexString(value));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("KZGProof " + value + " is invalid: " + e.getMessage(), e);
    }
  }

  public String toHexString() {
    return bytes.toHexString();
  }

  public Bytes toBytes() {
    return bytes;
  }

  public static KZGProof empty() {
    return new KZGProof(Bytes.wrap(new byte[SIZE]));
  }

  public tech.pegasys.teku.kzg.KZGProof asInternalKZGProof() {
    return tech.pegasys.teku.kzg.KZGProof.fromSSZBytes(bytes);
  }
}
