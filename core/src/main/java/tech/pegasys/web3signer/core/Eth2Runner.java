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
import static tech.pegasys.web3signer.core.config.HealthCheckNames.KEYS_CHECK_CONFIG_FILE_LOADING;
import static tech.pegasys.web3signer.core.config.HealthCheckNames.KEYS_CHECK_GCP_BULK_LOADING;
import static tech.pegasys.web3signer.core.config.HealthCheckNames.KEYS_CHECK_KEYSTORE_BULK_LOADING;
import static tech.pegasys.web3signer.core.config.HealthCheckNames.SLASHING_PROTECTION_DB;
import static tech.pegasys.web3signer.signing.KeyType.BLS;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.web3signer.core.config.BaseConfig;
import tech.pegasys.web3signer.core.metrics.SlashingProtectionMetrics;
import tech.pegasys.web3signer.core.service.http.SigningObjectMapperFactory;
import tech.pegasys.web3signer.core.service.http.handlers.HighWatermarkHandler;
import tech.pegasys.web3signer.core.service.http.handlers.LogErrorHandler;
import tech.pegasys.web3signer.core.service.http.handlers.keymanager.delete.DeleteKeystoresHandler;
import tech.pegasys.web3signer.core.service.http.handlers.keymanager.imports.ImportKeystoresHandler;
import tech.pegasys.web3signer.core.service.http.handlers.keymanager.list.ListKeystoresHandler;
import tech.pegasys.web3signer.core.service.http.handlers.signing.SignerForIdentifier;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.Eth2SignForIdentifierHandler;
import tech.pegasys.web3signer.core.service.http.metrics.HttpApiMetrics;
import tech.pegasys.web3signer.keystorage.aws.AwsSecretsManagerProvider;
import tech.pegasys.web3signer.keystorage.azure.AzureKeyVault;
import tech.pegasys.web3signer.keystorage.common.MappedResults;
import tech.pegasys.web3signer.keystorage.hashicorp.HashicorpConnectionFactory;
import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.signing.BlsArtifactSignature;
import tech.pegasys.web3signer.signing.BlsArtifactSigner;
import tech.pegasys.web3signer.signing.FileValidatorManager;
import tech.pegasys.web3signer.signing.KeystoreFileManager;
import tech.pegasys.web3signer.signing.ValidatorManager;
import tech.pegasys.web3signer.signing.bulkloading.BlsAwsBulkLoader;
import tech.pegasys.web3signer.signing.bulkloading.BlsGcpBulkLoader;
import tech.pegasys.web3signer.signing.bulkloading.BlsKeystoreBulkLoader;
import tech.pegasys.web3signer.signing.config.AwsVaultParameters;
import tech.pegasys.web3signer.signing.config.AzureKeyVaultFactory;
import tech.pegasys.web3signer.signing.config.AzureKeyVaultParameters;
import tech.pegasys.web3signer.signing.config.DefaultArtifactSignerProvider;
import tech.pegasys.web3signer.signing.config.GcpSecretManagerParameters;
import tech.pegasys.web3signer.signing.config.KeystoresParameters;
import tech.pegasys.web3signer.signing.config.SignerLoader;
import tech.pegasys.web3signer.signing.config.metadata.AbstractArtifactSignerFactory;
import tech.pegasys.web3signer.signing.config.metadata.BlsArtifactSignerFactory;
import tech.pegasys.web3signer.signing.config.metadata.SignerOrigin;
import tech.pegasys.web3signer.signing.config.metadata.interlock.InterlockKeyProvider;
import tech.pegasys.web3signer.signing.config.metadata.parser.YamlMapperFactory;
import tech.pegasys.web3signer.signing.config.metadata.parser.YamlSignerParser;
import tech.pegasys.web3signer.signing.config.metadata.yubihsm.YubiHsmOpaqueDataProvider;
import tech.pegasys.web3signer.slashingprotection.DbHealthCheck;
import tech.pegasys.web3signer.slashingprotection.DbPrunerRunner;
import tech.pegasys.web3signer.slashingprotection.DbValidatorManager;
import tech.pegasys.web3signer.slashingprotection.SlashingProtectionContext;
import tech.pegasys.web3signer.slashingprotection.SlashingProtectionContextFactory;
import tech.pegasys.web3signer.slashingprotection.SlashingProtectionParameters;
import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.impl.BlockingHandlerDecorator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.plugin.services.MetricsSystem;

public class Eth2Runner extends Runner {
  public static final String KEYSTORES_PATH = "/eth/v1/keystores";
  public static final String PUBLIC_KEYS_PATH = "/api/v1/eth2/publicKeys";
  public static final String SIGN_PATH = "/api/v1/eth2/sign/:identifier";
  public static final String HIGH_WATERMARK_PATH = "/api/v1/eth2/highWatermark";
  private static final Logger LOG = LogManager.getLogger();

  private final Optional<SlashingProtectionContext> slashingProtectionContext;
  private final AzureKeyVaultParameters azureKeyVaultParameters;
  private final AwsVaultParameters awsVaultParameters;
  private final GcpSecretManagerParameters gcpSecretManagerParameters;
  private final SlashingProtectionParameters slashingProtectionParameters;
  private final boolean pruningEnabled;
  private final KeystoresParameters keystoresParameters;
  private final Spec eth2Spec;
  private final boolean isKeyManagerApiEnabled;

  public Eth2Runner(
      final BaseConfig baseConfig,
      final SlashingProtectionParameters slashingProtectionParameters,
      final AzureKeyVaultParameters azureKeyVaultParameters,
      final KeystoresParameters keystoresParameters,
      final AwsVaultParameters awsVaultParameters,
      final GcpSecretManagerParameters gcpSecretManagerParameters,
      final Spec eth2Spec,
      final boolean isKeyManagerApiEnabled) {
    super(baseConfig);
    this.slashingProtectionContext = createSlashingProtection(slashingProtectionParameters);
    this.azureKeyVaultParameters = azureKeyVaultParameters;
    this.slashingProtectionParameters = slashingProtectionParameters;
    this.pruningEnabled = slashingProtectionParameters.isPruningEnabled();
    this.keystoresParameters = keystoresParameters;
    this.eth2Spec = eth2Spec;
    this.isKeyManagerApiEnabled = isKeyManagerApiEnabled;
    this.awsVaultParameters = awsVaultParameters;
    this.gcpSecretManagerParameters = gcpSecretManagerParameters;
  }

  private Optional<SlashingProtectionContext> createSlashingProtection(
      final SlashingProtectionParameters slashingProtectionParameters) {
    if (slashingProtectionParameters.isEnabled()) {
      try {
        return Optional.of(SlashingProtectionContextFactory.create(slashingProtectionParameters));
      } catch (final IllegalStateException e) {
        throw new InitializationException(e.getMessage(), e);
      }
    } else {
      return Optional.empty();
    }
  }

  @Override
  public void populateRouter(final Context context) {
    registerEth2Routes(
        context.getRouter(),
        context.getArtifactSignerProvider(),
        context.getErrorHandler(),
        context.getMetricsSystem(),
        slashingProtectionContext);
  }

  private void registerEth2Routes(
      final Router router,
      final ArtifactSignerProvider blsSignerProvider,
      final LogErrorHandler errorHandler,
      final MetricsSystem metricsSystem,
      final Optional<SlashingProtectionContext> slashingProtectionContext) {
    final ObjectMapper objectMapper = SigningObjectMapperFactory.createObjectMapper();

    addPublicKeysListHandler(router, blsSignerProvider, PUBLIC_KEYS_PATH, errorHandler);

    final SignerForIdentifier<BlsArtifactSignature> blsSigner =
        new SignerForIdentifier<>(blsSignerProvider, this::formatBlsSignature, BLS);
    router
        .route(HttpMethod.POST, SIGN_PATH)
        .handler(
            new BlockingHandlerDecorator(
                new Eth2SignForIdentifierHandler(
                    blsSigner,
                    new HttpApiMetrics(metricsSystem, BLS, blsSignerProvider),
                    new SlashingProtectionMetrics(metricsSystem),
                    slashingProtectionContext.map(SlashingProtectionContext::getSlashingProtection),
                    objectMapper,
                    eth2Spec),
                false))
        .failureHandler(errorHandler);

    addReloadHandler(router, List.of(blsSignerProvider), errorHandler);

    slashingProtectionContext.ifPresent(
        protectionContext ->
            router
                .route(HttpMethod.GET, HIGH_WATERMARK_PATH)
                .handler(new HighWatermarkHandler(protectionContext.getSlashingProtection()))
                .failureHandler(errorHandler));

    if (isKeyManagerApiEnabled) {
      router
          .route(HttpMethod.GET, KEYSTORES_PATH)
          .handler(
              new BlockingHandlerDecorator(
                  new ListKeystoresHandler(blsSignerProvider, objectMapper), false))
          .failureHandler(errorHandler);

      final ValidatorManager validatorManager =
          createValidatorManager(blsSignerProvider, objectMapper);

      router
          .route(HttpMethod.POST, KEYSTORES_PATH)
          .blockingHandler(
              new ImportKeystoresHandler(
                  objectMapper,
                  baseConfig.getKeyConfigPath(),
                  slashingProtectionContext.map(SlashingProtectionContext::getSlashingProtection),
                  blsSignerProvider,
                  validatorManager),
              false)
          .failureHandler(errorHandler);

      router
          .route(HttpMethod.DELETE, KEYSTORES_PATH)
          .handler(
              new BlockingHandlerDecorator(
                  new DeleteKeystoresHandler(
                      objectMapper,
                      slashingProtectionContext.map(
                          SlashingProtectionContext::getSlashingProtection),
                      blsSignerProvider,
                      validatorManager),
                  false))
          .failureHandler(errorHandler);
    }
  }

  private ValidatorManager createValidatorManager(
      final ArtifactSignerProvider artifactSignerProvider, final ObjectMapper objectMapper) {
    final FileValidatorManager fileValidatorManager =
        new FileValidatorManager(
            artifactSignerProvider,
            new KeystoreFileManager(
                baseConfig.getKeyConfigPath(),
                YamlMapperFactory.createYamlMapper(baseConfig.getKeyStoreConfigFileMaxSize())),
            objectMapper);
    if (slashingProtectionContext.isPresent()) {
      final SlashingProtectionContext slashingProtectionContext =
          this.slashingProtectionContext.get();
      return new DbValidatorManager(
          fileValidatorManager,
          slashingProtectionContext.getRegisteredValidators(),
          slashingProtectionContext.getSlashingProtectionJdbi(),
          new ValidatorsDao());
    } else {
      return fileValidatorManager;
    }
  }

  @Override
  protected ArtifactSignerProvider createArtifactSignerProvider(
      final Vertx vertx, final MetricsSystem metricsSystem) {
    return new DefaultArtifactSignerProvider(
        () -> {
          try (final AzureKeyVaultFactory azureKeyVaultFactory = new AzureKeyVaultFactory()) {
            final List<ArtifactSigner> signers = new ArrayList<>();
            signers.addAll(
                loadSignersFromKeyConfigFiles(vertx, azureKeyVaultFactory, metricsSystem)
                    .getValues());
            signers.addAll(bulkLoadSigners(azureKeyVaultFactory).getValues());

            final List<Bytes> validators =
                signers.stream()
                    .map(ArtifactSigner::getIdentifier)
                    .map(Bytes::fromHexString)
                    .collect(Collectors.toList());
            if (validators.isEmpty()) {
              LOG.warn("No BLS keys loaded. Check that the key store has BLS key config files");
            } else {
              slashingProtectionContext.ifPresent(
                  context -> context.getRegisteredValidators().registerValidators(validators));
            }

            return signers;
          }
        });
  }

  private MappedResults<ArtifactSigner> loadSignersFromKeyConfigFiles(
      final Vertx vertx,
      final AzureKeyVaultFactory azureKeyVaultFactory,
      final MetricsSystem metricsSystem) {
    try (final HashicorpConnectionFactory hashicorpConnectionFactory =
            new HashicorpConnectionFactory();
        final InterlockKeyProvider interlockKeyProvider = new InterlockKeyProvider(vertx);
        final YubiHsmOpaqueDataProvider yubiHsmOpaqueDataProvider =
            new YubiHsmOpaqueDataProvider();
        final AwsSecretsManagerProvider awsSecretsManagerProvider =
            new AwsSecretsManagerProvider(awsVaultParameters.getCacheMaximumSize()); ) {
      final AbstractArtifactSignerFactory artifactSignerFactory =
          new BlsArtifactSignerFactory(
              baseConfig.getKeyConfigPath(),
              metricsSystem,
              hashicorpConnectionFactory,
              interlockKeyProvider,
              yubiHsmOpaqueDataProvider,
              awsSecretsManagerProvider,
              (args) -> new BlsArtifactSigner(args.getKeyPair(), args.getOrigin(), args.getPath()),
              azureKeyVaultFactory);

      final MappedResults<ArtifactSigner> results =
          new SignerLoader(baseConfig.keystoreParallelProcessingEnabled())
              .load(
                  baseConfig.getKeyConfigPath(),
                  "yaml",
                  new YamlSignerParser(
                      List.of(artifactSignerFactory),
                      YamlMapperFactory.createYamlMapper(
                          baseConfig.getKeyStoreConfigFileMaxSize())));
      registerSignerLoadingHealthCheck(KEYS_CHECK_CONFIG_FILE_LOADING, results);

      return results;
    }
  }

  private MappedResults<ArtifactSigner> bulkLoadSigners(
      final AzureKeyVaultFactory azureKeyVaultFactory) {
    MappedResults<ArtifactSigner> results = MappedResults.newSetInstance();
    if (azureKeyVaultParameters.isAzureKeyVaultEnabled()) {
      LOG.info("Bulk loading keys from Azure key vault ... ");
      /*
       Note: Azure supports 25K bytes per secret. https://learn.microsoft.com/en-us/azure/key-vault/secrets/about-secrets
       Each raw bls private key in hex format is approximately 100 bytes. We should store about 200 or fewer
       `\n` delimited keys per secret.
      */
      final MappedResults<ArtifactSigner> azureResult = loadAzureSigners(azureKeyVaultFactory);
      LOG.info(
          "Keys loaded from Azure: [{}], with error count: [{}]",
          azureResult.getValues().size(),
          azureResult.getErrorCount());
      registerSignerLoadingHealthCheck(KEYS_CHECK_AZURE_BULK_LOADING, azureResult);
      results = MappedResults.merge(results, azureResult);
    }

    if (keystoresParameters.isEnabled()) {
      LOG.info("Bulk loading keys from local keystores ... ");
      final BlsKeystoreBulkLoader blsKeystoreBulkLoader = new BlsKeystoreBulkLoader();
      final MappedResults<ArtifactSigner> keystoreSignersResult =
          keystoresParameters.hasKeystoresPasswordsPath()
              ? blsKeystoreBulkLoader.loadKeystoresUsingPasswordDir(
                  keystoresParameters.getKeystoresPath(),
                  keystoresParameters.getKeystoresPasswordsPath())
              : blsKeystoreBulkLoader.loadKeystoresUsingPasswordFile(
                  keystoresParameters.getKeystoresPath(),
                  keystoresParameters.getKeystoresPasswordFile());
      LOG.info(
          "Keys loaded from local keystores: [{}], with error count: [{}]",
          keystoreSignersResult.getValues().size(),
          keystoreSignersResult.getErrorCount());

      registerSignerLoadingHealthCheck(KEYS_CHECK_KEYSTORE_BULK_LOADING, keystoreSignersResult);
      results = MappedResults.merge(results, keystoreSignersResult);
    }

    if (awsVaultParameters.isEnabled()) {
      LOG.info("Bulk loading keys from AWS Secrets Manager ... ");
      final BlsAwsBulkLoader blsAwsBulkLoader = new BlsAwsBulkLoader();

      final MappedResults<ArtifactSigner> awsResult = blsAwsBulkLoader.load(awsVaultParameters);
      LOG.info(
          "Keys loaded from AWS Secrets Manager: [{}], with error count: [{}]",
          awsResult.getValues().size(),
          awsResult.getErrorCount());
      registerSignerLoadingHealthCheck(KEYS_CHECK_AWS_BULK_LOADING, awsResult);
      results = MappedResults.merge(results, awsResult);
    }

    if (gcpSecretManagerParameters.isEnabled()) {
      LOG.info("Bulk loading keys from GCP Secret Manager ... ");
      final BlsGcpBulkLoader blsGcpBulkLoader = new BlsGcpBulkLoader();
      final MappedResults<ArtifactSigner> gcpResult =
          blsGcpBulkLoader.load(gcpSecretManagerParameters);
      LOG.info(
          "Keys loaded from GCP Secret Manager: [{}], with error count: [{}]",
          gcpResult.getValues().size(),
          gcpResult.getErrorCount());
      registerSignerLoadingHealthCheck(KEYS_CHECK_GCP_BULK_LOADING, gcpResult);
      results = MappedResults.merge(results, gcpResult);
    }

    return results;
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

  @Override
  public void run() {
    super.run();
    if (pruningEnabled && slashingProtectionContext.isPresent()) {
      scheduleAndExecuteInitialDbPruning();
    }
    slashingProtectionContext.ifPresent(this::scheduleDbHealthCheck);
  }

  private void scheduleDbHealthCheck(final SlashingProtectionContext protectionContext) {
    final DbHealthCheck dbHealthCheck =
        new DbHealthCheck(
            protectionContext, slashingProtectionParameters.getDbHealthCheckTimeoutMilliseconds());

    Executors.newScheduledThreadPool(1)
        .scheduleAtFixedRate(
            dbHealthCheck,
            0,
            slashingProtectionParameters.getDbHealthCheckIntervalMilliseconds(),
            TimeUnit.MILLISECONDS);

    super.registerHealthCheckProcedure(
        SLASHING_PROTECTION_DB,
        promise -> promise.complete(dbHealthCheck.isDbUp() ? Status.OK() : Status.KO()));
  }

  private void scheduleAndExecuteInitialDbPruning() {
    final DbPrunerRunner dbPrunerRunner =
        new DbPrunerRunner(
            slashingProtectionParameters,
            slashingProtectionContext.get().getPruner(),
            Executors.newScheduledThreadPool(1));
    if (slashingProtectionParameters.isPruningAtBootEnabled()) {
      dbPrunerRunner.execute();
    }
    dbPrunerRunner.schedule();
  }

  final MappedResults<ArtifactSigner> loadAzureSigners(
      final AzureKeyVaultFactory azureKeyVaultFactory) {
    final AzureKeyVault keyVault =
        azureKeyVaultFactory.createAzureKeyVault(azureKeyVaultParameters);

    return keyVault.mapSecrets(
        (name, value) -> {
          try {
            final Bytes privateKeyBytes = Bytes.fromHexString(value);
            final BLSKeyPair keyPair =
                new BLSKeyPair(BLSSecretKey.fromBytes(Bytes32.wrap(privateKeyBytes)));
            return new BlsArtifactSigner(keyPair, SignerOrigin.AZURE);
          } catch (final Exception e) {
            LOG.error(
                "Failed to load secret named {} from azure key vault due to: {}.",
                name,
                e.getMessage());
            return null;
          }
        },
        azureKeyVaultParameters.getTags());
  }

  private String formatBlsSignature(final BlsArtifactSignature signature) {
    return signature.getSignatureData().toString();
  }
}
