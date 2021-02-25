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
package tech.pegasys.web3signer.commandline.subcommands;

import static tech.pegasys.web3signer.core.config.AzureAuthenticationMode.CLIENT_SECRET;
import static tech.pegasys.web3signer.core.config.AzureAuthenticationMode.USER_ASSIGNED_MANAGED_IDENTITY;

import tech.pegasys.web3signer.commandline.PicoCliAzureKeyVaultParameters;
import tech.pegasys.web3signer.commandline.SlashingProtectionParameters;
import tech.pegasys.web3signer.core.Eth2Runner;
import tech.pegasys.web3signer.core.Runner;

import java.util.List;

import com.google.common.collect.Lists;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

@Command(
    name = Eth2SubCommand.COMMAND_NAME,
    description = "Handle Ethereum-2 BLS signing operations and public key reporting",
    subcommands = {HelpCommand.class, Eth2ExportSubCommand.class, Eth2ImportSubCommand.class},
    mixinStandardHelpOptions = true)
public class Eth2SubCommand extends ModeSubCommand {

  public static final String COMMAND_NAME = "eth2";

  @Spec CommandSpec spec;

  @Mixin public SlashingProtectionParameters slashingProtectionParameters;
  @Mixin public PicoCliAzureKeyVaultParameters azureKeyVaultParameters;

  @Override
  public Runner createRunner() {
    return new Eth2Runner(
        config,
        slashingProtectionParameters.isEnabled(),
        slashingProtectionParameters.getDbUrl(),
        slashingProtectionParameters.getDbUsername(),
        slashingProtectionParameters.getDbPassword(),
        slashingProtectionParameters.isPruningEnabled(),
        slashingProtectionParameters.getPruningEpochs(),
        slashingProtectionParameters.getPruningEpochsPerSlot(),
        slashingProtectionParameters.getPruningPeriod(),
        azureKeyVaultParameters);
  }

  @Override
  protected void validateArgs() {
    if (slashingProtectionParameters.isEnabled()
        && slashingProtectionParameters.getDbUrl() == null) {
      throw new ParameterException(spec.commandLine(), "Missing slashing protection database url");
    }

    if (azureKeyVaultParameters.isAzureKeyVaultEnabled()) {

      final List<String> missingAzureFields = Lists.newArrayList();

      if (azureKeyVaultParameters.getKeyVaultName() == null) {
        missingAzureFields.add("--azure-vault-name");
      }

      if (azureKeyVaultParameters.getAuthenticationMode() == CLIENT_SECRET) {
        // client secret authentication mode requires all of following options
        if (azureKeyVaultParameters.getClientSecret() == null) {
          missingAzureFields.add("--azure-client-secret");
        }

        if (azureKeyVaultParameters.getClientId() == null) {
          missingAzureFields.add("--azure-client-id");
        }

        if (azureKeyVaultParameters.getTenantId() == null) {
          missingAzureFields.add("--azure-tenant-id");
        }
      } else if (azureKeyVaultParameters.getAuthenticationMode()
          == USER_ASSIGNED_MANAGED_IDENTITY) {
        if (azureKeyVaultParameters.getClientId() == null) {
          missingAzureFields.add("--azure-client-id");
        }
      }

      // no extra validation required for "system-assigned managed identity".

      if (!missingAzureFields.isEmpty()) {
        final String errorMsg =
            String.format(
                "Azure Key Vault was enabled, but the following parameters were missing [%s].",
                String.join(",", missingAzureFields));
        throw new ParameterException(spec.commandLine(), errorMsg);
      }
    }
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }
}
