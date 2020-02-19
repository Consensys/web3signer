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

import tech.pegasys.eth2signer.core.multikey.metadata.FileBasedSigningMetadataFile;
import tech.pegasys.eth2signer.core.multikey.metadata.HashicorpSigningMetadataFile;
import tech.pegasys.eth2signer.core.multikey.metadata.SigningMetadataFile;
import tech.pegasys.eth2signer.core.signers.filebased.FileBasedSignerFactory;
import tech.pegasys.eth2signer.core.signers.hashicorp.HashicorpVaultSignerFactory;
import tech.pegasys.eth2signer.core.signing.ArtefactSigner;
import tech.pegasys.eth2signer.core.signing.ArtefactSignerProvider;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MultiKeyTransactionSignerProvider
    implements ArtefactSignerProvider, MultiSignerFactory {

  private static final Logger LOG = LogManager.getLogger();

  private final SigningMetadataTomlConfigLoader signingMetadataTomlConfigLoader;

  MultiKeyTransactionSignerProvider(
      final SigningMetadataTomlConfigLoader signingMetadataTomlConfigLoader) {
    this.signingMetadataTomlConfigLoader = signingMetadataTomlConfigLoader;
  }

  @Override
  public Optional<ArtefactSigner> getSigner(final String address) {
    return signingMetadataTomlConfigLoader
        .loadMetadataForAddress(address)
        .map(metadataFile -> metadataFile.createSigner(this));
  }

  @Override
  public Set<String> availableAddresses() {
    return signingMetadataTomlConfigLoader.loadAvailableSigningMetadataTomlConfigs().stream()
        .map(metadataFile -> metadataFile.createSigner(this))
        .filter(Objects::nonNull)
        .map(ArtefactSigner::getAddress)
        .collect(Collectors.toSet());
  }

  @Override
  public ArtefactSigner createSigner(final HashicorpSigningMetadataFile metadataFile) {
    final ArtefactSigner signer;
    try {
      signer = HashicorpVaultSignerFactory.createSigner(metadataFile.getConfig());
    } catch (final RuntimeException e) {
      LOG.error("Failed to construct Hashicorp signer from " + metadataFile.getBaseFilename());
      return null;
    }

    if (filenameMatchesSigningAddress(signer, metadataFile)) {
      LOG.info("Loaded signer for address {}", signer.getAddress());
      return signer;
    }

    return null;
  }

  @Override
  public ArtefactSigner createSigner(final FileBasedSigningMetadataFile metadataFile) {
    try {
      final ArtefactSigner signer =
          FileBasedSignerFactory.createSigner(
              metadataFile.getKeyPath(), metadataFile.getPasswordPath());
      if (filenameMatchesSigningAddress(signer, metadataFile)) {
        LOG.info("Loaded signer for address {}", signer.getAddress());
        return signer;
      }

      return null;

    } catch (final RuntimeException e) {
      LOG.error("Unable to load signer with key " + metadataFile.getKeyPath().getFileName(), e);
      return null;
    }
  }

  private boolean filenameMatchesSigningAddress(
      final ArtefactSigner signer, final SigningMetadataFile metadataFile) {

    // strip leading 0x from the address.
    final String signerAddress = signer.getAddress().substring(2).toLowerCase();
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
