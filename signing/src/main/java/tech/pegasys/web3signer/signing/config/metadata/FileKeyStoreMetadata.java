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
package tech.pegasys.web3signer.signing.config.metadata;

import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.KeyType;

import java.nio.file.Path;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FileKeyStoreMetadata extends SigningMetadata {
  public static final String TYPE = "file-keystore";
  private final Path keystoreFile;
  private final Path keystorePasswordFile;

  public FileKeyStoreMetadata(
      @JsonProperty(value = "keystoreFile", required = true) final Path keystoreFile,
      @JsonProperty(value = "keystorePasswordFile", required = true)
          final Path keystorePasswordFile,
      @JsonProperty(value = "keyType") final KeyType keyType) {
    super(TYPE, keyType != null ? keyType : KeyType.BLS);
    this.keystoreFile = keystoreFile;
    this.keystorePasswordFile = keystorePasswordFile;
  }

  @Override
  public ArtifactSigner createSigner(final ArtifactSignerFactory artifactSignerFactory) {
    return artifactSignerFactory.create(this);
  }

  public Path getKeystoreFile() {
    return keystoreFile;
  }

  public Path getKeystorePasswordFile() {
    return keystorePasswordFile;
  }
}
