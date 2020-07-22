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
package tech.pegasys.eth2signer.core.service.operations;

import static tech.pegasys.eth2signer.core.service.operations.SignResponse.Type.SIGNER_NOT_FOUND;

import tech.pegasys.eth2signer.core.service.operations.SignResponse.Type;
import tech.pegasys.eth2signer.core.signing.ArtifactSignature;
import tech.pegasys.eth2signer.core.signing.ArtifactSignatureType;
import tech.pegasys.eth2signer.core.signing.ArtifactSigner;
import tech.pegasys.eth2signer.core.signing.ArtifactSignerProvider;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;

public class SignForIdentifier<T extends ArtifactSignature> {
  private static final Logger LOG = LogManager.getLogger();
  private final ArtifactSignerProvider signerProvider;
  private final SignatureFormatter<T> signatureFormatter;
  private final ArtifactSignatureType type;

  public SignForIdentifier(
      final ArtifactSignerProvider signerProvider,
      final SignatureFormatter<T> signatureFormatter,
      final ArtifactSignatureType type) {
    this.signerProvider = signerProvider;
    this.signatureFormatter = signatureFormatter;
    this.type = type;
  }

  public SignResponse sign(final String identifier, final String data) {
    final Optional<ArtifactSigner> signer = signerProvider.getSigner(identifier);
    if (signer.isEmpty()) {
      LOG.trace("Unsuitable handler for {}, invoking next handler", identifier);
      return new SignResponse(SIGNER_NOT_FOUND, identifier);
    }

    final Bytes dataToSign;
    try {
      if (StringUtils.isBlank(data)) {
        throw new IllegalArgumentException("Blank data");
      }
      dataToSign = Bytes.fromHexString(data);
    } catch (final IllegalArgumentException e) {
      LOG.debug("Invalid hex string {}", data, e);
      throw e;
    }
    final ArtifactSignature artifactSignature = signer.get().sign(dataToSign);
    final String formattedSignature = formatSignature(artifactSignature);
    return new SignResponse(Type.SIGNATURE_OK, formattedSignature);
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
