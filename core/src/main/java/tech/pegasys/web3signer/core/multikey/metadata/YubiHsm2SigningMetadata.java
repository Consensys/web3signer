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
package tech.pegasys.web3signer.core.multikey.metadata;

import tech.pegasys.web3signer.core.signing.ArtifactSigner;
import tech.pegasys.web3signer.core.signing.KeyType;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

public class YubiHsm2SigningMetadata extends SigningMetadata {
  private final String yubiShellBinaryPath;
  private final String connectorUrl;
  private final String authKey;
  private final String password;
  private final String opaqueData;

  // following fields are optional and will be populated if defined in configuration file
  private Optional<String> caCert = Optional.empty();
  private Optional<String> proxy = Optional.empty();

  @JsonCreator
  public YubiHsm2SigningMetadata(
      @JsonProperty("yubiShellBinaryPath") final String yubiShellBinaryPath,
      @JsonProperty("connectorUrl") final String connectorUrl,
      @JsonProperty("authKey") final String authKey,
      @JsonProperty("password") final String password,
      @JsonProperty("opaqueData") final String opaqueData,
      @JsonProperty(value = "keyType") final KeyType keyType) {
    super(keyType != null ? keyType : KeyType.BLS);
    this.yubiShellBinaryPath = yubiShellBinaryPath;
    this.connectorUrl = connectorUrl;
    this.authKey = authKey;
    this.password = password;
    this.opaqueData = opaqueData;
  }

  @JsonSetter("caCert")
  public void setCaCert(final String caCert) {
    this.caCert = Optional.ofNullable(caCert);
  }

  @JsonSetter("proxy")
  public void setProxy(final String proxy) {
    this.proxy = Optional.ofNullable(proxy);
  }

  @Override
  public ArtifactSigner createSigner(final ArtifactSignerFactory artifactSignerFactory) {
    return artifactSignerFactory.create(this);
  }
}
