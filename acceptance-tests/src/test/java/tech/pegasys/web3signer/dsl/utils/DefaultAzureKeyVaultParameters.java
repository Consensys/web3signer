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
package tech.pegasys.web3signer.dsl.utils;

import tech.pegasys.web3signer.core.config.AzureKeyVaultParameters;

public class DefaultAzureKeyVaultParameters implements AzureKeyVaultParameters {

  private String keyVaultName;
  private String clientId;
  private String tenantId;
  private String clientSecret;

  public DefaultAzureKeyVaultParameters(
      final String keyVaultName,
      final String clientId,
      final String tenantId,
      final String clientSecret) {
    this.keyVaultName = keyVaultName;
    this.clientId = clientId;
    this.tenantId = tenantId;
    this.clientSecret = clientSecret;
  }

  @Override
  public String getKeyVaultName() {
    return keyVaultName;
  }

  @Override
  public String getTenantId() {
    return tenantId;
  }

  @Override
  public String getClientlId() {
    return clientId;
  }

  @Override
  public String getClientSecret() {
    return clientSecret;
  }

  @Override
  public boolean isAzureKeyVaultEnabled() {
    return true;
  }
}
