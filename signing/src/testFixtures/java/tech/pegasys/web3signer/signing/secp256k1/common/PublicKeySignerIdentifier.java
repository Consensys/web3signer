/*
 * Copyright 2021 ConsenSys AG.
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
package tech.pegasys.web3signer.signing.secp256k1.common;

import static tech.pegasys.web3signer.signing.secp256k1.util.AddressUtil.remove0xPrefix;

import tech.pegasys.web3signer.signing.secp256k1.EthPublicKeyUtils;
import tech.pegasys.web3signer.signing.secp256k1.SignerIdentifier;

import java.security.interfaces.ECPublicKey;

public class PublicKeySignerIdentifier implements SignerIdentifier {
  private final ECPublicKey publicKey;

  public PublicKeySignerIdentifier(final ECPublicKey publicKey) {
    this.publicKey = publicKey;
  }

  @Override
  public String toStringIdentifier() {
    return remove0xPrefix(EthPublicKeyUtils.toHexString(publicKey));
  }

  @Override
  public boolean validate(final ECPublicKey publicKey) {
    return EthPublicKeyUtils.toHexString(this.publicKey)
        .equalsIgnoreCase(EthPublicKeyUtils.toHexString(publicKey));
  }
}
