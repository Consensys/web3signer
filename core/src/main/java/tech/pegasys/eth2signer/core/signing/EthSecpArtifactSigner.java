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
import java.security.SignatureException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.crypto.Sign.SignatureData;
import org.web3j.utils.Numeric;

public class EthSecpArtifactSigner implements ArtifactSigner {
  private static final Logger LOG = LogManager.getLogger();
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
  public boolean verify(final Bytes message, final ArtifactSignature artifactSignature) {
    checkArgument(artifactSignature instanceof SecpArtifactSignature);
    final SecpArtifactSignature secpArtifactSignature = (SecpArtifactSignature) artifactSignature;
    final Signature signature = secpArtifactSignature.getSignatureData();
    final SignatureData signatureData =
        new SignatureData(
            Numeric.toBytesPadded(signature.getV(), 1),
            Numeric.toBytesPadded(signature.getR(), 32),
            Numeric.toBytesPadded(signature.getS(), 32));

    final byte[] digest = Hash.sha3(message.toArrayUnsafe());

    try {
      final BigInteger signaturePublicKey = Sign.signedMessageHashToKey(digest, signatureData);
      final BigInteger expectedPublicKey =
          Numeric.toBigInt(EthPublicKeyUtils.toByteArray(signer.getPublicKey()));
      return signaturePublicKey.equals(expectedPublicKey);
    } catch (final SignatureException e) {
      LOG.error("Unable to recover public key from signature", e);
      return false;
    }
  }
}
