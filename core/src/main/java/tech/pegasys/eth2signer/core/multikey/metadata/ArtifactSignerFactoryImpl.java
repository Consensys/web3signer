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
import tech.pegasys.eth2signer.core.signing.KeyType;

public class ArtifactSignerFactoryImpl implements ArtifactSignerFactory {

  private final BlsArtifactSignerFactory blsFactory;
  private final Secp256k1ArtifactSignerFactory secpFactory;

  public ArtifactSignerFactoryImpl(
      final BlsArtifactSignerFactory blsFactory, final Secp256k1ArtifactSignerFactory secpFactory) {
    this.blsFactory = blsFactory;
    this.secpFactory = secpFactory;
  }

  @Override
  public ArtifactSigner create(final FileRawSigningMetadata metadata) {
    if (metadata.getKeyType() == KeyType.BLS) {
      return blsFactory.create(metadata);
    } else {
      return secpFactory.create(metadata);
    }
  }

  @Override
  public ArtifactSigner create(final FileKeyStoreMetadata metadata) {
    if (metadata.getKeyType() == KeyType.BLS) {
      return blsFactory.create(metadata);
    } else {
      return secpFactory.create(metadata);
    }
  }

  @Override
  public ArtifactSigner create(final HashicorpSigningMetadata metadata) {
    if (metadata.getKeyType() == KeyType.BLS) {
      return blsFactory.create(metadata);
    } else {
      return secpFactory.create(metadata);
    }
  }

  @Override
  public ArtifactSigner create(final AzureSecretSigningMetadata metadata) {
    if (metadata.getKeyType() == KeyType.BLS) {
      return blsFactory.create(metadata);
    } else {
      return secpFactory.create(metadata);
    }
  }

  @Override
  public ArtifactSigner create(final AzureKeySigningMetadata metadata) {
    if (metadata.getKeyType() == KeyType.BLS) {
      throw new SigningMetadataException(
          "Cannot perform BLS12-381 signing in using Azure Keyvault Key.");
    } else {
      return secpFactory.create(metadata);
    }
  }
}
