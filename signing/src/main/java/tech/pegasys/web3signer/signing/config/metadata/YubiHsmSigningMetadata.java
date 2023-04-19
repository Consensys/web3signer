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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class YubiHsmSigningMetadata extends SigningMetadata {
  public static final String TYPE = "yubihsm";
  private final String pkcs11ModulePath;
  private final String connectorUrl;
  private final String additionalInitConfig;

  private final short authId;
  private final String password;

  private final short opaqueDataId;

  @JsonCreator
  public YubiHsmSigningMetadata(
      @JsonProperty(value = "pkcs11ModulePath", required = true) final String pkcs11ModulePath,
      @JsonProperty(value = "connectorUrl", required = true) final String connectorUrl,
      @JsonProperty(value = "additionalInitConfig") final String additionalInitConfig,
      @JsonProperty(value = "authId", required = true) final short authId,
      @JsonProperty(value = "password", required = true) final String password,
      @JsonProperty(value = "opaqueDataId", required = true) final short opaqueDataId,
      @JsonProperty(value = "keyType") final KeyType keyType) {
    super(TYPE, keyType != null ? keyType : KeyType.BLS);

    this.pkcs11ModulePath = pkcs11ModulePath;
    this.connectorUrl = connectorUrl;
    this.additionalInitConfig = additionalInitConfig;
    this.authId = authId;
    this.password = password;
    this.opaqueDataId = opaqueDataId;
  }

  public String getPkcs11ModulePath() {
    return pkcs11ModulePath;
  }

  public String getConnectorUrl() {
    return connectorUrl;
  }

  public String getAdditionalInitConfig() {
    return additionalInitConfig;
  }

  public short getAuthId() {
    return authId;
  }

  public String getPassword() {
    return password;
  }

  public short getOpaqueDataId() {
    return opaqueDataId;
  }

  @Override
  public ArtifactSigner createSigner(final ArtifactSignerFactory artifactSignerFactory) {
    return artifactSignerFactory.create(this);
  }
}
