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

import tech.pegasys.web3signer.signing.secp256k1.Signer;
import tech.pegasys.web3signer.signing.secp256k1.filebased.FileSignerConfig;
import tech.pegasys.web3signer.signing.secp256k1.multikey.MultiSignerFactory;

import java.util.Objects;

public class FileBasedSigningMetadataFile extends SigningMetadataFile {

  private final FileSignerConfig config;

  public FileBasedSigningMetadataFile(final String filename, final FileSignerConfig config) {
    super(filename);
    this.config = config;
  }

  public FileSignerConfig getConfig() {
    return config;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final FileBasedSigningMetadataFile that = (FileBasedSigningMetadataFile) o;
    return Objects.equals(config, that.config);
  }

  @Override
  public int hashCode() {
    return Objects.hash(config);
  }

  @Override
  public Signer createSigner(final MultiSignerFactory factory) {
    return factory.createSigner(this);
  }
}
