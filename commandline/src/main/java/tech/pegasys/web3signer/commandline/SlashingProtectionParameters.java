/*
 * Copyright 2021 ConsenSys AG.
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

import picocli.CommandLine.Option;

public class SlashingProtectionParameters {
  @Option(
      names = {"--slashing-protection-enabled"},
      description =
          "Set to true if all Eth2 signing operations should be validated against historic data, "
              + "prior to responding with signatures"
              + "(default: ${DEFAULT-VALUE})",
      paramLabel = "<BOOL>",
      arity = "1")
  boolean slashingProtectionEnabled = true;

  @Option(
      names = {"--slashing-protection-db-url"},
      description = "The jdbc url to use to connect to the slashing protection database",
      paramLabel = "<jdbc url>",
      arity = "1")
  String slashingProtectionDbUrl;

  @Option(
      names = {"--slashing-protection-db-username"},
      description = "The username to use when connecting to the slashing protection database",
      paramLabel = "<jdbc user>")
  String slashingProtectionDbUsername;

  @Option(
      names = {"--slashing-protection-db-password"},
      description = "The password to use when connecting to the slashing protection database",
      paramLabel = "<jdbc password>")
  String slashingProtectionDbPassword;

  @Option(
      names = {"--slashing-protection-pruning-enabled"},
      description =
          "Set to true if all Eth2 slashing protection database should be pruned "
              + "(default: ${DEFAULT-VALUE})",
      paramLabel = "<BOOL>",
      arity = "1")
  boolean slashingProtectionPruningEnabled = true;

  @Option(
      names = {"--slashing-protection-pruning-epochs"},
      description =
          "Number of epochs back from latest epoch where data should be pruned to "
              + "(default: ${DEFAULT-VALUE})",
      arity = "1")
  long slashingProtectionPruningEpochs = 10_000;

  @Option(
      names = {"--slashing-protection-pruning-slots-per-epoch"},
      description =
          "Slots per epoch to use when calculating the number of slots to prune for signed"
              + " blocks. This typically will not need changing and defaults to value used on mainnet "
              + "(default: ${DEFAULT-VALUE})")
  long slashingProtectionPruningEpochsPerSlot = 32;

  @Option(
      names = {"--slashing-protection-pruning-period"},
      description =
          "How often the pruning process should be run in hours (default: ${DEFAULT-VALUE})")
  long slashingProtectionPruningPeriod = 12;

  public boolean isEnabled() {
    return slashingProtectionEnabled;
  }

  public String getDbUrl() {
    return slashingProtectionDbUrl;
  }

  public String getDbUsername() {
    return slashingProtectionDbUsername;
  }

  public String getDbPassword() {
    return slashingProtectionDbPassword;
  }

  public boolean isSlashingProtectionPruningEnabled() {
    return slashingProtectionPruningEnabled;
  }

  public long getSlashingProtectionPruningEpochs() {
    return slashingProtectionPruningEpochs;
  }

  public long getSlashingProtectionPruningEpochsPerSlot() {
    return slashingProtectionPruningEpochsPerSlot;
  }

  public long getSlashingProtectionPruningPeriod() {
    return slashingProtectionPruningPeriod;
  }
}
