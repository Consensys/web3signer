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
import tech.pegasys.eth2signer.crypto.KeyPair;
import tech.pegasys.eth2signer.crypto.SecretKey;
import tech.pegasys.signers.bls.keystore.KeyStore;
import tech.pegasys.signers.bls.keystore.KeyStoreLoader;
import tech.pegasys.signers.bls.keystore.KeyStoreValidationException;
import tech.pegasys.signers.bls.keystore.model.KeyStoreData;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

import com.google.common.io.Files;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.metrics.LabelledMetric;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer.TimingContext;

public class ArtifactSignerFactory {

  private final LabelledMetric<OperationTimer> privateKeyRetrievalTimer;
  private Path configsDirectory;

  public ArtifactSignerFactory(final Path configsDirectory, final MetricsSystem metricsSystem) {
    this.configsDirectory = configsDirectory;
    privateKeyRetrievalTimer =
        metricsSystem.createLabelledTimer(
            Eth2SignerMetricCategory.SIGNING,
            "private_key_retrieval_time",
            "Time taken to retrieve private key",
            "signer");
  }

  public ArtifactSigner create(final FileRawSigningMetadata fileRawSigningMetadata) {
    try (TimingContext ignored = privateKeyRetrievalTimer.labels("file-raw").startTimer()) {
      return new ArtifactSigner(new KeyPair(fileRawSigningMetadata.getPrivateKey()));
    }
  }

  public ArtifactSigner create(final FileKeyStoreMetadata fileKeyStoreMetadata) {
    try (TimingContext ignored = privateKeyRetrievalTimer.labels("file-keystore").startTimer()) {
      return createKeystoreArtifact(fileKeyStoreMetadata);
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
      final KeyPair keyPair = new KeyPair(SecretKey.fromBytes(privateKey));
      return new ArtifactSigner(keyPair);
    } catch (final KeyStoreValidationException e) {
      throw new SigningMetadataException(e.getMessage(), e);
    }
  }

  private String loadPassword(final Path passwordFile) {
    final String password;
    try {
      password = Files.asCharSource(passwordFile.toFile(), UTF_8).readFirstLine();
      if (password == null || password.isBlank()) {
        throw new SigningMetadataException("Keystore password cannot be empty: " + passwordFile);
      }
    } catch (final FileNotFoundException e) {
      throw new SigningMetadataException("Keystore password file not found: " + passwordFile, e);
    } catch (final IOException e) {
      final String errorMessage =
          format(
              "Unexpected IO error while reading keystore password file [%s]: %s",
              passwordFile, e.getMessage());
      throw new SigningMetadataException(errorMessage, e);
    }
    return password;
  }

  private Path makeRelativePathAbsolute(final Path path) {
    return path.isAbsolute() ? path : configsDirectory.resolve(path);
  }
}
