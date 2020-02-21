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

import tech.pegasys.eth2signer.crypto.BLS12381;
import tech.pegasys.eth2signer.crypto.KeyPair;
import tech.pegasys.eth2signer.crypto.Signature;
import tech.pegasys.eth2signer.crypto.SignatureAndPublicKey;

import org.apache.tuweni.bytes.Bytes;

public class ArtifactSigner {

  private final KeyPair key;

  public ArtifactSigner(final KeyPair key) {
    this.key = key;
  }

  public String getIdentifier() {
    return key.publicKey().toString();
  }

  public Signature sign(final Bytes message, final Bytes domain) {
    final SignatureAndPublicKey signatureResult = BLS12381.sign(key, message, domain);
    return signatureResult.signature();
  }
}
