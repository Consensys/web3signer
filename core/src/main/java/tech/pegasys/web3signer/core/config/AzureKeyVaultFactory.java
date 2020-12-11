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
package tech.pegasys.web3signer.core.config;

import tech.pegasys.signers.azure.AzureKeyVault;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

public class AzureKeyVaultFactory {
  public static AzureKeyVault createAzureKeyVault(
      final AzureKeyVaultParameters azureKeyVaultParameters) {
    if (azureKeyVaultParameters.getAuthenticationMode() == AzureAuthenticationMode.CLIENT_SECRET) {
      return AzureKeyVault.createUsingClientSecretCredentials(
          azureKeyVaultParameters.getClientId(),
          azureKeyVaultParameters.getClientSecret(),
          azureKeyVaultParameters.getTenantId(),
          azureKeyVaultParameters.getKeyVaultName());
    } else {
      final Optional<String> clientId =
          StringUtils.isBlank(azureKeyVaultParameters.getClientId())
              ? Optional.empty()
              : Optional.of(azureKeyVaultParameters.getClientId());
      return AzureKeyVault.createUsingManagedIdentity(
          clientId, azureKeyVaultParameters.getKeyVaultName());
    }
  }
}
