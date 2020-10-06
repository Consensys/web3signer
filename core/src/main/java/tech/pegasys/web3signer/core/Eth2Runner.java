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
import static tech.pegasys.web3signer.core.service.http.metrics.HttpApiMetrics.incSignerLoadCount;
import static tech.pegasys.web3signer.core.signing.KeyType.BLS;

import tech.pegasys.signers.azure.AzureKeyVault;
import tech.pegasys.signers.hashicorp.HashicorpConnectionFactory;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.web3signer.core.config.AzureKeyVaultParameters;
import tech.pegasys.web3signer.core.config.Config;
import tech.pegasys.web3signer.core.metrics.SlashingProtectionMetrics;
import tech.pegasys.web3signer.core.multikey.DefaultArtifactSignerProvider;
import tech.pegasys.web3signer.core.multikey.SignerLoader;
import tech.pegasys.web3signer.core.multikey.metadata.AbstractArtifactSignerFactory;
import tech.pegasys.web3signer.core.multikey.metadata.BlsArtifactSignerFactory;
import tech.pegasys.web3signer.core.multikey.metadata.parser.YamlSignerParser;
import tech.pegasys.web3signer.core.service.http.SigningJsonRpcModule;
import tech.pegasys.web3signer.core.service.http.handlers.LogErrorHandler;
import tech.pegasys.web3signer.core.service.http.handlers.signing.Eth2SignForIdentifierHandler;
import tech.pegasys.web3signer.core.service.http.handlers.signing.SignerForIdentifier;
import tech.pegasys.web3signer.core.service.http.metrics.HttpApiMetrics;
import tech.pegasys.web3signer.core.signing.ArtifactSigner;
import tech.pegasys.web3signer.core.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.core.signing.BlsArtifactSignature;
import tech.pegasys.web3signer.core.signing.BlsArtifactSigner;
import tech.pegasys.web3signer.slashingprotection.DbConnection;
import tech.pegasys.web3signer.slashingprotection.SlashingProtection;
import tech.pegasys.web3signer.slashingprotection.SlashingProtectionFactory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
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
import org.jdbi.v3.core.Jdbi;

public class Eth2Runner extends Runner {

  private final Optional<SlashingProtection> slashingProtection;
  private final AzureKeyVaultParameters azureKeyVaultParameters;

  private static final Logger LOG = LogManager.getLogger();

  public Eth2Runner(
      final Config config,
      final boolean slashingProtectionEnabled,
      final String slashingProtectionDbUrl,
      final String slashingProtectionDbUser,
      final String slashingProtectionDbPassword,
      final AzureKeyVaultParameters azureKeyVaultParameters) {
    super(config);
    this.slashingProtection =
        createSlashingProtection(
            slashingProtectionEnabled,
            slashingProtectionDbUrl,
            slashingProtectionDbUser,
            slashingProtectionDbPassword);
    this.azureKeyVaultParameters = azureKeyVaultParameters;
  }

  private Optional<SlashingProtection> createSlashingProtection(
      final boolean slashingProtectionEnabled,
      final String slashingProtectionDbUrl,
      final String slashingProtectionDbUser,
      final String slashingProtectionDbPassword) {
    if (slashingProtectionEnabled) {
      final Jdbi jdbi =
          DbConnection.createConnection(
              slashingProtectionDbUrl, slashingProtectionDbUser, slashingProtectionDbPassword);
      return Optional.of(SlashingProtectionFactory.createSlashingProtection(jdbi));
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
    final ArtifactSignerProvider signerProvider =
        loadSigners(config, context.getVertx(), context.getMetricsSystem());
    incSignerLoadCount(context.getMetricsSystem(), signerProvider.availableIdentifiers().size());

    registerEth2Routes(
        context.getRouterFactory(),
        signerProvider,
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
    final ObjectMapper objectMapper =
        new ObjectMapper()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .registerModule(new SigningJsonRpcModule());

    addPublicKeysListHandler(
        routerFactory, blsSignerProvider.availableIdentifiers(), ETH2_LIST.name(), errorHandler);

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
                objectMapper),
            false));
    routerFactory.addFailureHandlerByOperationId(ETH2_SIGN.name(), errorHandler);
  }

  private ArtifactSignerProvider loadSigners(
      final Config config, final Vertx vertx, final MetricsSystem metricsSystem) {

    final List<ArtifactSigner> signers = Lists.newArrayList();
    final HashicorpConnectionFactory hashicorpConnectionFactory =
        new HashicorpConnectionFactory(vertx);

    final AbstractArtifactSignerFactory artifactSignerFactory =
        new BlsArtifactSignerFactory(
            config.getKeyConfigPath(),
            metricsSystem,
            hashicorpConnectionFactory,
            BlsArtifactSigner::new);

    signers.addAll(
        SignerLoader.load(
            config.getKeyConfigPath(),
            "yaml",
            new YamlSignerParser(List.of(artifactSignerFactory))));

    if (azureKeyVaultParameters.isAzureKeyVaultEnabled()) {
      signers.addAll(loadAzureSigners());
    }

    final List<Bytes> validators =
        signers.stream()
            .map(ArtifactSigner::getIdentifier)
            .map(Bytes::fromHexString)
            .collect(Collectors.toList());
    slashingProtection.ifPresent(
        slashingProtection -> slashingProtection.registerValidators(validators));

    return DefaultArtifactSignerProvider.create(signers);
  }

  final Collection<ArtifactSigner> loadAzureSigners() {
    final AzureKeyVault keyVault =
        new AzureKeyVault(
            azureKeyVaultParameters.getClientlId(),
            azureKeyVaultParameters.getClientSecret(),
            azureKeyVaultParameters.getTenantId(),
            azureKeyVaultParameters.getKeyVaultName());

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
