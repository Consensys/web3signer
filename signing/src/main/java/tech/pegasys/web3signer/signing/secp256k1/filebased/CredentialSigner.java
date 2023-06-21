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
package tech.pegasys.web3signer.signing.secp256k1.filebased;

import tech.pegasys.web3signer.signing.secp256k1.EthPublicKeyUtils;
import tech.pegasys.web3signer.signing.secp256k1.Signature;
import tech.pegasys.web3signer.signing.secp256k1.Signer;

import java.math.BigInteger;
import java.security.interfaces.ECPublicKey;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.Sign;
import org.web3j.crypto.Sign.SignatureData;

public class CredentialSigner implements Signer {

  private final Credentials credentials;
  private final ECPublicKey publicKey;
  private final boolean needToHash;

  public CredentialSigner(final Credentials credentials, final boolean needToHash) {
    this.credentials = credentials;
    this.publicKey = EthPublicKeyUtils.createPublicKey(credentials.getEcKeyPair().getPublicKey());
    this.needToHash = needToHash;
  }

  public CredentialSigner(final Credentials credentials) {
    this(credentials, true);
  }

  @Override
  public Signature sign(final byte[] data) {
    final SignatureData signature = Sign.signMessage(data, credentials.getEcKeyPair(), needToHash);
    return new Signature(
        new BigInteger(signature.getV()),
        new BigInteger(1, signature.getR()),
        new BigInteger(1, signature.getS()));
  }

  @Override
  public ECPublicKey getPublicKey() {
    return publicKey;
  }
}
