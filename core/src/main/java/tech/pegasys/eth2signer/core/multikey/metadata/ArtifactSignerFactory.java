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

import tech.pegasys.eth2signer.core.signing.ArtifactSigner;

public interface ArtifactSignerFactory {

  default ArtifactSigner create(FileRawSigningMetadata fileRawSigningMetadata) {
    throw new UnsupportedOperationException(
        "Unable to generate a signer of requested type from supplied metdata");
  }

  default ArtifactSigner create(FileKeyStoreMetadata fileKeyStoreMetadata) {
    throw new UnsupportedOperationException(
        "Unable to generate a signer of requested type from supplied metdata");
  }

  default ArtifactSigner create(HashicorpSigningMetadata hashicorpMetadata) {
    throw new UnsupportedOperationException(
        "Unable to generate a signer of requested type from supplied metdata");
  }

  default ArtifactSigner create(AzureSigningMetadata azureSigningMetadata) {
    throw new UnsupportedOperationException(
        "Unable to generate a signer of requested type from supplied metdata");
  }

  default ArtifactSigner create(AzureSecpCloudSigningMetadata azureSigningMetadata) {
    throw new UnsupportedOperationException(
        "Unable to generate a signer of requested type from supplied metdata");
  }
}
