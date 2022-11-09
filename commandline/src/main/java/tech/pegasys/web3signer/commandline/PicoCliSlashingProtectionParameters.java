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

import tech.pegasys.web3signer.slashingprotection.SlashingProtectionParameters;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import picocli.CommandLine.Option;

public class PicoCliSlashingProtectionParameters implements SlashingProtectionParameters {
  @Option(
      names = {"--slashing-protection-enabled"},
      description =
          "Set to true if all Eth2 signing operations should be validated against historic data, "
              + "prior to responding with signatures"
              + "(default: ${DEFAULT-VALUE})",
      paramLabel = "<BOOL>",
      arity = "1")
  boolean enabled = true;

  @Option(
      names = {"--slashing-protection-db-url"},
      description = "The jdbc url to use to connect to the slashing protection database",
      paramLabel = "<jdbc url>",
      arity = "1")
  String dbUrl;

  @Option(
      names = {"--slashing-protection-db-username"},
      description = "The username to use when connecting to the slashing protection database",
      paramLabel = "<jdbc user>")
  String dbUsername;

  @Option(
      names = {"--slashing-protection-db-password"},
      description = "The password to use when connecting to the slashing protection database",
      paramLabel = "<jdbc password>")
  String dbPassword;

  @Option(
      names = "--slashing-protection-db-pool-configuration-file",
      description = "Optional configuration file for Hikari database connection pool.",
      paramLabel = "<hikari configuration properties file>")
  private Path dbPoolConfigurationFile = null;

  @Option(
      names = "--slashing-protection-pruning-db-pool-configuration-file",
      description =
          "Optional configuration file for Hikari database connection pool used by pruning process.",
      paramLabel = "<hikari configuration properties file>")
  private Path pruningDbPoolConfigurationFile = null;

  @Option(
      names = {"--slashing-protection-pruning-enabled"},
      description =
          "Set to true if all Eth2 slashing protection database should be pruned "
              + "(default: ${DEFAULT-VALUE})",
      paramLabel = "<BOOL>",
      arity = "1")
  boolean pruningEnabled = true;

  @Option(
      names = {"--slashing-protection-pruning-epochs-to-keep"},
      description = "Number of epochs to keep. (default: ${DEFAULT-VALUE})",
      arity = "1")
  long pruningEpochsToKeep = 250;

  @Option(
      names = {"--slashing-protection-pruning-slots-per-epoch"},
      description =
          "Slots per epoch to use when calculating the number of slots to prune for signed"
              + " blocks. This typically will not need changing and defaults to value used on mainnet "
              + "(default: ${DEFAULT-VALUE})")
  long pruningSlotsPerEpoch = 32;

  @Option(
      names = {"--slashing-protection-pruning-interval"},
      description = "Hours between pruning operations (default: ${DEFAULT-VALUE})")
  long pruningInterval = 12;

  @Option(
      names = {"--slashing-protection-pruning-at-boot-enabled"},
      description =
          "Set to true to enable slashing protection pruning logic at server boot"
              + "(default: ${DEFAULT-VALUE})",
      paramLabel = "<BOOL>",
      arity = "1")
  boolean pruningAtBootEnabled = false;

  @Option(
      names = {"--slashing-protection-db-health-check-timeout-milliseconds"},
      paramLabel = "<timeout in milliseconds>",
      description =
          "Number of milliseconds after which the database health check will be failed (Default: ${DEFAULT-VALUE})",
      arity = "1")
  private long dbHealthCheckTimeoutMilliseconds = 3000;

  @Option(
      names = {"--slashing-protection-db-health-check-interval-milliseconds"},
      paramLabel = "<interval in milliseconds>",
      description =
          "Number of milliseconds between the database health check operation (Default: ${DEFAULT-VALUE})",
      arity = "1")
  private long dbHealthCheckIntervalMilliseconds = 30000;

  @Option(
      names = "--Xslashing-protection-db-connection-pool-enabled",
      description =
          "Set to false to disable internal database connection pooling. Should only be disabled when using an external database connection pool. (Default: ${DEFAULT-VALUE})",
      paramLabel = "<BOOL>",
      arity = "1",
      hidden = true)
  private boolean dbConnectionPoolEnabled = true;

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public String getDbUrl() {
    return dbUrl;
  }

  @Override
  public String getDbUsername() {
    return dbUsername;
  }

  @Override
  public String getDbPassword() {
    return dbPassword;
  }

  @Override
  public Path getDbPoolConfigurationFile() {
    return dbPoolConfigurationFile;
  }

  @Override
  public Path getPruningDbPoolConfigurationFile() {
    return pruningDbPoolConfigurationFile;
  }

  @Override
  public boolean isPruningEnabled() {
    return pruningEnabled;
  }

  @Override
  public long getPruningEpochsToKeep() {
    return pruningEpochsToKeep;
  }

  @Override
  public long getPruningSlotsPerEpoch() {
    return pruningSlotsPerEpoch;
  }

  @Override
  public long getPruningInterval() {
    return pruningInterval;
  }

  @Override
  public TimeUnit getPruningIntervalTimeUnit() {
    return TimeUnit.HOURS;
  }

  @Override
  public boolean isPruningAtBootEnabled() {
    return pruningAtBootEnabled;
  }

  @Override
  public long getDbHealthCheckTimeoutMilliseconds() {
    return dbHealthCheckTimeoutMilliseconds;
  }

  @Override
  public long getDbHealthCheckIntervalMilliseconds() {
    return dbHealthCheckIntervalMilliseconds;
  }

  @Override
  public boolean isDbConnectionPoolEnabled() {
    return dbConnectionPoolEnabled;
  }
}
