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

import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.SpecFactory;
import tech.pegasys.teku.spec.networks.Eth2Network;
import tech.pegasys.web3signer.commandline.PicoCliAzureKeyVaultParameters;
import tech.pegasys.web3signer.commandline.PicoCliSlashingProtectionParameters;
import tech.pegasys.web3signer.core.Eth2Runner;
import tech.pegasys.web3signer.core.Runner;
import tech.pegasys.web3signer.slashingprotection.SlashingProtectionParameters;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import picocli.CommandLine;
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

  @Spec CommandSpec commandSpec;

  @CommandLine.Option(
      names = {"--network"},
      paramLabel = "<NETWORK>",
      description =
          "Predefined network configuration to use. Possible values: [mainnet, pyrmont, prater, minimal], file path"
              + " or URL to a YAML configuration file. Defaults to mainnet.",
      arity = "1")
  private String network = "mainnet";

  @CommandLine.Option(
      names = {"--Xnetwork-altair-fork-epoch"},
      hidden = true,
      paramLabel = "<epoch>",
      description = "Override the Altair fork activation epoch.",
      arity = "1",
      converter = UInt64Converter.class)
  private UInt64 altairForkEpoch;

  @Mixin private PicoCliSlashingProtectionParameters slashingProtectionParameters;
  @Mixin private PicoCliAzureKeyVaultParameters azureKeyVaultParameters;
  private tech.pegasys.teku.spec.Spec eth2Spec;

  @Override
  public Runner createRunner() {
    return new Eth2Runner(config, slashingProtectionParameters, azureKeyVaultParameters, eth2Spec);
  }

  @Override
  protected void validateArgs() {
    final String networkConfigName =
        Eth2Network.fromStringLenient(network).map(Eth2Network::configName).orElse(network);
    try {
      eth2Spec = SpecFactory.create(networkConfigName, Optional.ofNullable(altairForkEpoch));
    } catch (final IllegalArgumentException e) {
      throw new ParameterException(
          commandSpec.commandLine(), "Failed to load network spec: " + networkConfigName, e);
    }

    if (slashingProtectionParameters.isEnabled()
        && slashingProtectionParameters.getDbUrl() == null) {
      throw new ParameterException(
          commandSpec.commandLine(), "Missing slashing protection database url");
    }

    validatePositiveValue(
        slashingProtectionParameters.getPruningEpochsToKeep(), "Pruning epochsToKeep");
    validatePositiveValue(slashingProtectionParameters.getPruningInterval(), "Pruning interval");
    validatePositiveValue(
        slashingProtectionParameters.getPruningSlotsPerEpoch(), "Pruning slots per epoch");

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
        throw new ParameterException(commandSpec.commandLine(), errorMsg);
      }
    }
  }

  private void validatePositiveValue(final long value, final String fieldName) {
    if (value < 1) {
      throw new ParameterException(
          commandSpec.commandLine(),
          String.format("%s must be 1 or more. Value was %d.", fieldName, value));
    }
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }

  public SlashingProtectionParameters getSlashingProtectionParameters() {
    return slashingProtectionParameters;
  }

  static class UInt64Converter implements CommandLine.ITypeConverter<UInt64> {
    @Override
    public UInt64 convert(final String value) throws Exception {
      return UInt64.valueOf(value);
    }
  }
}
