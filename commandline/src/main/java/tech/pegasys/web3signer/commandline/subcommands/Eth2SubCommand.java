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

import static tech.pegasys.web3signer.slashingprotection.SlashingProtectionFactory.createSlashingProtection;

import tech.pegasys.web3signer.commandline.PicoCliAzureKeyVaultParameters;
import tech.pegasys.web3signer.core.Eth2Runner;
import tech.pegasys.web3signer.slashingprotection.SlashingProtection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;

import com.google.common.collect.Lists;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

@Command(
    name = Eth2SubCommand.COMMAND_NAME,
    description = "Handle Ethereum-2 BLS signing operations and public key reporting",
    mixinStandardHelpOptions = true)
public class Eth2SubCommand extends ModeSubCommand {

  public static final String COMMAND_NAME = "eth2";

  @Spec CommandSpec spec;

  @Command(name = "export", description = "Export slashing protection db to json file")
  public void exportSlashingDb(@Option(names = "--to") File output) {
    final SlashingProtection slashingProtection =
        createSlashingProtection(
            slashingProtectionDbUrl, slashingProtectionDbUsername, slashingProtectionDbPassword);
    try {
      slashingProtection.export(new FileOutputStream(output));
    } catch (final FileNotFoundException e) {
      throw new RuntimeException("Unable to find output target file", e);
    }
  }

  @Option(
      names = {"--slashing-protection-enabled"},
      description =
          "Set to true if all Eth2 signing operations should be validated against historic data, "
              + "prior to responding with signatures"
              + "(default: ${DEFAULT-VALUE})",
      paramLabel = "<BOOL>",
      arity = "1")
  private boolean slashingProtectionEnabled = true;

  @Option(
      names = {"--slashing-protection-db-url"},
      description = "The jdbc url to use to connect to the slashing protection database",
      paramLabel = "<jdbc url>",
      arity = "1")
  private String slashingProtectionDbUrl;

  @Option(
      names = {"--slashing-protection-db-username"},
      description = "The username to use when connecting to the slashing protection database",
      paramLabel = "<jdbc user>")
  private String slashingProtectionDbUsername;

  @Option(
      names = {"--slashing-protection-db-password"},
      description = "The password to use when connecting to the slashing protection database",
      paramLabel = "<jdbc password>")
  private String slashingProtectionDbPassword;

  @Mixin public PicoCliAzureKeyVaultParameters azureKeyVaultParameters;

  @Override
  public Eth2Runner createRunner() {
    validateArgs();
    return new Eth2Runner(
        config,
        slashingProtectionEnabled,
        slashingProtectionDbUrl,
        slashingProtectionDbUsername,
        slashingProtectionDbPassword,
        azureKeyVaultParameters);
  }

  private void validateArgs() {
    if (slashingProtectionEnabled && slashingProtectionDbUrl == null) {
      throw new ParameterException(spec.commandLine(), "Missing slashing protection database url");
    }

    if (azureKeyVaultParameters.isAzureKeyVaultEnabled()) {

      List<String> missingAzureFields = Lists.newArrayList();

      if (azureKeyVaultParameters.getClientSecret() == null) {
        missingAzureFields.add("--azure-client-secret");
      }

      if (azureKeyVaultParameters.getClientlId() == null) {
        missingAzureFields.add("--azure-client-id");
      }

      if (azureKeyVaultParameters.getTenantId() == null) {
        missingAzureFields.add("--azure-tenant-id");
      }

      if (azureKeyVaultParameters.getKeyVaultName() == null) {
        missingAzureFields.add("--azure-vault-name");
      }
      if (missingAzureFields.size() != 0) {
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
