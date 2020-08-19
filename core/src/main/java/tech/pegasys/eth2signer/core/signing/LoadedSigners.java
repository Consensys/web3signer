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
package tech.pegasys.eth2signer.core.signing;

import static java.util.Collections.emptyList;

import tech.pegasys.eth2signer.core.SignerTypes;
import tech.pegasys.eth2signer.core.config.Config;
import tech.pegasys.eth2signer.core.multikey.DefaultArtifactSignerProvider;
import tech.pegasys.eth2signer.core.multikey.SignerLoader;
import tech.pegasys.eth2signer.core.multikey.metadata.BlsArtifactSignerFactory;
import tech.pegasys.eth2signer.core.multikey.metadata.Secp256k1ArtifactSignerFactory;
import tech.pegasys.eth2signer.core.multikey.metadata.parser.YamlSignerParser;
import tech.pegasys.signers.hashicorp.HashicorpConnectionFactory;
import tech.pegasys.signers.secp256k1.azure.AzureKeyVaultSignerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.vertx.core.Vertx;
import org.hyperledger.besu.plugin.services.MetricsSystem;

public class LoadedSigners {

  private final Map<SignerTypes, List<ArtifactSigner>> signersByType;

  private LoadedSigners(final Map<SignerTypes, List<ArtifactSigner>> signersByType) {
    this.signersByType = signersByType;
  }

  public static LoadedSigners loadFrom(
      final Config config, final Vertx vertx, final MetricsSystem metricsSystem) {

    final AzureKeyVaultSignerFactory azureFactory = new AzureKeyVaultSignerFactory();
    final HashicorpConnectionFactory hashicorpConnectionFactory =
        new HashicorpConnectionFactory(vertx);

    final BlsArtifactSignerFactory blsArtifactSignerFactory =
        new BlsArtifactSignerFactory(
            config.getKeyConfigPath(), metricsSystem, hashicorpConnectionFactory);

    final Secp256k1ArtifactSignerFactory ethSecpArtifactSignerFactory =
        new Secp256k1ArtifactSignerFactory(
            hashicorpConnectionFactory,
            config.getKeyConfigPath(),
            azureFactory,
            EthSecpArtifactSigner::new);

    final Secp256k1ArtifactSignerFactory fcSecpArtifactSignerFactory =
        new Secp256k1ArtifactSignerFactory(
            hashicorpConnectionFactory,
            config.getKeyConfigPath(),
            azureFactory,
            signer -> new FcSecpArtifactSigner(signer, config.getFilecoinNetwork()));

    final Collection<ArtifactSigner> signers =
        SignerLoader.load(
            config.getKeyConfigPath(),
            "yaml",
            new YamlSignerParser(
                List.of(
                    blsArtifactSignerFactory,
                    ethSecpArtifactSignerFactory,
                    fcSecpArtifactSignerFactory)));

    return new LoadedSigners(
        signers
            .parallelStream()
            .collect(
                Collectors.groupingBy(
                    i -> {
                      if (i instanceof BlsArtifactSigner) {
                        return SignerTypes.BLS;
                      } else if (i instanceof EthSecpArtifactSigner) {
                        return SignerTypes.ETH_SECP;
                      } else if (i instanceof FcSecpArtifactSigner) {
                        return SignerTypes.FC_SECP;
                      } else if (i instanceof FcBlsArtifactSigner) {
                        return SignerTypes.FC_BLS;
                      } else {
                        throw new IllegalStateException("Loaded an unknown type of ArtifactSigner");
                      }
                    })));
  }

  public ArtifactSignerProvider getBlsSignerProvider() {
    return getSignerProvider(SignerTypes.BLS);
  }

  public ArtifactSignerProvider getEthSignerProvider() {
    return getSignerProvider(SignerTypes.ETH_SECP);
  }

  public ArtifactSignerProvider getFcSecpSignerProvider() {
    return getSignerProvider(SignerTypes.FC_SECP);
  }

  public ArtifactSignerProvider getFcBlsSignerProvider() {
    return getSignerProvider(SignerTypes.FC_BLS);
  }

  private ArtifactSignerProvider getSignerProvider(final SignerTypes type) {
    return DefaultArtifactSignerProvider.create(signersByType.getOrDefault(type, emptyList()));
  }
}
