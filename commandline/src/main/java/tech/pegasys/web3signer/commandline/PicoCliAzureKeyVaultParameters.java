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
package tech.pegasys.web3signer.commandline;

import tech.pegasys.web3signer.core.config.AzureKeyVaultParameters;

import picocli.CommandLine.Option;

public class PicoCliAzureKeyVaultParameters implements AzureKeyVaultParameters {

  @Option(
      names = {"--azure-vault-name"},
      description = "Name of the vault to access - used as the sub-domain to vault.azure.net",
      paramLabel = "<KEY_VAULT_NAME>",
      required = true,
      arity = "1")
  private String keyVaultName;

  @Option(
      names = {"--azure-client-id"},
      description = "The ID used to authenticate with Azure key vault",
      required = true,
      paramLabel = "<CLIENT_ID>")
  private String clientId;

  @Option(
      names = {"--azure-tenant-id"},
      description = "The unique identifier of the Azure Portal instance being used",
      required = true,
      paramLabel = "<TENANT_ID>")
  private String tenantId;

  @Option(
      names = {"--azure-client-secret"},
      description = "The secret used to access the vault (along with client-id)",
      required = true,
      paramLabel = "<CLIENT_SECRET>")
  private String clientSecret;

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
}
