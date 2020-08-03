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
package tech.pegasys.eth2signer.core.multikey.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import tech.pegasys.eth2signer.core.signing.ArtifactSigner;
import tech.pegasys.signers.secp256k1.azure.AzureConfig;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class AzureSecpCloudSigningMetadata implements SigningMetadata {

  @JsonUnwrapped final AzureConfig config;

  @JsonCreator
  public AzureSecpCloudSigningMetadata(final AzureConfig config) {
    this.config = config;
  }

  public AzureConfig getConfig() {
    return config;
  }

  @Override
  public ArtifactSigner createSigner(ArtifactSignerFactory artifactSignerFactory) {
    return artifactSignerFactory.create(this);
  }
}
