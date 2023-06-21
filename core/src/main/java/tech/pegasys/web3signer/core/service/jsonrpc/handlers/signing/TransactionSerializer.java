/*
 * Copyright 2019 ConsenSys AG.
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
package tech.pegasys.web3signer.core.service.jsonrpc.handlers.signing;

import tech.pegasys.signers.secp256k1.api.Signature;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.sendtransaction.transaction.Transaction;
import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.signing.SecpArtifactSignature;

import org.apache.tuweni.bytes.Bytes;
import org.web3j.crypto.Sign.SignatureData;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.utils.Numeric;

public class TransactionSerializer {

  protected final ArtifactSignerProvider signer;
  protected final long chainId;

  protected final String identifier;

  public TransactionSerializer(
      final ArtifactSignerProvider signer, final long chainId, final String identifier) {
    this.signer = signer;
    this.chainId = chainId;
    this.identifier = identifier;
  }

  public String serialize(final Transaction transaction) {
    final byte[] bytesToSign = transaction.rlpEncode(chainId);

    final ArtifactSigner artifactSigner = signer.getSigner(identifier).get();
    final SecpArtifactSignature artifactSignature =
        ((SecpArtifactSignature) artifactSigner.sign(Bytes.of(bytesToSign)));
    final Signature signature = artifactSignature.getSignatureData();

    final SignatureData web3jSignature =
        new SignatureData(
            signature.getV().toByteArray(),
            signature.getR().toByteArray(),
            signature.getS().toByteArray());

    final SignatureData eip155Signature =
        TransactionEncoder.createEip155SignatureData(web3jSignature, chainId);

    final byte[] serializedBytes = transaction.rlpEncode(eip155Signature);
    return Numeric.toHexString(serializedBytes);
  }
}