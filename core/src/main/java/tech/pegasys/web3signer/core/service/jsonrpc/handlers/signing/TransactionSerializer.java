/*
 * Copyright 2023 ConsenSys AG.
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

import static org.web3j.utils.Numeric.toHexString;
import static tech.pegasys.web3signer.core.service.jsonrpc.handlers.sendtransaction.transaction.Transaction.longToBytes;

import tech.pegasys.web3signer.core.service.jsonrpc.handlers.sendtransaction.transaction.Transaction;
import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.SecpArtifactSignature;
import tech.pegasys.web3signer.signing.secp256k1.Signature;

import java.nio.ByteBuffer;

import org.apache.tuweni.bytes.Bytes;
import org.web3j.crypto.Sign.SignatureData;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.transaction.type.TransactionType;

public class TransactionSerializer {

  protected final ArtifactSigner signer;
  protected final long chainId;

  public TransactionSerializer(final ArtifactSigner signer, final long chainId) {
    this.signer = signer;
    this.chainId = chainId;
  }

  public String serialize(final Transaction transaction) {
    if (transaction.isEip1559()) {
      return toHexString(serializeEip1559(transaction));
    } else {
      return toHexString(serializeFrontier(transaction));
    }
  }

  private byte[] serializeFrontier(final Transaction transaction) {
    final SignatureData preSigningSignatureData =
        new SignatureData(longToBytes(chainId), new byte[] {}, new byte[] {});
    byte[] bytesToSign = transaction.rlpEncode(preSigningSignatureData);

    SignatureData signatureData = sign(bytesToSign);

    signatureData = TransactionEncoder.createEip155SignatureData(signatureData, chainId);
    return transaction.rlpEncode(signatureData);
  }

  private byte[] serializeEip1559(final Transaction transaction) {
    byte[] bytesToSign = transaction.rlpEncode(null);
    bytesToSign = prependEip1559TransactionType(bytesToSign);

    final SignatureData signatureData = sign(bytesToSign);

    byte[] serializedBytes = transaction.rlpEncode(signatureData);
    return prependEip1559TransactionType(serializedBytes);
  }

  private static byte[] prependEip1559TransactionType(byte[] bytesToSign) {
    return ByteBuffer.allocate(bytesToSign.length + 1)
        .put(TransactionType.EIP1559.getRlpType())
        .put(bytesToSign)
        .array();
  }

  private SignatureData sign(final byte[] bytesToSign) {
    final SecpArtifactSignature artifactSignature =
        (SecpArtifactSignature) signer.sign(Bytes.of(bytesToSign));

    final Signature signature = artifactSignature.getSignatureData();

    return new SignatureData(
        signature.getV().toByteArray(),
        signature.getR().toByteArray(),
        signature.getS().toByteArray());
  }
}
