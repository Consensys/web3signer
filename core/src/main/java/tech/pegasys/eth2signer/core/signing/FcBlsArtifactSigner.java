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

import static tech.pegasys.filecoin.bls12_381.LibBlsFilecoin.fil_private_key_public_key;

import tech.pegasys.eth2signer.core.signing.filecoin.FilecoinAddress;
import tech.pegasys.eth2signer.core.signing.filecoin.FilecoinNetwork;
import tech.pegasys.filecoin.bls12_381.LibBlsFilecoin;
import tech.pegasys.filecoin.bls12_381.LibBlsFilecoin.fil_PrivateKeyPublicKeyResponse;
import tech.pegasys.filecoin.bls12_381.LibBlsFilecoin.fil_PrivateKeySignResponse;

import org.apache.tuweni.bytes.Bytes;

public class FcBlsArtifactSigner implements ArtifactSigner {
  private final Bytes privateKey;
  private final FilecoinNetwork filecoinNetwork;

  public FcBlsArtifactSigner(final Bytes privateKey, final FilecoinNetwork filecoinNetwork) {
    this.privateKey = privateKey;
    this.filecoinNetwork = filecoinNetwork;
  }

  @Override
  public String getIdentifier() {
    final fil_PrivateKeyPublicKeyResponse publicKeyResponse =
        fil_private_key_public_key(privateKey.toArrayUnsafe());
    final Bytes publicKey = Bytes.wrap(publicKeyResponse.public_key);
    LibBlsFilecoin.fil_destroy_private_key_public_key_response(publicKeyResponse);
    return FilecoinAddress.blsAddress(publicKey).encode(filecoinNetwork);
  }

  @Override
  public ArtifactSignature sign(final Bytes data) {
    final fil_PrivateKeySignResponse signResponse =
        LibBlsFilecoin.fil_private_key_sign(
            privateKey.toArrayUnsafe(), data.toArrayUnsafe(), data.size());
    final Bytes signature = Bytes.wrap(signResponse.signature);
    LibBlsFilecoin.fil_destroy_private_key_sign_response(signResponse);
    return new FcBlsArtifactSignature(signature);
  }
}
