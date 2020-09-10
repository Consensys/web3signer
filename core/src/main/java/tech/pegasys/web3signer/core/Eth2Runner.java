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

import tech.pegasys.web3signer.core.config.Config;
import tech.pegasys.web3signer.core.service.http.SigningJsonRpcModule;
import tech.pegasys.web3signer.core.service.http.handlers.LogErrorHandler;
import tech.pegasys.web3signer.core.service.http.handlers.signing.Eth2SignForIdentifierHandler;
import tech.pegasys.web3signer.core.service.http.handlers.signing.SignerForIdentifier;
import tech.pegasys.web3signer.core.service.http.metrics.HttpApiMetrics;
import tech.pegasys.web3signer.core.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.core.signing.BlsArtifactSignature;
import tech.pegasys.web3signer.slashingprotection.NoOpSlashingProtection;
import tech.pegasys.web3signer.slashingprotection.SlashingProtection;
import tech.pegasys.web3signer.slashingprotection.SlashingProtectionFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.impl.BlockingHandlerDecorator;
import org.hyperledger.besu.plugin.services.MetricsSystem;

public class Eth2Runner extends Runner {

  public Eth2Runner(final Config config) {
    super(config);
  }

  @Override
  public Router populateRouter(final Context context) {
    final SlashingProtection slashingProtection;
    if (config.isSlashingProtectionEnabled()) {
      slashingProtection = SlashingProtectionFactory.createSlashingProtection();
    } else {
      slashingProtection = new NoOpSlashingProtection();
    }

    registerEth2Routes(
        context.getRouterFactory(),
        context.getSigners().getBlsSignerProvider(),
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
      final SlashingProtection slashingProtection) {
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
