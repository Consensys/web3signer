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
package tech.pegasys.eth2signer.core.signing;

import static com.google.common.base.Preconditions.checkArgument;
import static tech.pegasys.eth2signer.core.service.operations.IdentifierUtils.normaliseIdentifier;

import tech.pegasys.signers.secp256k1.EthPublicKeyUtils;
import tech.pegasys.signers.secp256k1.api.Signature;
import tech.pegasys.signers.secp256k1.api.Signer;

import java.math.BigInteger;

import org.apache.tuweni.bytes.Bytes;
import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

public class EthSecpArtifactSigner implements ArtifactSigner {

  private final Signer signer;

  public EthSecpArtifactSigner(final Signer signer) {
    this.signer = signer;
  }

  @Override
  public String getIdentifier() {
    return normaliseIdentifier(EthPublicKeyUtils.toHexString(signer.getPublicKey()));
  }

  @Override
  public SecpArtifactSignature sign(final Bytes message) {
    return new SecpArtifactSignature(signer.sign(message.toArray()));
  }

  @Override
  public boolean verify(final Bytes message, final ArtifactSignature signature) {
    checkArgument(signature instanceof SecpArtifactSignature);
    final SecpArtifactSignature secpArtifactSignature = (SecpArtifactSignature) signature;
    final Signature signatureData = secpArtifactSignature.getSignatureData();

    final ECDSASignature initialSignature =
        new ECDSASignature(signatureData.getR(), signatureData.getS());
    final ECDSASignature canonicalSignature = initialSignature.toCanonicalised();

    final int recId = signatureData.getV().intValue();
    final byte[] digest = Hash.sha3(message.toArrayUnsafe());
    final BigInteger signaturePublicKey =
        Sign.recoverFromSignature(recId, canonicalSignature, digest);
    final BigInteger expectedPublicKey =
        Numeric.toBigInt(EthPublicKeyUtils.toByteArray(signer.getPublicKey()));
    return signaturePublicKey.equals(expectedPublicKey);
  }
}
