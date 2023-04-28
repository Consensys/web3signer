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

import java.net.URI;
import java.nio.file.Path;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class InterlockSigningMetadata extends SigningMetadata {
  public static final String TYPE = "interlock";
  private final URI interlockUrl;
  private final Path knownServersFile;

  private final String volume;
  private final String password;
  private final String keyPath;

  @JsonCreator
  public InterlockSigningMetadata(
      @JsonProperty(value = "interlockUrl", required = true) final String interlockUrl,
      @JsonProperty(value = "knownServersFile", required = true) final String knownServersFile,
      @JsonProperty(value = "volume", required = true) final String volume,
      @JsonProperty(value = "password", required = true) final String password,
      @JsonProperty(value = "keyPath", required = true) final String keyPath,
      @JsonProperty(value = "keyType") final KeyType keyType) {
    super(TYPE, keyType != null ? keyType : KeyType.BLS);

    this.interlockUrl = URI.create(interlockUrl);
    this.knownServersFile = Path.of(knownServersFile);
    this.volume = volume;
    this.password = password;
    this.keyPath = keyPath;
  }

  public URI getInterlockUrl() {
    return interlockUrl;
  }

  public Path getKnownServersFile() {
    return knownServersFile;
  }

  public String getVolume() {
    return volume;
  }

  public String getPassword() {
    return password;
  }

  public String getKeyPath() {
    return keyPath;
  }

  @Override
  public ArtifactSigner createSigner(final ArtifactSignerFactory artifactSignerFactory) {
    return artifactSignerFactory.create(this);
  }
}
