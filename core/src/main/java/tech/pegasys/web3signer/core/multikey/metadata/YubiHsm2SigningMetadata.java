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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import tech.pegasys.signers.yubihsm2.OutputFormat;
import tech.pegasys.web3signer.core.signing.ArtifactSigner;
import tech.pegasys.web3signer.core.signing.KeyType;

import java.util.Optional;

public class YubiHsm2SigningMetadata extends SigningMetadata {
  private final String connectorUrl;
  private final Short authKey;
  private final String password;
  private final Short opaqueObjId;

  // following fields are optional and will be populated if defined in configuration file
  private Optional<OutputFormat> outformat = Optional.empty();
  private Optional<String> caCertPath = Optional.empty();
  private Optional<String> proxyUrl = Optional.empty();

  @JsonCreator
  public YubiHsm2SigningMetadata(
      @JsonProperty(value = "connectorUrl", required = true) final String connectorUrl,
      @JsonProperty(value = "authKey", required = true) final Short authKey,
      @JsonProperty(value = "password", required = true) final String password,
      @JsonProperty(value = "opaqueObjId", required = true) final Short opaqueObjId,
      @JsonProperty("keyType") final KeyType keyType) {
    super(keyType != null ? keyType : KeyType.BLS);

    this.connectorUrl = connectorUrl;
    this.authKey = authKey;
    this.password = password;
    this.opaqueObjId = opaqueObjId;
  }

  @SuppressWarnings("UnunsedMethod")
  @JsonSetter("caCertPath")
  public void setCaCertPath(final String caCertPath) {
    this.caCertPath = Optional.ofNullable(caCertPath);
  }

  @SuppressWarnings("UnunsedMethod")
  @JsonSetter("proxyUrl")
  public void setProxyUrl(final String proxyUrl) {
    this.proxyUrl = Optional.ofNullable(proxyUrl);
  }

  @JsonSetter("outformat")
  public void setOutformat(final OutputFormat outformat) {
    this.outformat = Optional.ofNullable(outformat);
  }

  public String getConnectorUrl() {
    return connectorUrl;
  }

  public Short getAuthKey() {
    return authKey;
  }

  public String getPassword() {
    return password;
  }

  public Short getOpaqueObjId() {
    return opaqueObjId;
  }

  public Optional<OutputFormat> getOutformat() {
    return outformat;
  }

  public Optional<String> getCaCertPath() {
    return caCertPath;
  }

  public Optional<String> getProxyUrl() {
    return proxyUrl;
  }

  @Override
  public ArtifactSigner createSigner(final ArtifactSignerFactory artifactSignerFactory) {
    return artifactSignerFactory.create(this);
  }
}
