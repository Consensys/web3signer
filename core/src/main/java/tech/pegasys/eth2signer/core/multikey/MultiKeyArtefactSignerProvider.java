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
package tech.pegasys.eth2signer.core.multikey;

import tech.pegasys.eth2signer.core.multikey.metadata.SigningMetadataFile;
import tech.pegasys.eth2signer.core.multikey.metadata.UnencryptedKeyMetadataFile;
import tech.pegasys.eth2signer.core.signers.unencryptedfile.UnencryptedKeyFileSignerFactory;
import tech.pegasys.eth2signer.core.signing.ArtefactSignerProvider;
import tech.pegasys.eth2signer.core.signing.ArtifactSigner;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MultiKeyArtefactSignerProvider implements ArtefactSignerProvider, MultiSignerFactory {

  private static final Logger LOG = LogManager.getLogger();

  private final SigningMetadataTomlConfigLoader signingMetadataTomlConfigLoader;

  public MultiKeyArtefactSignerProvider(
      final SigningMetadataTomlConfigLoader signingMetadataTomlConfigLoader) {
    this.signingMetadataTomlConfigLoader = signingMetadataTomlConfigLoader;
  }

  @Override
  public Optional<ArtifactSigner> getSigner(final String signerIdentifier) {
    return signingMetadataTomlConfigLoader
        .loadMetadataForAddress(signerIdentifier)
        .map(metadataFile -> metadataFile.createSigner(this));
  }

  @Override
  public Set<String> availableSigners() {
    return signingMetadataTomlConfigLoader.loadAvailableSigningMetadataTomlConfigs().stream()
        .map(metadataFile -> metadataFile.createSigner(this))
        .filter(Objects::nonNull)
        .map(ArtifactSigner::getIdentifier)
        .collect(Collectors.toSet());
  }

  @Override
  public ArtifactSigner createSigner(final UnencryptedKeyMetadataFile metadataFile) {
    final ArtifactSigner signer;
    try {
      signer = UnencryptedKeyFileSignerFactory.createSigner(metadataFile.getKeyFile());
    } catch (final IOException e) {
      LOG.error("Unable to read key from file, {}.", metadataFile.getKeyFile().toString());
      return null;
    }

    if (filenameMatchesSigningAddress(signer, metadataFile)) {
      LOG.info("Loaded signer for address {}", signer.getIdentifier());
      return signer;
    }

    return null;
  }

  private boolean filenameMatchesSigningAddress(
      final ArtifactSigner signer, final SigningMetadataFile metadataFile) {

    // strip leading 0x from the address.
    final String signerAddress = signer.getIdentifier().substring(2).toLowerCase();
    if (!metadataFile.getBaseFilename().toLowerCase().endsWith(signerAddress)) {
      LOG.error(
          String.format(
              "Signer's Ethereum Address (%s) does not align with metadata filename (%s)",
              signerAddress, metadataFile.getBaseFilename()));
      return false;
    }
    return true;
  }
}
