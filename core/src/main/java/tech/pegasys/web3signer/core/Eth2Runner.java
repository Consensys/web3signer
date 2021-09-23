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

import static tech.pegasys.web3signer.core.service.http.OpenApiOperationsId.ETH2_LIST;
import static tech.pegasys.web3signer.core.service.http.OpenApiOperationsId.ETH2_SIGN;
import static tech.pegasys.web3signer.core.service.http.OpenApiOperationsId.RELOAD;
import static tech.pegasys.web3signer.core.signing.KeyType.BLS;

import tech.pegasys.signers.azure.AzureKeyVault;
import tech.pegasys.signers.hashicorp.HashicorpConnectionFactory;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.web3signer.core.config.AzureKeyVaultFactory;
import tech.pegasys.web3signer.core.config.AzureKeyVaultParameters;
import tech.pegasys.web3signer.core.config.Config;
import tech.pegasys.web3signer.core.metrics.SlashingProtectionMetrics;
import tech.pegasys.web3signer.core.multikey.DefaultArtifactSignerProvider;
import tech.pegasys.web3signer.core.multikey.SignerLoader;
import tech.pegasys.web3signer.core.multikey.metadata.AbstractArtifactSignerFactory;
import tech.pegasys.web3signer.core.multikey.metadata.BlsArtifactSignerFactory;
import tech.pegasys.web3signer.core.multikey.metadata.interlock.InterlockKeyProvider;
import tech.pegasys.web3signer.core.multikey.metadata.parser.YamlSignerParser;
import tech.pegasys.web3signer.core.multikey.metadata.yubihsm.YubiHsmOpaqueDataProvider;
import tech.pegasys.web3signer.core.service.http.SigningObjectMapperFactory;
import tech.pegasys.web3signer.core.service.http.handlers.LogErrorHandler;
import tech.pegasys.web3signer.core.service.http.handlers.signing.SignerForIdentifier;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.Eth2SignForIdentifierHandler;
import tech.pegasys.web3signer.core.service.http.metrics.HttpApiMetrics;
import tech.pegasys.web3signer.core.signing.ArtifactSigner;
import tech.pegasys.web3signer.core.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.core.signing.BlsArtifactSignature;
import tech.pegasys.web3signer.core.signing.BlsArtifactSigner;
import tech.pegasys.web3signer.slashingprotection.DbPrunerRunner;
import tech.pegasys.web3signer.slashingprotection.SlashingProtection;
import tech.pegasys.web3signer.slashingprotection.SlashingProtectionFactory;
import tech.pegasys.web3signer.slashingprotection.SlashingProtectionParameters;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.impl.BlockingHandlerDecorator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.plugin.services.MetricsSystem;

public class Eth2Runner extends Runner {
  private static final Logger LOG = LogManager.getLogger();

  private final Optional<SlashingProtection> slashingProtection;
  private final AzureKeyVaultParameters azureKeyVaultParameters;
  private final SlashingProtectionParameters slashingProtectionParameters;
  private final boolean pruningEnabled;
  private final Spec eth2Spec;

  public Eth2Runner(
      final Config config,
      final SlashingProtectionParameters slashingProtectionParameters,
      final AzureKeyVaultParameters azureKeyVaultParameters,
      final Spec eth2Spec) {
    super(config);
    this.slashingProtection = createSlashingProtection(slashingProtectionParameters);
    this.azureKeyVaultParameters = azureKeyVaultParameters;
    this.slashingProtectionParameters = slashingProtectionParameters;
    this.pruningEnabled = slashingProtectionParameters.isPruningEnabled();
    this.eth2Spec = eth2Spec;
  }

  private Optional<SlashingProtection> createSlashingProtection(
      final SlashingProtectionParameters slashingProtectionParameters) {
    if (slashingProtectionParameters.isEnabled()) {
      try {
        return Optional.of(
            SlashingProtectionFactory.createSlashingProtection(slashingProtectionParameters));
      } catch (final IllegalStateException e) {
        throw new InitializationException(e.getMessage(), e);
      }
    } else {
      return Optional.empty();
    }
  }

  @Override
  protected String getOpenApiSpecResource() {
    return "openapi/web3signer-eth2.yaml";
  }

  @Override
  public Router populateRouter(final Context context) {
    registerEth2Routes(
        context.getRouterFactory(),
        context.getArtifactSignerProvider(),
        context.getErrorHandler(),
        context.getMetricsSystem(),
        slashingProtection);

    return context.getRouterFactory().getRouter();
  }

  private void registerEth2Routes(
      final OpenAPI3RouterFactory routerFactory,
      final ArtifactSignerProvider blsSignerProvider,
      final LogErrorHandler errorHandler,
      final MetricsSystem metricsSystem,
      final Optional<SlashingProtection> slashingProtection) {
    final ObjectMapper objectMapper = SigningObjectMapperFactory.createObjectMapper();

    addPublicKeysListHandler(routerFactory, blsSignerProvider, ETH2_LIST.name(), errorHandler);

    final SignerForIdentifier<BlsArtifactSignature> blsSigner =
        new SignerForIdentifier<>(blsSignerProvider, this::formatBlsSignature, BLS);
    routerFactory.addHandlerByOperationId(
        ETH2_SIGN.name(),
        new BlockingHandlerDecorator(
            new Eth2SignForIdentifierHandler(
                blsSigner,
                new HttpApiMetrics(metricsSystem, BLS),
                new SlashingProtectionMetrics(metricsSystem),
                slashingProtection,
                objectMapper,
                eth2Spec),
            false));
    routerFactory.addFailureHandlerByOperationId(ETH2_SIGN.name(), errorHandler);

    addReloadHandler(routerFactory, blsSignerProvider, RELOAD.name(), errorHandler);
  }

  @Override
  protected ArtifactSignerProvider createArtifactSignerProvider(
      final Vertx vertx, final MetricsSystem metricsSystem) {
    return new DefaultArtifactSignerProvider(
        () -> {
          final List<ArtifactSigner> signers = Lists.newArrayList();
          final HashicorpConnectionFactory hashicorpConnectionFactory =
              new HashicorpConnectionFactory(vertx);

          try (final InterlockKeyProvider interlockKeyProvider = new InterlockKeyProvider(vertx);
              final YubiHsmOpaqueDataProvider yubiHsmOpaqueDataProvider =
                  new YubiHsmOpaqueDataProvider()) {
            final AbstractArtifactSignerFactory artifactSignerFactory =
                new BlsArtifactSignerFactory(
                    config.getKeyConfigPath(),
                    metricsSystem,
                    hashicorpConnectionFactory,
                    interlockKeyProvider,
                    yubiHsmOpaqueDataProvider,
                    BlsArtifactSigner::new);

            signers.addAll(
                SignerLoader.load(
                    config.getKeyConfigPath(),
                    "yaml",
                    new YamlSignerParser(List.of(artifactSignerFactory))));
          }

          if (azureKeyVaultParameters.isAzureKeyVaultEnabled()) {
            signers.addAll(loadAzureSigners());
          }

          final List<Bytes> validators =
              signers.stream()
                  .map(ArtifactSigner::getIdentifier)
                  .map(Bytes::fromHexString)
                  .collect(Collectors.toList());
          if (validators.isEmpty()) {
            LOG.warn("No BLS keys loaded. Check that the key store has BLS key config files");
          } else {
            slashingProtection.ifPresent(
                slashingProtection1 -> slashingProtection1.registerValidators(validators));
          }
          return signers;
        });
  }

  @Override
  public void run() {
    super.run();
    if (pruningEnabled && slashingProtection.isPresent()) {
      scheduleAndExecuteInitialDbPruning();
    }
  }

  private void scheduleAndExecuteInitialDbPruning() {
    final DbPrunerRunner dbPrunerRunner =
        new DbPrunerRunner(
            slashingProtectionParameters,
            slashingProtection.get(),
            Executors.newScheduledThreadPool(1));
    if (slashingProtectionParameters.isPruningAtBootEnabled()) {
      dbPrunerRunner.execute();
    }
    dbPrunerRunner.schedule();
  }

  final Collection<ArtifactSigner> loadAzureSigners() {
    final AzureKeyVault keyVault =
        AzureKeyVaultFactory.createAzureKeyVault(azureKeyVaultParameters);

    return keyVault.mapSecrets(
        (name, value) -> {
          try {
            final Bytes privateKeyBytes = Bytes.fromHexString(value);
            final BLSKeyPair keyPair =
                new BLSKeyPair(BLSSecretKey.fromBytes(Bytes32.wrap(privateKeyBytes)));
            return new BlsArtifactSigner(keyPair);
          } catch (final Exception e) {
            LOG.error("Failed to load secret named {} from azure key vault.", name);
            return null;
          }
        });
  }

  private String formatBlsSignature(final BlsArtifactSignature signature) {
    return signature.getSignatureData().toString();
  }
}
