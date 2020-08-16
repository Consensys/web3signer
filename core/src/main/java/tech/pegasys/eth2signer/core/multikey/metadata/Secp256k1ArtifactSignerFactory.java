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
package tech.pegasys.eth2signer.core.multikey.metadata;

import tech.pegasys.eth2signer.core.multikey.metadata.model.AzureKeySigningMetadata;
import tech.pegasys.eth2signer.core.multikey.metadata.model.AzureSecretSigningMetadata;
import tech.pegasys.eth2signer.core.multikey.metadata.model.FileKeyStoreMetadata;
import tech.pegasys.eth2signer.core.multikey.metadata.model.FileRawSigningMetadata;
import tech.pegasys.eth2signer.core.multikey.metadata.model.HashicorpSigningMetadata;
import tech.pegasys.eth2signer.core.signing.ArtifactSigner;
import tech.pegasys.eth2signer.core.signing.SecpArtifactSigner;
import tech.pegasys.signers.hashicorp.HashicorpConnectionFactory;
import tech.pegasys.signers.secp256k1.azure.AzureConfig;
import tech.pegasys.signers.secp256k1.azure.AzureKeyVaultSignerFactory;
import tech.pegasys.signers.secp256k1.filebased.CredentialSigner;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.tuweni.bytes.Bytes;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

public class Secp256k1ArtifactSignerFactory extends AbstractArtifactSignerFactory {

  final AzureKeyVaultSignerFactory azureCloudSignerFactory;

  public Secp256k1ArtifactSignerFactory(
      final HashicorpConnectionFactory connectionFactory,
      final Path configsDirectory,
      final AzureKeyVaultSignerFactory azureCloudSignerFactory) {
    super(connectionFactory, configsDirectory);
    this.azureCloudSignerFactory = azureCloudSignerFactory;
  }

  @Override
  public ArtifactSigner create(final FileRawSigningMetadata fileRawSigningMetadata) {
    final Bytes privateKeyBytes = fileRawSigningMetadata.getPrivateKeyBytes();
    final Credentials credentials = Credentials.create(privateKeyBytes.toHexString());
    return new SecpArtifactSigner(new CredentialSigner(credentials));
  }

  @Override
  public ArtifactSigner create(final FileKeyStoreMetadata fileKeyStoreMetadata) {
    final Path keystoreFile = makeRelativePathAbsolute(fileKeyStoreMetadata.getKeystoreFile());
    final Path keystorePasswordFile =
        makeRelativePathAbsolute(fileKeyStoreMetadata.getKeystorePasswordFile());
    try {
      final String password = loadPassword(keystorePasswordFile);
      final Credentials credentials = WalletUtils.loadCredentials(password, keystoreFile.toFile());
      return new SecpArtifactSigner(new CredentialSigner(credentials));
    } catch (final IOException | CipherException e) {
      throw new SigningMetadataException(e.getMessage(), e);
    }
  }

  @Override
  public ArtifactSigner create(final HashicorpSigningMetadata hashicorpMetadata) {
    final Bytes privateKeyBytes = extractBytesFromVault(hashicorpMetadata);
    final Credentials credentials = Credentials.create(privateKeyBytes.toHexString());
    return new SecpArtifactSigner(new CredentialSigner(credentials));
  }

  @Override
  public ArtifactSigner create(final AzureSecretSigningMetadata azureSecretSigningMetadata) {
    final Bytes privateKeyBytes = extractBytesFromVault(azureSecretSigningMetadata);
    final Credentials credentials = Credentials.create(privateKeyBytes.toHexString());
    return new SecpArtifactSigner(new CredentialSigner(credentials));
  }

  @Override
  public ArtifactSigner create(final AzureKeySigningMetadata azureSigningMetadata) {
    final AzureConfig config =
        new AzureConfig(
            azureSigningMetadata.getVaultName(),
            azureSigningMetadata.getKeyName(),
            "",
            azureSigningMetadata.getClientId(),
            azureSigningMetadata.getClientSecret(),
            azureSigningMetadata.getTenantId());

    return new SecpArtifactSigner(azureCloudSignerFactory.createSigner(config));
  }
}
