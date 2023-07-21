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
package tech.pegasys.web3signer.signing.config.metadata;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

import tech.pegasys.web3signer.keystorage.azure.AzureKeyVault;
import tech.pegasys.web3signer.keystorage.hashicorp.HashicorpConnection;
import tech.pegasys.web3signer.keystorage.hashicorp.HashicorpConnectionFactory;
import tech.pegasys.web3signer.keystorage.hashicorp.TrustStoreType;
import tech.pegasys.web3signer.keystorage.hashicorp.config.ConnectionParameters;
import tech.pegasys.web3signer.keystorage.hashicorp.config.KeyDefinition;
import tech.pegasys.web3signer.keystorage.hashicorp.config.TlsOptions;
import tech.pegasys.web3signer.signing.KeyType;
import tech.pegasys.web3signer.signing.config.AzureKeyVaultFactory;
import tech.pegasys.web3signer.signing.config.metadata.interlock.InterlockKeyProvider;
import tech.pegasys.web3signer.signing.config.metadata.yubihsm.YubiHsmOpaqueDataProvider;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import com.google.common.io.Files;
import org.apache.tuweni.bytes.Bytes;

public abstract class AbstractArtifactSignerFactory implements ArtifactSignerFactory {

  final HashicorpConnectionFactory hashicorpConnectionFactory;
  final Path configsDirectory;
  private final InterlockKeyProvider interlockKeyProvider;
  private final YubiHsmOpaqueDataProvider yubiHsmOpaqueDataProvider;
  private final AzureKeyVaultFactory azureKeyVaultFactory;

  protected AbstractArtifactSignerFactory(
      final HashicorpConnectionFactory hashicorpConnectionFactory,
      final Path configsDirectory,
      final InterlockKeyProvider interlockKeyProvider,
      final YubiHsmOpaqueDataProvider yubiHsmOpaqueDataProvider,
      final AzureKeyVaultFactory azureKeyVaultFactory) {
    this.hashicorpConnectionFactory = hashicorpConnectionFactory;
    this.configsDirectory = configsDirectory;
    this.interlockKeyProvider = interlockKeyProvider;
    this.yubiHsmOpaqueDataProvider = yubiHsmOpaqueDataProvider;
    this.azureKeyVaultFactory = azureKeyVaultFactory;
  }

  protected Bytes extractBytesFromVault(final AzureSecretSigningMetadata metadata) {
    final AzureKeyVault azureVault = azureKeyVaultFactory.createAzureKeyVault(metadata);

    return azureVault
        .fetchSecret(metadata.getSecretName())
        .map(Bytes::fromHexString)
        .orElseThrow(
            () ->
                new SigningMetadataException(
                    "secret '" + metadata.getSecretName() + "' doesn't exist"));
  }

  protected Bytes extractBytesFromVault(final HashicorpSigningMetadata metadata) {
    final Optional<TlsOptions> tlsOptions = buildTlsOptions(metadata);

    try {
      final ConnectionParameters connectionParameters =
          ConnectionParameters.newBuilder()
              .withServerHost(metadata.getServerHost())
              .withServerPort(metadata.getServerPort())
              .withTlsOptions(tlsOptions.orElse(null))
              .withTimeoutMs(metadata.getTimeout())
              .withHttpProtocolVersion(metadata.getHttpProtocolVersion())
              .build();
      final HashicorpConnection connection =
          hashicorpConnectionFactory.create(connectionParameters);

      final String secret =
          connection.fetchKey(
              new KeyDefinition(
                  metadata.getKeyPath(),
                  Optional.ofNullable(metadata.getKeyName()),
                  metadata.getToken()));
      return Bytes.fromHexString(secret);
    } catch (final Exception e) {
      throw new SigningMetadataException("Failed to fetch secret from hashicorp vault", e);
    }
  }

  protected Bytes extractBytesFromInterlock(final InterlockSigningMetadata metadata) {
    try {
      return interlockKeyProvider.fetchKey(metadata);
    } catch (final RuntimeException e) {
      throw new SigningMetadataException(
          "Failed to fetch secret from Interlock: " + e.getMessage(), e);
    }
  }

  protected Bytes extractOpaqueDataFromYubiHsm(
      final YubiHsmSigningMetadata yubiHsmSigningMetadata) {
    try {
      return yubiHsmOpaqueDataProvider.fetchOpaqueData(yubiHsmSigningMetadata);
    } catch (final RuntimeException e) {
      throw new SigningMetadataException(
          "Failed to fetch opaque data from YubiHSM: " + e.getMessage(), e);
    }
  }

  private Optional<TlsOptions> buildTlsOptions(final HashicorpSigningMetadata metadata) {
    if (metadata.getTlsEnabled()) {
      final Path knownServerFile = metadata.getTlsKnownServerFile();
      if (knownServerFile == null) {
        return Optional.of(new TlsOptions(Optional.empty(), null, null)); // use CA Auth
      } else {
        final Path configRelativeKnownServerPath = makeRelativePathAbsolute(knownServerFile);
        if (!configRelativeKnownServerPath.toFile().exists()) {
          throw new SigningMetadataException(
              String.format(
                  "Known servers file (%s) does not exist.", configRelativeKnownServerPath));
        }
        return Optional.of(
            new TlsOptions(Optional.of(TrustStoreType.WHITELIST), knownServerFile, null));
      }
    }
    return Optional.empty();
  }

  protected String loadPassword(final Path passwordFile) {
    try {
      final String password = Files.asCharSource(passwordFile.toFile(), UTF_8).readFirstLine();
      if (password == null || password.isBlank()) {
        throw new SigningMetadataException("Keystore password cannot be empty: " + passwordFile);
      }
      return password;
    } catch (final FileNotFoundException e) {
      throw new SigningMetadataException("Keystore password file not found: " + passwordFile, e);
    } catch (final IOException e) {
      final String errorMessage =
          format(
              "Unexpected IO error while reading keystore password file [%s]: %s",
              passwordFile, e.getMessage());
      throw new SigningMetadataException(errorMessage, e);
    }
  }

  protected Path makeRelativePathAbsolute(final Path path) {
    return path.isAbsolute() ? path : configsDirectory.resolve(path);
  }

  public abstract KeyType getKeyType();
}
