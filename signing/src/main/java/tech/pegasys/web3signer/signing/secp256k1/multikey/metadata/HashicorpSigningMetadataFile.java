/*
 * Copyright 2019 ConsenSys AG.
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
package tech.pegasys.web3signer.signing.secp256k1.multikey.metadata;

import tech.pegasys.web3signer.keystorage.hashicorp.config.HashicorpKeyConfig;
import tech.pegasys.web3signer.signing.secp256k1.Signer;
import tech.pegasys.web3signer.signing.secp256k1.multikey.MultiSignerFactory;

public class HashicorpSigningMetadataFile extends SigningMetadataFile {

  private final HashicorpKeyConfig hashicorpConfig;

  public HashicorpSigningMetadataFile(
      final String filename, final HashicorpKeyConfig hashicorpConfig) {
    super(filename);
    this.hashicorpConfig = hashicorpConfig;
  }

  public HashicorpKeyConfig getConfig() {
    return hashicorpConfig;
  }

  @Override
  public Signer createSigner(final MultiSignerFactory factory) {
    return factory.createSigner(this);
  }
}
