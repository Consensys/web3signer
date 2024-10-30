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

import static tech.pegasys.web3signer.commandline.PicoCliAwsSecretsManagerParameters.AWS_SECRETS_ACCESS_KEY_ID_OPTION;
import static tech.pegasys.web3signer.commandline.PicoCliAwsSecretsManagerParameters.AWS_SECRETS_AUTH_MODE_OPTION;
import static tech.pegasys.web3signer.commandline.PicoCliAwsSecretsManagerParameters.AWS_SECRETS_REGION_OPTION;
import static tech.pegasys.web3signer.commandline.PicoCliAwsSecretsManagerParameters.AWS_SECRETS_SECRET_ACCESS_KEY_OPTION;
import static tech.pegasys.web3signer.signing.config.AzureAuthenticationMode.CLIENT_SECRET;
import static tech.pegasys.web3signer.signing.config.AzureAuthenticationMode.USER_ASSIGNED_MANAGED_IDENTITY;

import tech.pegasys.teku.infrastructure.collections.TekuPair;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.networks.Eth2NetworkConfiguration;
import tech.pegasys.teku.spec.ForkSchedule;
import tech.pegasys.teku.spec.SpecMilestone;
import tech.pegasys.teku.spec.networks.Eth2Network;
import tech.pegasys.web3signer.commandline.PicoCliAwsSecretsManagerParameters;
import tech.pegasys.web3signer.commandline.PicoCliEth2AzureKeyVaultParameters;
import tech.pegasys.web3signer.commandline.PicoCliGcpSecretManagerParameters;
import tech.pegasys.web3signer.commandline.PicoCliSlashingProtectionParameters;
import tech.pegasys.web3signer.commandline.config.PicoCommitBoostApiParameters;
import tech.pegasys.web3signer.commandline.config.PicoKeystoresParameters;
import tech.pegasys.web3signer.common.config.AwsAuthenticationMode;
import tech.pegasys.web3signer.core.Eth2Runner;
import tech.pegasys.web3signer.core.Runner;
import tech.pegasys.web3signer.signing.config.KeystoresParameters;
import tech.pegasys.web3signer.slashingprotection.SlashingProtectionParameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    subcommands = {
      HelpCommand.class,
      Eth2ExportSubCommand.class,
      Eth2ImportSubCommand.class,
      Eth2WatermarkRepairSubCommand.class
    },
    mixinStandardHelpOptions = true)
public class Eth2SubCommand extends ModeSubCommand {
  private static final Logger LOG = LogManager.getLogger();
  public static final String COMMAND_NAME = "eth2";

  private static class NetworkCliCompletionCandidates extends ArrayList<String> {
    NetworkCliCompletionCandidates() {
      super(
          Arrays.stream(Eth2Network.values())
              .map(Eth2Network::configName)
              .collect(Collectors.toList()));
    }
  }

  @Spec CommandSpec commandSpec;

  @CommandLine.Option(
      names = {"--network"},
      paramLabel = "<NETWORK>",
      defaultValue = "mainnet",
      completionCandidates = NetworkCliCompletionCandidates.class,
      description =
          "Predefined network configuration to use. Possible values: [${COMPLETION-CANDIDATES}], file path"
              + " or URL to a YAML configuration file. Defaults to ${DEFAULT-VALUE}.",
      arity = "1")
  private String network;

  @CommandLine.Option(
      names = {"--Xnetwork-altair-fork-epoch"},
      hidden = true,
      paramLabel = "<epoch>",
      description = "Override the Altair fork activation epoch.",
      arity = "1",
      converter = UInt64Converter.class)
  private UInt64 altairForkEpoch;

  @CommandLine.Option(
      names = {"--Xnetwork-bellatrix-fork-epoch"},
      hidden = true,
      paramLabel = "<epoch>",
      description = "Override the Bellatrix fork activation epoch.",
      arity = "1",
      converter = UInt64Converter.class)
  private UInt64 bellatrixForkEpoch;

  @CommandLine.Option(
      names = {"--Xnetwork-capella-fork-epoch"},
      hidden = true,
      paramLabel = "<epoch>",
      description = "Override the Capella fork activation epoch.",
      arity = "1",
      converter = UInt64Converter.class)
  private UInt64 capellaForkEpoch;

  @CommandLine.Option(
      names = {"--Xnetwork-deneb-fork-epoch"},
      hidden = true,
      paramLabel = "<epoch>",
      description = "Override the Deneb fork activation epoch.",
      arity = "1",
      converter = UInt64Converter.class)
  private UInt64 denebForkEpoch;

  @CommandLine.Option(
      names = {"--Xnetwork-electra-fork-epoch"},
      hidden = true,
      paramLabel = "<epoch>",
      description = "Override the Electra fork activation epoch.",
      arity = "1",
      converter = UInt64Converter.class)
  private UInt64 electraForkEpoch;

  @CommandLine.Option(
      names = {"--Xtrusted-setup"},
      hidden = true,
      paramLabel = "<STRING>",
      description =
          "The trusted setup which is needed for KZG commitments. Only required when creating a custom network. This value should be a file or URL pointing to a trusted setup.",
      arity = "1")
  private String trustedSetup = null; // Depends on network configuration

  @CommandLine.Option(
      names = {"--key-manager-api-enabled", "--enable-key-manager-api"},
      paramLabel = "<BOOL>",
      description = "Enable the key manager API to manage key stores (default: ${DEFAULT-VALUE}).",
      arity = "1")
  private boolean isKeyManagerApiEnabled = false;

  @CommandLine.Option(
      names = "--Xsigning-ext-enabled",
      description = "Set to true to enable signing extensions.",
      paramLabel = "<BOOL>",
      arity = "1",
      hidden = true)
  private boolean signingExtEnabled = false;

  @Mixin private PicoCliSlashingProtectionParameters slashingProtectionParameters;
  @Mixin private PicoCliEth2AzureKeyVaultParameters azureKeyVaultParameters;
  @Mixin private PicoKeystoresParameters keystoreParameters;
  @Mixin private PicoCliAwsSecretsManagerParameters awsSecretsManagerParameters;
  @Mixin private PicoCliGcpSecretManagerParameters gcpSecretManagerParameters;
  @Mixin private PicoCommitBoostApiParameters commitBoostApiParameters;
  private tech.pegasys.teku.spec.Spec eth2Spec;

  public Eth2SubCommand() {
    network = "mainnet";
  }

  @Override
  public Runner createRunner() {
    logNetworkSpecInformation();

    return new Eth2Runner(
        config,
        slashingProtectionParameters,
        azureKeyVaultParameters,
        keystoreParameters,
        awsSecretsManagerParameters,
        gcpSecretManagerParameters,
        eth2Spec,
        isKeyManagerApiEnabled,
        signingExtEnabled,
        commitBoostApiParameters);
  }

  private void logNetworkSpecInformation() {
    final ForkSchedule forkSchedule = eth2Spec.getForkSchedule();
    final Map<SpecMilestone, UInt64> milestoneSlotMap =
        forkSchedule
            .streamMilestoneBoundarySlots()
            .collect(Collectors.toMap(TekuPair::getLeft, TekuPair::getRight));

    final StringBuilder logString = new StringBuilder(String.format("Network: %s%n", network));
    forkSchedule
        .getActiveMilestones()
        .forEach(
            m -> {
              final String specName = m.getSpecMilestone().name();
              final UInt64 forkEpoch = m.getFork().getEpoch();
              final UInt64 boundarySlot = milestoneSlotMap.get(m.getSpecMilestone());
              logString.append(
                  String.format(
                      "Spec Name: %s, Fork Epoch: %s, First Slot: %s%n",
                      specName, forkEpoch, boundarySlot));
            });
    LOG.info(logString);
  }

  private Eth2NetworkConfiguration createEth2NetworkConfig() {
    Eth2NetworkConfiguration.Builder builder = Eth2NetworkConfiguration.builder();
    builder.applyNetworkDefaults(network);
    if (altairForkEpoch != null) {
      builder.altairForkEpoch(altairForkEpoch);
    }
    if (bellatrixForkEpoch != null) {
      builder.bellatrixForkEpoch(bellatrixForkEpoch);
    }
    if (capellaForkEpoch != null) {
      builder.capellaForkEpoch(capellaForkEpoch);
    }
    if (denebForkEpoch != null) {
      builder.denebForkEpoch(denebForkEpoch);
    }
    if (electraForkEpoch != null) {
      builder.electraForkEpoch(electraForkEpoch);
    }
    if (trustedSetup != null) {
      builder.trustedSetup(trustedSetup);
    }
    return builder.build();
  }

  @Override
  protected void validateArgs() {
    try {
      Eth2NetworkConfiguration eth2NetworkConfig = createEth2NetworkConfig();
      eth2Spec = eth2NetworkConfig.getSpec();
    } catch (final IllegalArgumentException e) {
      throw new ParameterException(
          commandSpec.commandLine(),
          "Failed to load network " + network + " due to " + e.getMessage(),
          e);
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

    validateAzureParameters();
    validateKeystoreParameters(keystoreParameters);
    validateAwsSecretsManageParameters();
    validateGcpSecretManagerParameters();
    commitBoostApiParameters.validateParameters();
  }

  private void validateGcpSecretManagerParameters() {
    if (gcpSecretManagerParameters.isEnabled()) {
      final List<String> specifiedAuthModeMissingFields =
          missingGcpSecretManagerParametersForSpecified();
      if (!specifiedAuthModeMissingFields.isEmpty()) {
        final String errorMsg =
            String.format(
                "%s=%s, but the following parameters were missing [%s].",
                PicoCliGcpSecretManagerParameters.GCP_SECRETS_ENABLED_OPTION,
                true,
                String.join(", ", specifiedAuthModeMissingFields));
        throw new ParameterException(commandSpec.commandLine(), errorMsg);
      }
    }
  }

  private void validateAzureParameters() {
    if (azureKeyVaultParameters.isAzureKeyVaultEnabled()) {
      final List<String> missingAzureFields = missingAzureFields();
      if (!missingAzureFields.isEmpty()) {
        final String errorMsg =
            String.format(
                "Azure Key Vault was enabled, but the following parameters were missing [%s].",
                String.join(",", missingAzureFields));
        throw new ParameterException(commandSpec.commandLine(), errorMsg);
      }
    }
  }

  private List<String> missingAzureFields() {
    final List<String> missingFields = Lists.newArrayList();
    if (azureKeyVaultParameters.getKeyVaultName() == null) {
      missingFields.add("--azure-vault-name");
    }
    if (azureKeyVaultParameters.getAuthenticationMode() == CLIENT_SECRET) {
      // client secret authentication mode requires all of following options
      if (azureKeyVaultParameters.getClientSecret() == null) {
        missingFields.add("--azure-client-secret");
      }

      if (azureKeyVaultParameters.getKeyVaultName() == null) {
        missingFields.add("--azure-vault-name");
      }

      if (azureKeyVaultParameters.getTenantId() == null) {
        missingFields.add("--azure-tenant-id");
      }
    } else if (azureKeyVaultParameters.getAuthenticationMode() == USER_ASSIGNED_MANAGED_IDENTITY) {
      if (azureKeyVaultParameters.getClientId() == null) {
        missingFields.add("--azure-client-id");
      }
    }
    return missingFields;
  }

  private void validateKeystoreParameters(final KeystoresParameters keystoresParameters) {
    if (keystoresParameters.hasKeystoresPasswordsPath()
        && keystoresParameters.hasKeystoresPasswordFile()) {
      throw new ParameterException(
          commandSpec.commandLine(),
          "Only one of --keystores-passwords-path or --keystores-password-file options can be specified");
    }
  }

  private void validateAwsSecretsManageParameters() {
    if (awsSecretsManagerParameters.isEnabled()) {
      final List<String> specifiedAuthModeMissingFields =
          missingAwsSecretsManagerParametersForSpecified();
      if (!specifiedAuthModeMissingFields.isEmpty()) {
        final String errorMsg =
            String.format(
                "%s=%s, but the following parameters were missing [%s].",
                AWS_SECRETS_AUTH_MODE_OPTION,
                AwsAuthenticationMode.SPECIFIED,
                String.join(", ", specifiedAuthModeMissingFields));
        throw new ParameterException(commandSpec.commandLine(), errorMsg);
      }
    }
  }

  private List<String> missingGcpSecretManagerParametersForSpecified() {
    final List<String> missingFields = Lists.newArrayList();
    if (gcpSecretManagerParameters.getProjectId() == null) {
      missingFields.add(PicoCliGcpSecretManagerParameters.GCP_PROJECT_ID_OPTION);
    }
    return missingFields;
  }

  private List<String> missingAwsSecretsManagerParametersForSpecified() {
    final List<String> missingFields = Lists.newArrayList();
    if (awsSecretsManagerParameters.getAuthenticationMode() == AwsAuthenticationMode.SPECIFIED) {
      if (awsSecretsManagerParameters.getAccessKeyId() == null) {
        missingFields.add(AWS_SECRETS_ACCESS_KEY_ID_OPTION);
      }

      if (awsSecretsManagerParameters.getSecretAccessKey() == null) {
        missingFields.add(AWS_SECRETS_SECRET_ACCESS_KEY_OPTION);
      }

      if (awsSecretsManagerParameters.getRegion() == null) {
        missingFields.add(AWS_SECRETS_REGION_OPTION);
      }
    }

    return missingFields;
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

  @VisibleForTesting
  public PicoCliAwsSecretsManagerParameters getAwsSecretsManagerParameters() {
    return awsSecretsManagerParameters;
  }

  public static class UInt64Converter implements CommandLine.ITypeConverter<UInt64> {
    @Override
    public UInt64 convert(final String value) throws Exception {
      return UInt64.valueOf(value);
    }
  }
}
