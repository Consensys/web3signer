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

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

import tech.pegasys.eth2signer.core.metrics.Eth2SignerMetricCategory;
import tech.pegasys.eth2signer.core.signing.ArtifactSigner;
import tech.pegasys.eth2signer.core.signing.BlsArtifactSigner;
import tech.pegasys.signers.bls.keystore.KeyStore;
import tech.pegasys.signers.bls.keystore.KeyStoreLoader;
import tech.pegasys.signers.bls.keystore.KeyStoreValidationException;
import tech.pegasys.signers.bls.keystore.model.KeyStoreData;
import tech.pegasys.signers.hashicorp.HashicorpConnection;
import tech.pegasys.signers.hashicorp.HashicorpConnectionFactory;
import tech.pegasys.signers.hashicorp.TrustStoreType;
import tech.pegasys.signers.hashicorp.config.ConnectionParameters;
import tech.pegasys.signers.hashicorp.config.KeyDefinition;
import tech.pegasys.signers.hashicorp.config.TlsOptions;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSSecretKey;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import com.google.common.io.Files;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.metrics.LabelledMetric;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer.TimingContext;

public class ArtifactSignerFactory {

  private final LabelledMetric<OperationTimer> privateKeyRetrievalTimer;
  private final Path configsDirectory;
  private final HashicorpConnectionFactory connectionFactory;

  public ArtifactSignerFactory(
      final Path configsDirectory,
      final MetricsSystem metricsSystem,
      final HashicorpConnectionFactory connectionFactory) {
    this.configsDirectory = configsDirectory;
    privateKeyRetrievalTimer =
        metricsSystem.createLabelledTimer(
            Eth2SignerMetricCategory.SIGNING,
            "private_key_retrieval_time",
            "Time taken to retrieve private key",
            "signer");
    this.connectionFactory = connectionFactory;
  }

  public ArtifactSigner create(final FileRawSigningMetadata fileRawSigningMetadata) {
    try (TimingContext ignored = privateKeyRetrievalTimer.labels("file-raw").startTimer()) {
      return new BlsArtifactSigner(new BLSKeyPair(fileRawSigningMetadata.getSecretKey()));
    }
  }

  public ArtifactSigner create(final FileKeyStoreMetadata fileKeyStoreMetadata) {
    try (TimingContext ignored = privateKeyRetrievalTimer.labels("file-keystore").startTimer()) {
      return createKeystoreArtifact(fileKeyStoreMetadata);
    }
  }

  public ArtifactSigner create(final HashicorpSigningMetadata hashicorpMetadata) {
    try (TimingContext ignored = privateKeyRetrievalTimer.labels("hashicorp").startTimer()) {
      return createHashicorpArtifact(hashicorpMetadata);
    }
  }

  public ArtifactSigner create(final AzureSigningMetadata azureSigningMetadata) {
    try (TimingContext ignored = privateKeyRetrievalTimer.labels("azure").startTimer()) {
      return createAzureArtifact(azureSigningMetadata);
    }
  }

  private ArtifactSigner createKeystoreArtifact(final FileKeyStoreMetadata fileKeyStoreMetadata) {
    final Path keystoreFile = makeRelativePathAbsolute(fileKeyStoreMetadata.getKeystoreFile());
    final Path keystorePasswordFile =
        makeRelativePathAbsolute(fileKeyStoreMetadata.getKeystorePasswordFile());
    try {
      final KeyStoreData keyStoreData = KeyStoreLoader.loadFromFile(keystoreFile);
      final String password = loadPassword(keystorePasswordFile);
      final Bytes privateKey = KeyStore.decrypt(password, keyStoreData);
      final BLSKeyPair keyPair = new BLSKeyPair(BLSSecretKey.fromBytes(privateKey));
      return new BlsArtifactSigner(keyPair);
    } catch (final KeyStoreValidationException e) {
      throw new SigningMetadataException(e.getMessage(), e);
    }
  }

  private ArtifactSigner createHashicorpArtifact(final HashicorpSigningMetadata metadata) {
    TlsOptions tlsOptions = null;
    if (metadata.getTlsEnabled()) {
      final Path knownServerFile = metadata.getTlsKnownServerFile();
      if (knownServerFile == null) {
        tlsOptions = new TlsOptions(Optional.empty(), null, null); // use CA Auth
      } else {
        final Path configRelativeKnownServerPath = makeRelativePathAbsolute(knownServerFile);
        if (!configRelativeKnownServerPath.toFile().exists()) {
          throw new SigningMetadataException(
              String.format(
                  "Known servers file (%s) does not exist.", configRelativeKnownServerPath));
        }
        tlsOptions = new TlsOptions(Optional.of(TrustStoreType.WHITELIST), knownServerFile, null);
      }
    }
    try {
      final HashicorpConnection connection =
          connectionFactory.create(
              new ConnectionParameters(
                  metadata.getServerHost(),
                  Optional.ofNullable(metadata.getServerPort()),
                  Optional.ofNullable(tlsOptions),
                  Optional.ofNullable(metadata.getTimeout())));

      final String secret =
          connection.fetchKey(
              new KeyDefinition(
                  metadata.getKeyPath(),
                  Optional.ofNullable(metadata.getKeyName()),
                  metadata.getToken()));
      final BLSKeyPair keyPair =
          new BLSKeyPair(BLSSecretKey.fromBytes(Bytes.fromHexString(secret)));
      return new BlsArtifactSigner(keyPair);
    } catch (Exception e) {
      throw new SigningMetadataException("Failed to fetch secret from hashicorp vault", e);
    }
  }

  private ArtifactSigner createAzureArtifact(final AzureSigningMetadata metadata) {

    try {
      final AzureVault azureVault = new AzureVault(metadata.getClientId(), metadata.getClientSecret(), metadata.getTenantId(), metadata.getVaultName());

      final String secret = azureVault.fetchSecret(metadata.getSecretName());
      final BLSKeyPair keyPair =
              new BLSKeyPair(BLSSecretKey.fromBytes(Bytes.fromHexString(secret)));
      return new BlsArtifactSigner(keyPair);
    } catch (Exception e) {
      throw new SigningMetadataException("Failed to fetch secret from azure vault", e);
    }
  }

  private String loadPassword(final Path passwordFile) {
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

  private Path makeRelativePathAbsolute(final Path path) {
    return path.isAbsolute() ? path : configsDirectory.resolve(path);
  }
}
