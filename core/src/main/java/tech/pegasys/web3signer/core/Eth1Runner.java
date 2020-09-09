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

import static tech.pegasys.web3signer.core.service.http.OpenApiOperationsId.ETH1_LIST;
import static tech.pegasys.web3signer.core.service.http.OpenApiOperationsId.ETH1_SIGN;
import static tech.pegasys.web3signer.core.signing.KeyType.SECP256K1;

import tech.pegasys.web3signer.core.config.Config;
import tech.pegasys.web3signer.core.service.http.handlers.signing.SignerForIdentifier;
import tech.pegasys.web3signer.core.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.core.signing.SecpArtifactSignature;

public class Eth1Runner extends Runner {

  public Eth1Runner(Config config) {
    super(config);
  }

  @Override
  protected void createHandler(Context context) {
    final ArtifactSignerProvider secpSignerProvider = context.getSigners().getEthSignerProvider();
    addPublicKeysListHandler(
        context.getRouterFactory(),
        secpSignerProvider.availableIdentifiers(),
        ETH1_LIST.name(),
        context.getErrorHandler());

    final SignerForIdentifier<SecpArtifactSignature> secpSigner =
        new SignerForIdentifier<>(secpSignerProvider, this::formatSecpSignature, SECP256K1);

    addSignHandler(
        context.getRouterFactory(), ETH1_SIGN.name(), secpSigner, context.getMetricsSystem(),
        SECP256K1, context.getErrorHandler(), null);
  }


  private String formatSecpSignature(final SecpArtifactSignature signature) {
    return SecpArtifactSignature.toBytes(signature).toHexString();
  }
}
