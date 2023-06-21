/*
 * Copyright 2020 ConsenSys AG.
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
package tech.pegasys.web3signer.signing;

import tech.pegasys.web3signer.signing.secp256k1.Signature;
import tech.pegasys.web3signer.signing.util.ByteUtils;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.web3j.utils.Numeric;

public class SecpArtifactSignature implements ArtifactSignature {
  private final Signature signature;

  public SecpArtifactSignature(final Signature signature) {
    this.signature = signature;
  }

  @Override
  public KeyType getType() {
    return KeyType.SECP256K1;
  }

  public Signature getSignatureData() {
    return signature;
  }

  public static SecpArtifactSignature fromBytes(final Bytes signature) {
    final Bytes r = signature.slice(0, 32);
    final Bytes s = signature.slice(32, 32);
    final Bytes v = signature.slice(64);
    return new SecpArtifactSignature(
        new Signature(
            Numeric.toBigInt(v.toArrayUnsafe()),
            Numeric.toBigInt(r.toArrayUnsafe()),
            Numeric.toBigInt(s.toArrayUnsafe())));
  }

  public static Bytes toBytes(final SecpArtifactSignature signature) {
    final Signature signatureData = signature.getSignatureData();
    return Bytes.concatenate(
        Bytes32.leftPad(Bytes.wrap(ByteUtils.bigIntegerToBytes(signatureData.getR()))),
        Bytes32.leftPad(Bytes.wrap(ByteUtils.bigIntegerToBytes(signatureData.getS()))),
        Bytes.wrap(ByteUtils.bigIntegerToBytes(signatureData.getV())));
  }
}
