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

import tech.pegasys.eth2signer.core.multikey.DefaultArtifactSignerProvider;
import tech.pegasys.eth2signer.core.multikey.DirectoryLoader;
import tech.pegasys.eth2signer.core.multikey.metadata.parser.YamlSignerParser;
import tech.pegasys.eth2signer.core.signing.ArtifactSignerProvider;
import tech.pegasys.signers.hashicorp.HashicorpConnectionFactory;
import tech.pegasys.signers.secp256k1.azure.AzureKeyVaultSignerFactory;

import java.nio.file.Path;

import io.vertx.core.Vertx;
import org.hyperledger.besu.plugin.services.MetricsSystem;

public class ArtifactSignerProviderFactory {

  private final MetricsSystem metricsSystem;
  private final AzureKeyVaultSignerFactory azureFactory;
  private final HashicorpConnectionFactory hashicorpConnectionFactory;

  public ArtifactSignerProviderFactory(
      final MetricsSystem metricsSystem,
      final Vertx vertx,
      final AzureKeyVaultSignerFactory azureFactory) {
    this.metricsSystem = metricsSystem;
    this.azureFactory = azureFactory;
    this.hashicorpConnectionFactory = new HashicorpConnectionFactory(vertx);
  }

  public ArtifactSignerProvider createBlsSignerProvider(final Path keyConfigPath) {
    final ArtifactSignerFactory artifactSignerFactory =
        new BlsArtifactSignerFactory(keyConfigPath, metricsSystem, hashicorpConnectionFactory);

    return DefaultArtifactSignerProvider.create(
        DirectoryLoader.loadFromDisk(
            keyConfigPath, "yaml", new YamlSignerParser(artifactSignerFactory)));
  }

  public ArtifactSignerProvider createSecpSignerProvider(final Path keyConfigPath) {
    final ArtifactSignerFactory artifactSignerFactory =
        new Secp256k1ArtifactSignerFactory(hashicorpConnectionFactory, keyConfigPath, azureFactory);

    return DefaultArtifactSignerProvider.create(
        DirectoryLoader.loadFromDisk(
            keyConfigPath, "yaml", new YamlSignerParser(artifactSignerFactory)));
  }
}
