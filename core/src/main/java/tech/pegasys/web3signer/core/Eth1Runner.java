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
package tech.pegasys.web3signer.core;

import static tech.pegasys.web3signer.core.service.http.OpenApiOperationsId.ETH1_LIST;
import static tech.pegasys.web3signer.core.service.http.OpenApiOperationsId.ETH1_SIGN;
import static tech.pegasys.web3signer.core.signing.KeyType.SECP256K1;

import tech.pegasys.signers.hashicorp.HashicorpConnectionFactory;
import tech.pegasys.signers.secp256k1.azure.AzureKeyVaultSignerFactory;
import tech.pegasys.web3signer.core.config.Config;
import tech.pegasys.web3signer.core.multikey.DefaultArtifactSignerProvider;
import tech.pegasys.web3signer.core.multikey.SignerLoader;
import tech.pegasys.web3signer.core.multikey.metadata.Secp256k1ArtifactSignerFactory;
import tech.pegasys.web3signer.core.multikey.metadata.parser.YamlSignerParser;
import tech.pegasys.web3signer.core.service.http.handlers.LogErrorHandler;
import tech.pegasys.web3signer.core.service.http.handlers.signing.Eth1SignForIdentifierHandler;
import tech.pegasys.web3signer.core.service.http.handlers.signing.SignerForIdentifier;
import tech.pegasys.web3signer.core.service.http.metrics.HttpApiMetrics;
import tech.pegasys.web3signer.core.signing.ArtifactSigner;
import tech.pegasys.web3signer.core.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.core.signing.EthSecpArtifactSigner;
import tech.pegasys.web3signer.core.signing.SecpArtifactSignature;

import java.util.Collection;
import java.util.List;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.impl.BlockingHandlerDecorator;
import org.hyperledger.besu.plugin.services.MetricsSystem;

public class Eth1Runner extends Runner {

  public Eth1Runner(final Config config) {
    super(config);
  }

  @Override
  protected String getOpenApiSpecResource() {
    return "openapi/web3signer-eth1.yaml";
  }

  @Override
  protected Router populateRouter(final Context context) {
    final ArtifactSignerProvider signerProvider = loadSigners(config, context.getVertx());
    incSignerLoadCount(context.getMetricsSystem(), signerProvider.availableIdentifiers().size());

    final OpenAPI3RouterFactory routerFactory = context.getRouterFactory();
    final LogErrorHandler errorHandler = context.getErrorHandler();

    addPublicKeysListHandler(
        routerFactory,
        signerProvider.availableIdentifiers(),
        ETH1_LIST.name(),
        context.getErrorHandler());

    final SignerForIdentifier<SecpArtifactSignature> secpSigner =
        new SignerForIdentifier<>(signerProvider, this::formatSecpSignature, SECP256K1);
    routerFactory.addHandlerByOperationId(
        ETH1_SIGN.name(),
        new BlockingHandlerDecorator(
            new Eth1SignForIdentifierHandler(
                secpSigner, new HttpApiMetrics(context.getMetricsSystem(), SECP256K1)),
            false));
    routerFactory.addFailureHandlerByOperationId(ETH1_SIGN.name(), errorHandler);

    return context.getRouterFactory().getRouter();
  }

  private ArtifactSignerProvider loadSigners(final Config config, final Vertx vertx) {
    final AzureKeyVaultSignerFactory azureFactory = new AzureKeyVaultSignerFactory();
    final HashicorpConnectionFactory hashicorpConnectionFactory =
        new HashicorpConnectionFactory(vertx);

    final Secp256k1ArtifactSignerFactory ethSecpArtifactSignerFactory =
        new Secp256k1ArtifactSignerFactory(
            hashicorpConnectionFactory,
            config.getKeyConfigPath(),
            azureFactory,
            EthSecpArtifactSigner::new,
            true);

    final Collection<ArtifactSigner> signers =
        SignerLoader.load(
            config.getKeyConfigPath(),
            "yaml",
            new YamlSignerParser(List.of(ethSecpArtifactSignerFactory)));

    return DefaultArtifactSignerProvider.create(signers);
  }

  private String formatSecpSignature(final SecpArtifactSignature signature) {
    return SecpArtifactSignature.toBytes(signature).toHexString();
  }
}
