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
package tech.pegasys.web3signer.signing.secp256k1.multikey;

import tech.pegasys.web3signer.signing.secp256k1.EthPublicKeyUtils;
import tech.pegasys.web3signer.signing.secp256k1.FileSelector;
import tech.pegasys.web3signer.signing.secp256k1.Signer;
import tech.pegasys.web3signer.signing.secp256k1.SignerIdentifier;
import tech.pegasys.web3signer.signing.secp256k1.SignerProvider;
import tech.pegasys.web3signer.signing.secp256k1.azure.AzureConfig;
import tech.pegasys.web3signer.signing.secp256k1.azure.AzureKeyVaultSignerFactory;
import tech.pegasys.web3signer.signing.secp256k1.common.SignerInitializationException;
import tech.pegasys.web3signer.signing.secp256k1.filebased.CredentialSigner;
import tech.pegasys.web3signer.signing.secp256k1.filebased.FileBasedSignerFactory;
import tech.pegasys.web3signer.signing.secp256k1.hashicorp.HashicorpSignerFactory;
import tech.pegasys.web3signer.signing.secp256k1.multikey.metadata.AzureSigningMetadataFile;
import tech.pegasys.web3signer.signing.secp256k1.multikey.metadata.FileBasedSigningMetadataFile;
import tech.pegasys.web3signer.signing.secp256k1.multikey.metadata.HashicorpSigningMetadataFile;
import tech.pegasys.web3signer.signing.secp256k1.multikey.metadata.RawSigningMetadataFile;
import tech.pegasys.web3signer.signing.secp256k1.multikey.metadata.SigningMetadataFile;

import java.io.IOException;
import java.nio.file.Path;
import java.security.interfaces.ECPublicKey;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.crypto.Credentials;

public class MultiKeySignerProvider implements SignerProvider, MultiSignerFactory {

  private static final Logger LOG = LogManager.getLogger();

  private final SigningMetadataTomlConfigLoader signingMetadataTomlConfigLoader;
  private final HashicorpSignerFactory hashicorpSignerFactory;
  private final FileSelector<Void> allConfigFilesSelector;
  private final FileSelector<SignerIdentifier> signerIdentifierConfigFileSelector;

  public static MultiKeySignerProvider create(
      final Path rootDir,
      final FileSelector<Void> allConfigFilesSelector,
      final FileSelector<SignerIdentifier> signerIdentifierConfigFileSelector) {
    final SigningMetadataTomlConfigLoader signingMetadataTomlConfigLoader =
        new SigningMetadataTomlConfigLoader(rootDir);

    final HashicorpSignerFactory hashicorpSignerFactory = new HashicorpSignerFactory();

    return new MultiKeySignerProvider(
        signingMetadataTomlConfigLoader,
        hashicorpSignerFactory,
        allConfigFilesSelector,
        signerIdentifierConfigFileSelector);
  }

  public MultiKeySignerProvider(
      final SigningMetadataTomlConfigLoader signingMetadataTomlConfigLoader,
      final HashicorpSignerFactory hashicorpSignerFactory,
      final FileSelector<Void> allConfigFilesSelector,
      final FileSelector<SignerIdentifier> signerIdentifierConfigFileSelector) {
    this.signingMetadataTomlConfigLoader = signingMetadataTomlConfigLoader;
    this.hashicorpSignerFactory = hashicorpSignerFactory;
    this.allConfigFilesSelector = allConfigFilesSelector;
    this.signerIdentifierConfigFileSelector = signerIdentifierConfigFileSelector;
  }

  @Override
  public Optional<Signer> getSigner(final SignerIdentifier signerIdentifier) {
    if (signerIdentifier == null) {
      return Optional.empty();
    }

    final Optional<Signer> signer =
        signingMetadataTomlConfigLoader
            .loadMetadata(signerIdentifierConfigFileSelector.getConfigFilesFilter(signerIdentifier))
            .map(metadataFile -> metadataFile.createSigner(this));
    if (signer.isPresent()) {
      if (signerIdentifier.validate(signer.get().getPublicKey())) {
        return signer;
      } else {
        LOG.warn(
            "Signer loaded from file with public key ({}) does not validate with the supplied identifier ({})",
            EthPublicKeyUtils.toHexString(signer.get().getPublicKey()),
            signerIdentifier.toStringIdentifier());
      }
    }
    return Optional.empty();
  }

  @Override
  public Set<ECPublicKey> availablePublicKeys(
      final Function<ECPublicKey, SignerIdentifier> identifierFunction) {
    return signingMetadataTomlConfigLoader
        .loadAvailableSigningMetadataTomlConfigs(allConfigFilesSelector.getConfigFilesFilter(null))
        .parallelStream()
        .map(metaFile -> createSigner(metaFile, identifierFunction))
        .filter(Objects::nonNull)
        .map(Signer::getPublicKey)
        .collect(Collectors.toSet());
  }

  private Signer createSigner(
      final SigningMetadataFile metadataFile,
      Function<ECPublicKey, SignerIdentifier> identifierFunction) {
    final Signer signer = metadataFile.createSigner(this);
    try {
      if ((signer != null)
          && signerIdentifierConfigFileSelector
              .getConfigFilesFilter(identifierFunction.apply(signer.getPublicKey()))
              .accept(Path.of(metadataFile.getFilename()))) {
        return signer;
      }
      return null;
    } catch (final IOException e) {
      LOG.warn("IO Exception raised while loading {}", metadataFile.getFilename());
      return null;
    }
  }

  @Override
  public Signer createSigner(final AzureSigningMetadataFile metadataFile) {
    try {
      final AzureConfig config = metadataFile.getConfig();
      final AzureKeyVaultSignerFactory azureFactory = new AzureKeyVaultSignerFactory();
      return azureFactory.createSigner(config);
    } catch (final SignerInitializationException e) {
      LOG.error("Failed to construct Azure signer from " + metadataFile.getFilename());
      return null;
    }
  }

  @Override
  public Signer createSigner(final HashicorpSigningMetadataFile metadataFile) {
    try {
      return hashicorpSignerFactory.create(metadataFile.getConfig());
    } catch (final SignerInitializationException e) {
      LOG.error("Failed to construct Hashicorp signer from " + metadataFile.getFilename());
      return null;
    }
  }

  @Override
  public Signer createSigner(final FileBasedSigningMetadataFile metadataFile) {
    try {
      return FileBasedSignerFactory.createSigner(metadataFile.getConfig());

    } catch (final SignerInitializationException e) {
      LOG.error("Unable to construct Filebased signer from " + metadataFile.getFilename());
      return null;
    }
  }

  @Override
  public Signer createSigner(final RawSigningMetadataFile metadataFile) {
    try {
      final Credentials credentials = Credentials.create(metadataFile.getPrivKey());
      return new CredentialSigner(credentials);
    } catch (final Exception e) {
      LOG.error("Unable to construct raw signer from " + metadataFile.getFilename());
      return null;
    }
  }

  @Override
  public void shutdown() {
    hashicorpSignerFactory.shutdown(); // required to clean up its Vertx instance.
  }
}
