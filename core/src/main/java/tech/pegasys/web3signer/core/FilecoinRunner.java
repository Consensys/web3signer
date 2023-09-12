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

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS;
import static com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static tech.pegasys.web3signer.core.service.http.handlers.ContentTypes.JSON_UTF_8;

import tech.pegasys.web3signer.core.config.BaseConfig;
import tech.pegasys.web3signer.core.service.jsonrpc.FcJsonRpc;
import tech.pegasys.web3signer.core.service.jsonrpc.FcJsonRpcMetrics;
import tech.pegasys.web3signer.core.service.jsonrpc.FilecoinJsonRpcModule;
import tech.pegasys.web3signer.keystorage.aws.AwsSecretsManagerProvider;
import tech.pegasys.web3signer.keystorage.hashicorp.HashicorpConnectionFactory;
import tech.pegasys.web3signer.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.signing.FcBlsArtifactSigner;
import tech.pegasys.web3signer.signing.FcSecpArtifactSigner;
import tech.pegasys.web3signer.signing.config.AzureKeyVaultFactory;
import tech.pegasys.web3signer.signing.config.DefaultArtifactSignerProvider;
import tech.pegasys.web3signer.signing.config.SignerLoader;
import tech.pegasys.web3signer.signing.config.metadata.AbstractArtifactSignerFactory;
import tech.pegasys.web3signer.signing.config.metadata.BlsArtifactSignerFactory;
import tech.pegasys.web3signer.signing.config.metadata.Secp256k1ArtifactSignerFactory;
import tech.pegasys.web3signer.signing.config.metadata.interlock.InterlockKeyProvider;
import tech.pegasys.web3signer.signing.config.metadata.parser.YamlMapperFactory;
import tech.pegasys.web3signer.signing.config.metadata.parser.YamlSignerParser;
import tech.pegasys.web3signer.signing.config.metadata.yubihsm.YubiHsmOpaqueDataProvider;
import tech.pegasys.web3signer.signing.filecoin.FilecoinNetwork;
import tech.pegasys.web3signer.signing.secp256k1.aws.AwsKmsSignerFactory;
import tech.pegasys.web3signer.signing.secp256k1.aws.CachedAwsKmsClientFactory;
import tech.pegasys.web3signer.signing.secp256k1.azure.AzureHttpClientFactory;
import tech.pegasys.web3signer.signing.secp256k1.azure.AzureKeyVaultSignerFactory;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.arteam.simplejsonrpc.server.JsonRpcServer;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import org.hyperledger.besu.plugin.services.MetricsSystem;

public class FilecoinRunner extends Runner {
  private static final int AWS_CACHE_MAXIMUM_SIZE = 1;
  private static final String FC_JSON_RPC_PATH = "/rpc/v0";
  private final FilecoinNetwork network;
  private final long awsKmsClientCacheSize;

  public FilecoinRunner(
      final BaseConfig baseConfig,
      final FilecoinNetwork network,
      final long awsKmsClientCacheSize) {
    super(baseConfig);
    this.network = network;
    this.awsKmsClientCacheSize = awsKmsClientCacheSize;
  }

  @Override
  protected void populateRouter(final Context context) {
    addReloadHandler(
        context.getRouter(),
        List.of(context.getArtifactSignerProvider()),
        context.getErrorHandler());

    registerFilecoinJsonRpcRoute(
        context.getRouter(), context.getMetricsSystem(), context.getArtifactSignerProvider());
  }

  private Router registerFilecoinJsonRpcRoute(
      final Router router,
      final MetricsSystem metricsSystem,
      final ArtifactSignerProvider fcSigners) {

    final FcJsonRpcMetrics fcJsonRpcMetrics = new FcJsonRpcMetrics(metricsSystem);
    final FcJsonRpc fileCoinJsonRpc = new FcJsonRpc(fcSigners, fcJsonRpcMetrics);
    final ObjectMapper mapper =
        JsonMapper.builder()
            .enable(ACCEPT_CASE_INSENSITIVE_ENUMS)
            .enable(ACCEPT_CASE_INSENSITIVE_PROPERTIES)
            .disable(FAIL_ON_UNKNOWN_PROPERTIES)
            .addModule(new FilecoinJsonRpcModule())
            .build();
    final JsonRpcServer jsonRpcServer = JsonRpcServer.withMapper(mapper);

    router
        .post(FC_JSON_RPC_PATH)
        .handler(fcJsonRpcMetrics::incTotalFilecoinRequests)
        .blockingHandler(
            routingContext -> {
              final String body = routingContext.body().asString();
              final String jsonRpcResponse = jsonRpcServer.handle(body, fileCoinJsonRpc);
              routingContext.response().putHeader(CONTENT_TYPE, JSON_UTF_8).end(jsonRpcResponse);
            },
            false);

    return router;
  }

  @Override
  protected ArtifactSignerProvider createArtifactSignerProvider(
      final Vertx vertx, final MetricsSystem metricsSystem) {
    return new DefaultArtifactSignerProvider(
        () -> {
          final AzureKeyVaultFactory azureKeyVaultFactory = new AzureKeyVaultFactory();
          registerClose(azureKeyVaultFactory::close);
          final AzureHttpClientFactory azureHttpClientFactory = new AzureHttpClientFactory();
          final AzureKeyVaultSignerFactory azureSignerFactory =
              new AzureKeyVaultSignerFactory(azureKeyVaultFactory, azureHttpClientFactory);
          final CachedAwsKmsClientFactory cachedAwsKmsClientFactory =
              new CachedAwsKmsClientFactory(awsKmsClientCacheSize);
          final boolean applySha3Hash = false;
          final AwsKmsSignerFactory awsKmsSignerFactory =
              new AwsKmsSignerFactory(cachedAwsKmsClientFactory, applySha3Hash);

          try (final HashicorpConnectionFactory hashicorpConnectionFactory =
                  new HashicorpConnectionFactory();
              final InterlockKeyProvider interlockKeyProvider = new InterlockKeyProvider(vertx);
              final YubiHsmOpaqueDataProvider yubiHsmOpaqueDataProvider =
                  new YubiHsmOpaqueDataProvider();
              final AwsSecretsManagerProvider awsSecretsManagerProvider =
                  new AwsSecretsManagerProvider(AWS_CACHE_MAXIMUM_SIZE)) {

            final AbstractArtifactSignerFactory blsArtifactSignerFactory =
                new BlsArtifactSignerFactory(
                    baseConfig.getKeyConfigPath(),
                    metricsSystem,
                    hashicorpConnectionFactory,
                    interlockKeyProvider,
                    yubiHsmOpaqueDataProvider,
                    awsSecretsManagerProvider,
                    (args) -> new FcBlsArtifactSigner(args.getKeyPair(), network),
                    azureKeyVaultFactory);

            final AbstractArtifactSignerFactory secpArtifactSignerFactory =
                new Secp256k1ArtifactSignerFactory(
                    hashicorpConnectionFactory,
                    baseConfig.getKeyConfigPath(),
                    azureSignerFactory,
                    interlockKeyProvider,
                    yubiHsmOpaqueDataProvider,
                    signer -> new FcSecpArtifactSigner(signer, network),
                    azureKeyVaultFactory,
                    awsKmsSignerFactory,
                    applySha3Hash);

            return new SignerLoader(baseConfig.keystoreParallelProcessingEnabled())
                .load(
                    baseConfig.getKeyConfigPath(),
                    "yaml",
                    new YamlSignerParser(
                        List.of(blsArtifactSignerFactory, secpArtifactSignerFactory),
                        YamlMapperFactory.createYamlMapper(
                            baseConfig.getKeyStoreConfigFileMaxSize())))
                .getValues();
          }
        });
  }
}
