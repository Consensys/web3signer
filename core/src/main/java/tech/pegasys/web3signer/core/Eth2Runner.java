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
import static tech.pegasys.web3signer.core.signing.KeyType.BLS;

import tech.pegasys.signers.hashicorp.HashicorpConnectionFactory;
import tech.pegasys.web3signer.core.config.Config;
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
import tech.pegasys.web3signer.slashingprotection.SlashingProtection;
import tech.pegasys.web3signer.slashingprotection.ValidatorsDao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.impl.BlockingHandlerDecorator;
import org.hyperledger.besu.plugin.services.MetricsSystem;

public class Eth2Runner extends Runner {
  final Optional<SlashingProtection> slashingProtection;
  private ValidatorsDao validatorsDao;

  public Eth2Runner(
      final Config config,
      final Optional<SlashingProtection> slashingProtection,
      final ValidatorsDao validatorsDao) {
    super(config);
    this.slashingProtection = slashingProtection;
    this.validatorsDao = validatorsDao;
  }

  @Override
  protected String getOpenApiSpecResource() {
    return "openapi/web3signer-eth2.yaml";
  }

  @Override
  protected ArtifactSignerProvider loadSigners(
      final Config config, final Vertx vertx, final MetricsSystem metricsSystem) {
    final HashicorpConnectionFactory hashicorpConnectionFactory =
        new HashicorpConnectionFactory(vertx);

    final AbstractArtifactSignerFactory artifactSignerFactory =
        new BlsArtifactSignerFactory(
            config.getKeyConfigPath(),
            metricsSystem,
            hashicorpConnectionFactory,
            BlsArtifactSigner::new);

    final Collection<ArtifactSigner> signers =
        SignerLoader.load(
            config.getKeyConfigPath(),
            "yaml",
            new YamlSignerParser(List.of(artifactSignerFactory)));

    final List<String> validators =
        signers.stream().map(ArtifactSigner::getIdentifier).collect(Collectors.toList());
    validatorsDao.registerValidators(validators);

    return DefaultArtifactSignerProvider.create(signers);
  }

  @Override
  public Router populateRouter(final Context context) {
    registerEth2Routes(
        context.getRouterFactory(),
        context.getSignerProvider(),
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
                slashingProtection,
                objectMapper),
            false));
    routerFactory.addFailureHandlerByOperationId(ETH2_SIGN.name(), errorHandler);
  }

  private String formatBlsSignature(final BlsArtifactSignature signature) {
    return signature.getSignatureData().toString();
  }
}
