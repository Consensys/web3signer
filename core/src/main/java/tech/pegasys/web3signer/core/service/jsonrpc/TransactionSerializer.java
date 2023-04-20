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
package tech.pegasys.web3signer.core.service.jsonrpc;

import tech.pegasys.signers.secp256k1.api.Signature;
import tech.pegasys.signers.secp256k1.api.Signer;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.sendtransaction.transaction.Transaction;

import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign.SignatureData;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.utils.Numeric;

public class TransactionSerializer {

  protected final Signer signer;
  protected final long chainId;

  public TransactionSerializer(final Signer signer, final long chainId) {
    this.signer = signer;
    this.chainId = chainId;
  }

  public String serialize(final Transaction transaction) {
    final byte[] bytesToSign = transaction.rlpEncode(chainId);

    final Signature signature = signer.sign(bytesToSign);

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

  public String getAddress() {
    return Keys.getAddress(signer.getPublicKey().toString());
  }
}
