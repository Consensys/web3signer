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
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package tech.pegasys.web3signer.core;

import static tech.pegasys.web3signer.core.service.http.OpenApiOperationsId.ETH2_LIST;
import static tech.pegasys.web3signer.core.service.http.OpenApiOperationsId.ETH2_SIGN;
import static tech.pegasys.web3signer.core.signing.KeyType.BLS;

import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import tech.pegasys.web3signer.core.config.Config;
import tech.pegasys.web3signer.core.service.http.handlers.LogErrorHandler;
import tech.pegasys.web3signer.core.service.http.handlers.signing.SignerForIdentifier;
import tech.pegasys.web3signer.core.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.core.signing.BlsArtifactSignature;
import tech.pegasys.web3signer.slashingprotection.NoOpSlashingProtection;
import tech.pegasys.web3signer.slashingprotection.SlashingProtection;
import tech.pegasys.web3signer.slashingprotection.SlashingProtectionFactory;

public class Eth2Runner extends Runner {

  public Eth2Runner(final Config config) {
    super(config);
  }

  @Override
  public void createHandler(final Context context) {
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
  }

  private void registerEth2Routes(
      final OpenAPI3RouterFactory routerFactory,
      final ArtifactSignerProvider blsSignerProvider,
      final LogErrorHandler errorHandler,
      final MetricsSystem metricsSystem,
      final SlashingProtection slashingProtection) {
    addPublicKeysListHandler(
        routerFactory, blsSignerProvider.availableIdentifiers(), ETH2_LIST.name(), errorHandler);

    final SignerForIdentifier<BlsArtifactSignature> blsSigner =
        new SignerForIdentifier<>(blsSignerProvider, this::formatBlsSignature, BLS);
    addSignHandler(
        routerFactory,
        ETH2_SIGN.name(),
        blsSigner,
        metricsSystem,
        BLS,
        errorHandler,
        slashingProtection);
  }

  private String formatBlsSignature(final BlsArtifactSignature signature) {
    return signature.getSignatureData().toString();
  }
}
