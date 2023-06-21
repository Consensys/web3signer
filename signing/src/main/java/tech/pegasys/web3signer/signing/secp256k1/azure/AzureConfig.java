/*
 * Copyright 2023 ConsenSys AG.
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
package tech.pegasys.web3signer.signing.secp256k1.azure;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;

public class AzureConfig {
  private final String keyVaultName;
  private final String keyName;
  private final String keyVersion;
  private final String clientId;
  private final String clientSecret;
  private final String tenantId;

  @JsonCreator
  public AzureConfig(
      final String keyVaultName,
      final String keyName,
      final String keyVersion,
      final String clientId,
      final String clientSecret,
      final String tenantId) {
    this.keyVaultName = keyVaultName;
    this.keyName = keyName;
    this.keyVersion = keyVersion;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.tenantId = tenantId;
  }

  public String getKeyVaultName() {
    return keyVaultName;
  }

  public String getKeyName() {
    return keyName;
  }

  public String getKeyVersion() {
    return keyVersion;
  }

  public String getClientId() {
    return clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public String getTenantId() {
    return tenantId;
  }

  public static class AzureConfigBuilder {

    private String keyVaultName;
    private String keyName;
    private String keyVersion;
    private String clientId;
    private String clientSecret;
    private String tenantId;

    public AzureConfigBuilder withKeyVaultName(final String keyVaultName) {
      this.keyVaultName = keyVaultName;
      return this;
    }

    public AzureConfigBuilder withKeyName(final String keyName) {
      this.keyName = keyName;
      return this;
    }

    public AzureConfigBuilder withKeyVersion(final String keyVersion) {
      this.keyVersion = keyVersion;
      return this;
    }

    public AzureConfigBuilder withClientId(final String clientId) {
      this.clientId = clientId;
      return this;
    }

    public AzureConfigBuilder withClientSecret(final String clientSecret) {
      this.clientSecret = clientSecret;
      return this;
    }

    public AzureConfigBuilder withTenantId(final String tenantId) {
      this.tenantId = tenantId;
      return this;
    }

    public AzureConfig build() {
      checkNotNull(keyVaultName, "Key Vault Name was not set.");
      checkNotNull(keyName, "Key Name was not set.");
      checkNotNull(keyVersion, "Key Version was not set.");
      checkNotNull(clientId, "Client Id was not set.");
      checkNotNull(clientSecret, "Client Secret was not set.");
      checkNotNull(tenantId, "Tenant Id was not set.");

      return new AzureConfig(keyVaultName, keyName, keyVersion, clientId, clientSecret, tenantId);
    }
  }
}
