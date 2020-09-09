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

import tech.pegasys.web3signer.core.config.Config;
import tech.pegasys.web3signer.core.service.http.handlers.LogErrorHandler;
import tech.pegasys.web3signer.core.service.http.handlers.signing.Eth1SignForIdentifierHandler;
import tech.pegasys.web3signer.core.service.http.handlers.signing.SignerForIdentifier;
import tech.pegasys.web3signer.core.service.http.metrics.HttpApiMetrics;
import tech.pegasys.web3signer.core.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.core.signing.SecpArtifactSignature;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.impl.BlockingHandlerDecorator;

public class Eth1Runner extends Runner {

  public Eth1Runner(final Config config) {
    super(config);
  }

  @Override
  protected Router populateRouter(final Context context) {
    final ArtifactSignerProvider secpSignerProvider = context.getSigners().getEthSignerProvider();
    final OpenAPI3RouterFactory routerFactory = context.getRouterFactory();
    final LogErrorHandler errorHandler = context.getErrorHandler();

    addPublicKeysListHandler(
        routerFactory,
        secpSignerProvider.availableIdentifiers(),
        ETH1_LIST.name(),
        context.getErrorHandler());

    final SignerForIdentifier<SecpArtifactSignature> secpSigner =
        new SignerForIdentifier<>(secpSignerProvider, this::formatSecpSignature, SECP256K1);
    routerFactory.addHandlerByOperationId(
        ETH1_SIGN.name(),
        new BlockingHandlerDecorator(
            new Eth1SignForIdentifierHandler(
                secpSigner, new HttpApiMetrics(context.getMetricsSystem(), SECP256K1)),
            false));
    routerFactory.addFailureHandlerByOperationId(ETH1_SIGN.name(), errorHandler);

    return context.getRouterFactory().getRouter();
  }

  private String formatSecpSignature(final SecpArtifactSignature signature) {
    return SecpArtifactSignature.toBytes(signature).toHexString();
  }
}
