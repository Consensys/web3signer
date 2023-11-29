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

import static tech.pegasys.web3signer.core.config.HealthCheckNames.KEYS_CHECK_AWS_BULK_LOADING;
import static tech.pegasys.web3signer.core.config.HealthCheckNames.KEYS_CHECK_AZURE_BULK_LOADING;
import static tech.pegasys.web3signer.core.config.HealthCheckNames.KEYS_CHECK_V3_KEYSTORES_BULK_LOADING;
import static tech.pegasys.web3signer.signing.KeyType.SECP256K1;

import tech.pegasys.web3signer.core.config.BaseConfig;
import tech.pegasys.web3signer.core.config.Eth1Config;
import tech.pegasys.web3signer.core.service.DownstreamPathCalculator;
import tech.pegasys.web3signer.core.service.VertxRequestTransmitter;
import tech.pegasys.web3signer.core.service.VertxRequestTransmitterFactory;
import tech.pegasys.web3signer.core.service.http.handlers.LogErrorHandler;
import tech.pegasys.web3signer.core.service.http.handlers.signing.Eth1SignForIdentifierHandler;
import tech.pegasys.web3signer.core.service.http.handlers.signing.SignerForIdentifier;
import tech.pegasys.web3signer.core.service.http.metrics.HttpApiMetrics;
import tech.pegasys.web3signer.core.service.jsonrpc.JsonDecoder;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.Eth1AccountsHandler;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.HttpResponseFactory;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.JsonRpcErrorHandler;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.JsonRpcHandler;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.PassThroughHandler;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.RequestMapper;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.internalresponse.EthSignResultProvider;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.internalresponse.EthSignTransactionResultProvider;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.internalresponse.EthSignTypedDataResultProvider;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.internalresponse.InternalResponseHandler;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.sendtransaction.SendTransactionHandler;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.sendtransaction.transaction.TransactionFactory;
import tech.pegasys.web3signer.keystorage.azure.AzureKeyVault;
import tech.pegasys.web3signer.keystorage.common.MappedResults;
import tech.pegasys.web3signer.keystorage.hashicorp.HashicorpConnectionFactory;
import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.signing.EthSecpArtifactSigner;
import tech.pegasys.web3signer.signing.SecpArtifactSignature;
import tech.pegasys.web3signer.signing.bulkloading.SecpAwsBulkLoader;
import tech.pegasys.web3signer.signing.bulkloading.SecpAzureBulkLoader;
import tech.pegasys.web3signer.signing.bulkloading.SecpV3KeystoresBulkLoader;
import tech.pegasys.web3signer.signing.config.AzureKeyVaultFactory;
import tech.pegasys.web3signer.signing.config.AzureKeyVaultParameters;
import tech.pegasys.web3signer.signing.config.DefaultArtifactSignerProvider;
import tech.pegasys.web3signer.signing.config.KeystoresParameters;
import tech.pegasys.web3signer.signing.config.SecpArtifactSignerProviderAdapter;
import tech.pegasys.web3signer.signing.config.SignerLoader;
import tech.pegasys.web3signer.signing.config.metadata.Secp256k1ArtifactSignerFactory;
import tech.pegasys.web3signer.signing.config.metadata.interlock.InterlockKeyProvider;
import tech.pegasys.web3signer.signing.config.metadata.parser.YamlMapperFactory;
import tech.pegasys.web3signer.signing.config.metadata.parser.YamlSignerParser;
import tech.pegasys.web3signer.signing.config.metadata.yubihsm.YubiHsmOpaqueDataProvider;
import tech.pegasys.web3signer.signing.secp256k1.aws.AwsKmsSignerFactory;
import tech.pegasys.web3signer.signing.secp256k1.aws.CachedAwsKmsClientFactory;
import tech.pegasys.web3signer.signing.secp256k1.azure.AzureHttpClientFactory;
import tech.pegasys.web3signer.signing.secp256k1.azure.AzureKeyVaultSignerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;
import io.vertx.ext.web.impl.BlockingHandlerDecorator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperledger.besu.plugin.services.MetricsSystem;

public class Eth1Runner extends Runner {
  public static final String PUBLIC_KEYS_PATH = "/api/v1/eth1/publicKeys";
  public static final String ROOT_PATH = "/";
  public static final String SIGN_PATH = "/api/v1/eth1/sign/:identifier";
  private static final Logger LOG = LogManager.getLogger();
  private final Eth1Config eth1Config;
  private final HttpResponseFactory responseFactory = new HttpResponseFactory();

  public Eth1Runner(final BaseConfig baseConfig, final Eth1Config eth1Config) {
    super(baseConfig);
    this.eth1Config = eth1Config;
  }

  @Override
  protected void populateRouter(final Context context) {
    final Router router = context.getRouter();
    final LogErrorHandler errorHandler = context.getErrorHandler();
    final ArtifactSignerProvider signerProvider = context.getArtifactSignerProvider();

    addPublicKeysListHandler(router, signerProvider, PUBLIC_KEYS_PATH, context.getErrorHandler());

    final SignerForIdentifier<SecpArtifactSignature> secpSigner =
        new SignerForIdentifier<>(signerProvider, this::formatSecpSignature, SECP256K1);
    router
        .route(HttpMethod.POST, SIGN_PATH)
        .handler(
            new BlockingHandlerDecorator(
                new Eth1SignForIdentifierHandler(
                    secpSigner,
                    new HttpApiMetrics(context.getMetricsSystem(), SECP256K1, signerProvider)),
                false))
        .failureHandler(errorHandler);

    final ArtifactSignerProvider secpArtifactSignerProvider =
        new SecpArtifactSignerProviderAdapter(signerProvider);

    loadSignerProvider(secpArtifactSignerProvider);

    // The order of the elements in the list DO matter
    addReloadHandler(
        router, List.of(signerProvider, secpArtifactSignerProvider), context.getErrorHandler());

    final DownstreamPathCalculator downstreamPathCalculator =
        new DownstreamPathCalculator(eth1Config.getDownstreamHttpPath());

    final WebClientOptions webClientOptions =
        new WebClientOptionsFactory().createWebClientOptions(eth1Config);
    final HttpClient downStreamConnection = context.getVertx().createHttpClient(webClientOptions);

    final VertxRequestTransmitterFactory transmitterFactory =
        responseBodyHandler ->
            new VertxRequestTransmitter(
                context.getVertx(),
                downStreamConnection,
                eth1Config.getDownstreamHttpRequestTimeout(),
                downstreamPathCalculator,
                responseBodyHandler);

    final JsonDecoder jsonDecoder = createJsonDecoder();
    final PassThroughHandler passThroughHandler =
        new PassThroughHandler(transmitterFactory, jsonDecoder);

    final RequestMapper requestMapper =
        createRequestMapper(
            transmitterFactory,
            secpArtifactSignerProvider,
            jsonDecoder,
            eth1Config.getChainId().id());

    router
        .route(HttpMethod.POST, ROOT_PATH)
        .produces(Runner.JSON)
        .handler(ResponseContentTypeHandler.create())
        .handler(BodyHandler.create())
        .failureHandler(new JsonRpcErrorHandler(new HttpResponseFactory()))
        .blockingHandler(new JsonRpcHandler(responseFactory, requestMapper, jsonDecoder), false);

    router.route().handler(BodyHandler.create()).handler(passThroughHandler);
  }

  @Override
  protected ArtifactSignerProvider createArtifactSignerProvider(
      final Vertx vertx, final MetricsSystem metricsSystem) {
    return new DefaultArtifactSignerProvider(
        () -> {
          final List<ArtifactSigner> signers = new ArrayList<>();
          final AzureKeyVaultFactory azureKeyVaultFactory = new AzureKeyVaultFactory();
          final AzureHttpClientFactory azureHttpClientFactory = new AzureHttpClientFactory();
          registerClose(azureKeyVaultFactory::close);
          final AzureKeyVaultSignerFactory azureSignerFactory =
              new AzureKeyVaultSignerFactory(azureKeyVaultFactory, azureHttpClientFactory);
          final CachedAwsKmsClientFactory cachedAwsKmsClientFactory =
              new CachedAwsKmsClientFactory(eth1Config.getAwsKmsClientCacheSize());
          final AwsKmsSignerFactory awsKmsSignerFactory =
              new AwsKmsSignerFactory(cachedAwsKmsClientFactory, true);
          signers.addAll(
              loadSignersFromKeyConfigFiles(
                      vertx, azureKeyVaultFactory, azureSignerFactory, awsKmsSignerFactory)
                  .getValues());
          signers.addAll(
              bulkLoadSigners(
                      azureKeyVaultFactory,
                      azureSignerFactory,
                      cachedAwsKmsClientFactory,
                      awsKmsSignerFactory)
                  .getValues());
          return signers;
        });
  }

  private MappedResults<ArtifactSigner> loadSignersFromKeyConfigFiles(
      final Vertx vertx,
      final AzureKeyVaultFactory azureKeyVaultFactory,
      final AzureKeyVaultSignerFactory azureSignerFactory,
      final AwsKmsSignerFactory awsKmsSignerFactory) {
    final HashicorpConnectionFactory hashicorpConnectionFactory = new HashicorpConnectionFactory();
    try (final InterlockKeyProvider interlockKeyProvider = new InterlockKeyProvider(vertx);
        final YubiHsmOpaqueDataProvider yubiHsmOpaqueDataProvider =
            new YubiHsmOpaqueDataProvider()) {

      final Secp256k1ArtifactSignerFactory ethSecpArtifactSignerFactory =
          new Secp256k1ArtifactSignerFactory(
              hashicorpConnectionFactory,
              baseConfig.getKeyConfigPath(),
              azureSignerFactory,
              interlockKeyProvider,
              yubiHsmOpaqueDataProvider,
              EthSecpArtifactSigner::new,
              azureKeyVaultFactory,
              awsKmsSignerFactory,
              true);

      return new SignerLoader(baseConfig.keystoreParallelProcessingEnabled())
          .load(
              baseConfig.getKeyConfigPath(),
              "yaml",
              new YamlSignerParser(
                  List.of(ethSecpArtifactSignerFactory),
                  YamlMapperFactory.createYamlMapper(baseConfig.getKeyStoreConfigFileMaxSize())));
    }
  }

  private MappedResults<ArtifactSigner> bulkLoadSigners(
      final AzureKeyVaultFactory azureKeyVaultFactory,
      final AzureKeyVaultSignerFactory azureSignerFactory,
      final CachedAwsKmsClientFactory cachedAwsKmsClientFactory,
      final AwsKmsSignerFactory awsKmsSignerFactory) {
    MappedResults<ArtifactSigner> results = MappedResults.newSetInstance();
    if (eth1Config.getAzureKeyVaultConfig().isAzureKeyVaultEnabled()) {
      results =
          MappedResults.merge(results, bulkLoadAzureKeys(azureKeyVaultFactory, azureSignerFactory));
    }
    if (eth1Config.getAwsVaultParameters().isEnabled()) {
      results =
          MappedResults.merge(
              results, bulkLoadAwsKeys(cachedAwsKmsClientFactory, awsKmsSignerFactory));
    }

    // v3 bulk loading
    results = MappedResults.merge(results, bulkloadV3Keystores());

    return results;
  }

  private MappedResults<ArtifactSigner> bulkLoadAzureKeys(
      AzureKeyVaultFactory azureKeyVaultFactory, AzureKeyVaultSignerFactory azureSignerFactory) {
    LOG.info("Bulk loading keys from Azure key vault ... ");
    final AzureKeyVaultParameters azureKeyVaultConfig = eth1Config.getAzureKeyVaultConfig();
    final AzureKeyVault azureKeyVault =
        azureKeyVaultFactory.createAzureKeyVault(
            azureKeyVaultConfig.getClientId(),
            azureKeyVaultConfig.getClientSecret(),
            azureKeyVaultConfig.getKeyVaultName(),
            azureKeyVaultConfig.getTenantId(),
            azureKeyVaultConfig.getAuthenticationMode(),
            azureKeyVaultConfig.getTimeout());
    final SecpAzureBulkLoader secpAzureBulkLoader =
        new SecpAzureBulkLoader(azureKeyVault, azureSignerFactory);
    final MappedResults<ArtifactSigner> azureResult = secpAzureBulkLoader.load(azureKeyVaultConfig);
    LOG.info(
        "Keys loaded from Azure: [{}], with error count: [{}]",
        azureResult.getValues().size(),
        azureResult.getErrorCount());
    registerSignerLoadingHealthCheck(KEYS_CHECK_AZURE_BULK_LOADING, azureResult);
    return azureResult;
  }

  private MappedResults<ArtifactSigner> bulkLoadAwsKeys(
      CachedAwsKmsClientFactory cachedAwsKmsClientFactory,
      AwsKmsSignerFactory awsKmsSignerFactory) {
    LOG.info("Bulk loading keys from AWS KMS key vault ... ");
    final SecpAwsBulkLoader secpAwsBulkLoader =
        new SecpAwsBulkLoader(cachedAwsKmsClientFactory, awsKmsSignerFactory);
    final MappedResults<ArtifactSigner> awsResult =
        secpAwsBulkLoader.load(eth1Config.getAwsVaultParameters());
    LOG.info(
        "Keys loaded from AWS: [{}], with error count: [{}]",
        awsResult.getValues().size(),
        awsResult.getErrorCount());
    registerSignerLoadingHealthCheck(KEYS_CHECK_AWS_BULK_LOADING, awsResult);
    return awsResult;
  }

  private MappedResults<ArtifactSigner> bulkloadV3Keystores() {
    final KeystoresParameters v3WalletBLParams = eth1Config.getV3KeystoresBulkLoadParameters();
    if (!v3WalletBLParams.isEnabled()) {
      return MappedResults.newInstance(Collections.emptyList(), 0);
    }

    LOG.info("Bulk loading v3 keystore files ... ");
    final MappedResults<ArtifactSigner> walletResults =
        SecpV3KeystoresBulkLoader.loadV3KeystoresUsingPasswordFileOrDir(
            v3WalletBLParams.getKeystoresPath(),
            v3WalletBLParams.hasKeystoresPasswordFile()
                ? v3WalletBLParams.getKeystoresPasswordFile()
                : v3WalletBLParams.getKeystoresPasswordsPath());
    LOG.info(
        "Keys loaded from v3 keystores files: [{}], with error count: [{}]",
        walletResults.getValues().size(),
        walletResults.getErrorCount());
    registerSignerLoadingHealthCheck(KEYS_CHECK_V3_KEYSTORES_BULK_LOADING, walletResults);
    return walletResults;
  }

  private String formatSecpSignature(final SecpArtifactSignature signature) {
    return SecpArtifactSignature.toBytes(signature).toHexString();
  }

  public static JsonDecoder createJsonDecoder() {
    // Force Transaction Deserialization to fail if missing expected properties
    final ObjectMapper jsonObjectMapper = new ObjectMapper();
    jsonObjectMapper.configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, true);
    jsonObjectMapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, true);

    return new JsonDecoder(jsonObjectMapper);
  }

  private RequestMapper createRequestMapper(
      final VertxRequestTransmitterFactory transmitterFactory,
      final ArtifactSignerProvider signerProviderMappedToEth1Address,
      final JsonDecoder jsonDecoder,
      final long chainId) {
    final PassThroughHandler defaultHandler =
        new PassThroughHandler(transmitterFactory, jsonDecoder);
    final SignerForIdentifier<SecpArtifactSignature> secpSigner =
        new SignerForIdentifier<>(
            signerProviderMappedToEth1Address, this::formatSecpSignature, SECP256K1);
    final TransactionFactory transactionFactory =
        new TransactionFactory(chainId, jsonDecoder, transmitterFactory);
    final SendTransactionHandler sendTransactionHandler =
        new SendTransactionHandler(chainId, transactionFactory, transmitterFactory, secpSigner);

    final RequestMapper requestMapper = new RequestMapper(defaultHandler);
    requestMapper.addHandler(
        "eth_accounts",
        new InternalResponseHandler<>(
            responseFactory,
            new Eth1AccountsHandler(signerProviderMappedToEth1Address::availableIdentifiers)));
    requestMapper.addHandler(
        "eth_sign",
        new InternalResponseHandler<>(responseFactory, new EthSignResultProvider(secpSigner)));
    requestMapper.addHandler(
        "eth_signTypedData",
        new InternalResponseHandler<>(
            responseFactory, new EthSignTypedDataResultProvider(secpSigner)));
    requestMapper.addHandler(
        "eth_signTransaction",
        new InternalResponseHandler<>(
            responseFactory,
            new EthSignTransactionResultProvider(chainId, secpSigner, jsonDecoder)));
    requestMapper.addHandler("eth_sendTransaction", sendTransactionHandler);
    requestMapper.addHandler("eea_sendTransaction", sendTransactionHandler);

    return requestMapper;
  }

  private void loadSignerProvider(final ArtifactSignerProvider signerProvider) {
    try {
      signerProvider.load().get(); // wait for signers to get loaded ...
    } catch (final Exception e) {
      throw new InitializationException(e);
    }
  }

  private void registerSignerLoadingHealthCheck(
      final String name, final MappedResults<ArtifactSigner> result) {
    super.registerHealthCheckProcedure(
        name,
        promise -> {
          final JsonObject statusJson =
              new JsonObject()
                  .put("keys-loaded", result.getValues().size())
                  .put("error-count", result.getErrorCount());
          promise.complete(
              result.getErrorCount() > 0 ? Status.KO(statusJson) : Status.OK(statusJson));
        });
  }
}
