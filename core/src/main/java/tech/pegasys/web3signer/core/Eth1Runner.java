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
import static tech.pegasys.web3signer.core.service.http.OpenApiOperationsId.RELOAD;
import static tech.pegasys.web3signer.signing.KeyType.SECP256K1;

import tech.pegasys.signers.hashicorp.HashicorpConnectionFactory;
import tech.pegasys.signers.secp256k1.azure.AzureKeyVaultSignerFactory;
import tech.pegasys.web3signer.core.config.Config;
import tech.pegasys.web3signer.core.service.DownstreamPathCalculator;
import tech.pegasys.web3signer.core.service.PassThroughHandler;
import tech.pegasys.web3signer.core.service.VertxRequestTransmitter;
import tech.pegasys.web3signer.core.service.VertxRequestTransmitterFactory;
import tech.pegasys.web3signer.core.service.http.handlers.LogErrorHandler;
import tech.pegasys.web3signer.core.service.http.handlers.signing.Eth1SignForIdentifierHandler;
import tech.pegasys.web3signer.core.service.http.handlers.signing.SignerForIdentifier;
import tech.pegasys.web3signer.core.service.http.metrics.HttpApiMetrics;
import tech.pegasys.web3signer.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.signing.EthSecpArtifactSigner;
import tech.pegasys.web3signer.signing.SecpArtifactSignature;
import tech.pegasys.web3signer.signing.config.DefaultArtifactSignerProvider;
import tech.pegasys.web3signer.signing.config.SignerLoader;
import tech.pegasys.web3signer.signing.config.metadata.Secp256k1ArtifactSignerFactory;
import tech.pegasys.web3signer.signing.config.metadata.interlock.InterlockKeyProvider;
import tech.pegasys.web3signer.signing.config.metadata.parser.YamlMapperFactory;
import tech.pegasys.web3signer.signing.config.metadata.parser.YamlSignerParser;
import tech.pegasys.web3signer.signing.config.metadata.yubihsm.YubiHsmOpaqueDataProvider;

import java.time.Duration;
import java.util.List;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.impl.BlockingHandlerDecorator;
import io.vertx.ext.web.openapi.RouterBuilder;
import org.hyperledger.besu.plugin.services.MetricsSystem;

public class Eth1Runner extends Runner {
  public Eth1Runner(final Config config) {
    super(config);
  }

  @Override
  protected String getOpenApiSpecResource() {
    return "eth1/web3signer.yaml";
  }

  @Override
  protected Router populateRouter(final Context context) {
    final RouterBuilder routerBuilder = context.getRouterBuilder();
    final LogErrorHandler errorHandler = context.getErrorHandler();
    final ArtifactSignerProvider signerProvider = context.getArtifactSignerProvider();

    addPublicKeysListHandler(
        routerBuilder, signerProvider, ETH1_LIST.name(), context.getErrorHandler());

    final SignerForIdentifier<SecpArtifactSignature> secpSigner =
        new SignerForIdentifier<>(signerProvider, this::formatSecpSignature, SECP256K1);
    routerBuilder
        .operation(ETH1_SIGN.name())
        .handler(
            new BlockingHandlerDecorator(
                new Eth1SignForIdentifierHandler(
                    secpSigner,
                    new HttpApiMetrics(context.getMetricsSystem(), SECP256K1, signerProvider)),
                false))
        .failureHandler(errorHandler);

    addReloadHandler(routerBuilder, signerProvider, RELOAD.name(), context.getErrorHandler());

    final Duration httpRequestTimeout = Duration.ofSeconds(1); // TODO Use value from config
    final String downstreamHttpPath = "/"; // TODO use value from config

    final DownstreamPathCalculator downstreamPathCalculator =
        new DownstreamPathCalculator(downstreamHttpPath);
    // TODO use values from config
    final WebClientOptions clientOptions =
        new WebClientOptions()
            .setDefaultPort(8545)
            .setDefaultHost("127.0.0.1")
            .setTryUseCompression(true);
    final HttpClient downStreamConnection = context.getVertx().createHttpClient(clientOptions);

    final VertxRequestTransmitterFactory transmitterFactory =
        responseBodyHandler ->
            new VertxRequestTransmitter(
                context.getVertx(),
                downStreamConnection,
                httpRequestTimeout,
                downstreamPathCalculator,
                responseBodyHandler);

    routerBuilder.rootHandler(new PassThroughHandler(transmitterFactory));

    return context.getRouterBuilder().createRouter();
  }

  @Override
  protected ArtifactSignerProvider createArtifactSignerProvider(
      final Vertx vertx, final MetricsSystem metricsSystem) {
    return new DefaultArtifactSignerProvider(
        () -> {
          final AzureKeyVaultSignerFactory azureFactory = new AzureKeyVaultSignerFactory();
          final HashicorpConnectionFactory hashicorpConnectionFactory =
              new HashicorpConnectionFactory();
          try (final InterlockKeyProvider interlockKeyProvider = new InterlockKeyProvider(vertx);
              final YubiHsmOpaqueDataProvider yubiHsmOpaqueDataProvider =
                  new YubiHsmOpaqueDataProvider()) {
            final Secp256k1ArtifactSignerFactory ethSecpArtifactSignerFactory =
                new Secp256k1ArtifactSignerFactory(
                    hashicorpConnectionFactory,
                    config.getKeyConfigPath(),
                    azureFactory,
                    interlockKeyProvider,
                    yubiHsmOpaqueDataProvider,
                    EthSecpArtifactSigner::new,
                    true);

            return new SignerLoader()
                .load(
                    config.getKeyConfigPath(),
                    "yaml",
                    new YamlSignerParser(
                        List.of(ethSecpArtifactSignerFactory),
                        YamlMapperFactory.createYamlMapper(config.getKeyStoreConfigFileMaxSize())))
                .getValues();
          }
        });
  }

  private String formatSecpSignature(final SecpArtifactSignature signature) {
    return SecpArtifactSignature.toBytes(signature).toHexString();
  }
}
