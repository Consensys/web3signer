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
package tech.pegasys.web3signer.core.multikey.metadata;

import tech.pegasys.signers.bls.keystore.KeyStore;
import tech.pegasys.signers.bls.keystore.KeyStoreLoader;
import tech.pegasys.signers.bls.keystore.KeyStoreValidationException;
import tech.pegasys.signers.bls.keystore.model.KeyStoreData;
import tech.pegasys.signers.hashicorp.HashicorpConnectionFactory;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.web3signer.core.metrics.Web3SignerMetricCategory;
import tech.pegasys.web3signer.core.signing.ArtifactSigner;
import tech.pegasys.web3signer.core.signing.FcBlsArtifactSigner;
import tech.pegasys.web3signer.core.signing.KeyType;
import tech.pegasys.web3signer.core.signing.filecoin.FilecoinNetwork;

import java.nio.file.Path;
import java.util.List;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.metrics.LabelledMetric;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer.TimingContext;

public class FCBlsArtifactSignerFactory extends AbstractArtifactSignerFactory {

  private final LabelledMetric<OperationTimer> privateKeyRetrievalTimer;
  private final FilecoinNetwork network;

  public FCBlsArtifactSignerFactory(
      final Path configsDirectory,
      final MetricsSystem metricsSystem,
      final HashicorpConnectionFactory connectionFactory,
      final FilecoinNetwork network) {
    super(connectionFactory, configsDirectory);
    privateKeyRetrievalTimer =
        metricsSystem.createLabelledTimer(
            Web3SignerMetricCategory.SIGNING,
            "private_key_retrieval_time",
            "Time taken to retrieve private key",
            "signer");
    this.network = network;
  }

  @Override
  public List<ArtifactSigner> create(final FileRawSigningMetadata fileRawSigningMetadata) {
    try (TimingContext ignored = privateKeyRetrievalTimer.labels("file-raw").startTimer()) {
      final BLSKeyPair keyPair =
          new BLSKeyPair(BLSSecretKey.fromBytes(fileRawSigningMetadata.getPrivateKeyBytes()));
      return List.of(new FcBlsArtifactSigner(keyPair, network));
    }
  }

  @Override
  public List<ArtifactSigner> create(final FileKeyStoreMetadata fileKeyStoreMetadata) {
    try (TimingContext ignored = privateKeyRetrievalTimer.labels("file-keystore").startTimer()) {
      return createKeystoreArtifact(fileKeyStoreMetadata);
    }
  }

  @Override
  public List<ArtifactSigner> create(final HashicorpSigningMetadata hashicorpMetadata) {
    try (TimingContext ignored = privateKeyRetrievalTimer.labels("hashicorp").startTimer()) {
      final Bytes privateKeyBytes = extractBytesFromVault(hashicorpMetadata);
      final BLSKeyPair keyPair =
          new BLSKeyPair(BLSSecretKey.fromBytes(Bytes32.wrap(privateKeyBytes)));
      return List.of(new FcBlsArtifactSigner(keyPair, network));
    }
  }

  @Override
  public List<ArtifactSigner> create(final AzureSecretSigningMetadata azureSecretSigningMetadata) {
    try (TimingContext ignored = privateKeyRetrievalTimer.labels("azure").startTimer()) {
      final Bytes privateKeyBytes = extractBytesFromVault(azureSecretSigningMetadata);
      final BLSKeyPair keyPair =
          new BLSKeyPair(BLSSecretKey.fromBytes(Bytes32.wrap(privateKeyBytes)));
      return List.of(new FcBlsArtifactSigner(keyPair, network));
    }
  }

  @Override
  public List<ArtifactSigner> create(final YubiHsm2SigningMetadata yubiHsm2SigningMetadata) {
    try (TimingContext ignored = privateKeyRetrievalTimer.labels("yubihsm2").startTimer()) {
      final Bytes privateKeyBytes = extractBytesFromVault(yubiHsm2SigningMetadata);
      final BLSKeyPair keyPair =
          new BLSKeyPair(BLSSecretKey.fromBytes(Bytes32.wrap(privateKeyBytes)));
      return List.of(new FcBlsArtifactSigner(keyPair, network));
    }
  }

  private List<ArtifactSigner> createKeystoreArtifact(
      final FileKeyStoreMetadata fileKeyStoreMetadata) {
    final Path keystoreFile = makeRelativePathAbsolute(fileKeyStoreMetadata.getKeystoreFile());
    final Path keystorePasswordFile =
        makeRelativePathAbsolute(fileKeyStoreMetadata.getKeystorePasswordFile());
    try {
      final KeyStoreData keyStoreData = KeyStoreLoader.loadFromFile(keystoreFile);
      final String password = loadPassword(keystorePasswordFile);
      final Bytes privateKey = KeyStore.decrypt(password, keyStoreData);
      final BLSKeyPair keyPair = new BLSKeyPair(BLSSecretKey.fromBytes(Bytes32.wrap(privateKey)));
      return List.of(new FcBlsArtifactSigner(keyPair, network));
    } catch (final KeyStoreValidationException e) {
      throw new SigningMetadataException(e.getMessage(), e);
    }
  }

  @Override
  public KeyType getKeyType() {
    return KeyType.BLS;
  }
}
