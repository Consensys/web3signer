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
package tech.pegasys.web3signer.core.service.http.handlers.signing;

import tech.pegasys.web3signer.core.signing.ArtifactSignature;
import tech.pegasys.web3signer.core.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.core.signing.KeyType;

import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;

public class SignerForIdentifier<T extends ArtifactSignature> {
  private final ArtifactSignerProvider signerProvider;
  private final SignatureFormatter<T> signatureFormatter;
  private final KeyType type;

  public SignerForIdentifier(
      final ArtifactSignerProvider signerProvider,
      final SignatureFormatter<T> signatureFormatter,
      final KeyType type) {
    this.signerProvider = signerProvider;
    this.signatureFormatter = signatureFormatter;
    this.type = type;
  }

  /**
   * Sign data for given identifier
   *
   * @param identifier The identifier for which to sign data.
   * @param data String in hex format which is signed
   * @return Optional String of signature (in hex format). Empty if no signer available for given
   *     identifier
   * @throws IllegalArgumentException if data is invalid i.e. not a valid hex string, null or empty.
   */
  public Optional<String> sign(final String identifier, final Bytes data) {
    return signerProvider.getSigner(identifier).map(signer -> formatSignature(signer.sign(data)));
  }

  @SuppressWarnings("unchecked")
  private String formatSignature(final ArtifactSignature signature) {
    if (signature.getType() == type) {
      final T artifactSignature = (T) signature;
      return signatureFormatter.format(artifactSignature);
    } else {
      throw new IllegalStateException("Invalid signature type");
    }
  }
}
