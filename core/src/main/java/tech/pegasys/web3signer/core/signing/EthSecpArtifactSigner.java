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
package tech.pegasys.web3signer.core.signing;

import static tech.pegasys.web3signer.core.util.IdentifierUtils.normaliseIdentifier;

import tech.pegasys.signers.secp256k1.EthPublicKeyUtils;
import tech.pegasys.signers.secp256k1.api.Signer;

import org.apache.tuweni.bytes.Bytes;

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
}
