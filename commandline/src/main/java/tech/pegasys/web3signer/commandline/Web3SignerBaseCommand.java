/*
 * Copyright 2019 ConsenSys AG.
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

import static tech.pegasys.web3signer.commandline.DefaultCommandValues.CONFIG_FILE_OPTION_NAME;
import static tech.pegasys.web3signer.commandline.DefaultCommandValues.MANDATORY_FILE_FORMAT_HELP;
import static tech.pegasys.web3signer.commandline.DefaultCommandValues.MANDATORY_HOST_FORMAT_HELP;
import static tech.pegasys.web3signer.commandline.DefaultCommandValues.MANDATORY_PORT_FORMAT_HELP;
import static tech.pegasys.web3signer.core.metrics.Web3SignerMetricCategory.DEFAULT_METRIC_CATEGORIES;

import tech.pegasys.web3signer.commandline.config.AllowListHostsProperty;
import tech.pegasys.web3signer.commandline.config.PicoCliTlsServerOptions;
import tech.pegasys.web3signer.commandline.config.PicoCliTlsServerOptionsValidator;
import tech.pegasys.web3signer.commandline.convertor.MetricCategoryConverter;
import tech.pegasys.web3signer.core.config.Config;
import tech.pegasys.web3signer.core.config.TlsOptions;
import tech.pegasys.web3signer.core.metrics.Web3SignerMetricCategory;
import tech.pegasys.web3signer.slashingprotection.SlashingMetricCategory;

import java.io.File;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.base.MoreObjects;
import org.apache.logging.log4j.Level;
import org.hyperledger.besu.metrics.StandardMetricCategory;
import org.hyperledger.besu.plugin.services.metrics.MetricCategory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

@SuppressWarnings("FieldCanBeLocal") // because Picocli injected fields report false positives
@Command(
    description =
        "This command runs the Web3Signer.\n"
            + "Documentation can be found at https://docs.web3signer.pegasys.tech.",
    abbreviateSynopsis = true,
    name = "web3signer",
    sortOptions = false,
    mixinStandardHelpOptions = true,
    versionProvider = VersionProvider.class,
    header = "Usage:",
    synopsisHeading = "%n",
    descriptionHeading = "%nDescription:%n%n",
    optionListHeading = "%nOptions:%n",
    footerHeading = "%n",
    subcommands = {HelpCommand.class},
    footer = "Web3Signer is licensed under the Apache License 2.0")
public class Web3SignerBaseCommand implements Config, Runnable {

  @Spec private CommandLine.Model.CommandSpec spec; // injected by picocli

  @SuppressWarnings("UnusedVariable")
  @CommandLine.Option(
      names = {CONFIG_FILE_OPTION_NAME},
      paramLabel = MANDATORY_FILE_FORMAT_HELP,
      description = "Config file in yaml format (default: none)")
  private final File configFile = null;

  @Option(
      names = {"--data-path"},
      description = "The path to a directory to store temporary files",
      paramLabel = DefaultCommandValues.MANDATORY_PATH_FORMAT_HELP,
      arity = "1")
  private Path dataPath;

  @Option(
      names = {"--key-store-path"},
      description = "The path to a directory storing toml files defining available keys",
      paramLabel = DefaultCommandValues.MANDATORY_PATH_FORMAT_HELP,
      arity = "1")
  private Path keyStorePath = Path.of("./");

  @Option(
      names = {"--logging", "-l"},
      paramLabel = "<LOG VERBOSITY LEVEL>",
      description =
          "Logging verbosity levels: OFF, FATAL, WARN, INFO, DEBUG, TRACE, ALL (default: INFO)")
  private final Level logLevel = null;

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = {"--http-listen-host"},
      description = "Host for HTTP to listen on (default: ${DEFAULT-VALUE})",
      paramLabel = MANDATORY_HOST_FORMAT_HELP,
      arity = "1")
  private String httpListenHost = InetAddress.getLoopbackAddress().getHostAddress();

  @Option(
      names = {"--http-listen-port"},
      description = "Port for HTTP to listen on (default: ${DEFAULT-VALUE})",
      paramLabel = MANDATORY_PORT_FORMAT_HELP,
      arity = "1")
  private final Integer httpListenPort = 9000;

  @Option(
      names = {"--http-host-allowlist"},
      paramLabel = "<hostname>[,<hostname>...]... or * or all",
      description =
          "Comma separated list of hostnames to allow for http access, or * to accept any host (default: ${DEFAULT-VALUE})",
      defaultValue = "localhost,127.0.0.1")
  private final AllowListHostsProperty httpHostAllowList = new AllowListHostsProperty();

  @Option(
      names = {"--metrics-enabled"},
      description = "Set to start the metrics exporter (default: ${DEFAULT-VALUE})")
  private final Boolean metricsEnabled = false;

  @SuppressWarnings({"FieldCanBeFinal", "FieldMayBeFinal"}) // PicoCLI requires non-final Strings.
  @Option(
      names = {"--metrics-host"},
      paramLabel = MANDATORY_HOST_FORMAT_HELP,
      description = "Host for the metrics exporter to listen on (default: ${DEFAULT-VALUE})",
      arity = "1")
  private String metricsHost = InetAddress.getLoopbackAddress().getHostAddress();

  @Option(
      names = {"--metrics-port"},
      paramLabel = MANDATORY_PORT_FORMAT_HELP,
      description = "Port for the metrics exporter to listen on (default: ${DEFAULT-VALUE})",
      arity = "1")
  private final Integer metricsPort = 9001;

  @Option(
      names = {"--metrics-category", "--metrics-categories"},
      paramLabel = "<category name>",
      split = ",",
      arity = "1..*",
      description =
          "Comma separated list of categories to track metrics for (default: ${DEFAULT-VALUE}),",
      converter = Web3signerMetricCategoryConverter.class)
  private final Set<MetricCategory> metricCategories = DEFAULT_METRIC_CATEGORIES;

  @Option(
      names = {"--metrics-host-allowlist"},
      paramLabel = "<hostname>[,<hostname>...]... or * or all",
      description =
          "Comma separated list of hostnames to allow for metrics access, or * to accept any host (default: ${DEFAULT-VALUE})",
      defaultValue = "localhost,127.0.0.1")
  private final AllowListHostsProperty metricsHostAllowList = new AllowListHostsProperty();

  @Option(
      names = {"--idle-connection-timeout-seconds"},
      paramLabel = "<timeout in seconds>",
      description =
          "Number of seconds after which an idle connection will be terminated (Default: ${DEFAULT-VALUE})",
      arity = "1")
  private int idleConnectionTimeoutSeconds = 30;

  @Option(
      names = {"--swagger-ui-enabled"},
      description = "Enable swagger UI (default: ${DEFAULT-VALUE})")
  private final Boolean swaggerUiEnabled = false;

  @Option(
      names = {"--access-logs-enabled"},
      description = "Enable access logs (default: ${DEFAULT-VALUE})")
  private final Boolean accessLogsEnabled = false;

  @CommandLine.Mixin private PicoCliTlsServerOptions picoCliTlsServerOptions;

  @Override
  public Level getLogLevel() {
    return logLevel;
  }

  @Override
  public String getHttpListenHost() {
    return httpListenHost;
  }

  @Override
  public Integer getHttpListenPort() {
    return httpListenPort;
  }

  @Override
  public AllowListHostsProperty getHttpHostAllowList() {
    return httpHostAllowList;
  }

  @Override
  public Path getDataPath() {
    return dataPath;
  }

  @Override
  public Path getKeyConfigPath() {
    return keyStorePath;
  }

  @Override
  public Boolean isMetricsEnabled() {
    return metricsEnabled;
  }

  @Override
  public Integer getMetricsPort() {
    return metricsPort;
  }

  @Override
  public String getMetricsNetworkInterface() {
    return metricsHost;
  }

  @Override
  public Set<MetricCategory> getMetricCategories() {
    return metricCategories;
  }

  @Override
  public List<String> getMetricsHostAllowList() {
    return metricsHostAllowList;
  }

  @Override
  public Optional<TlsOptions> getTlsOptions() {
    if (picoCliTlsServerOptions.getKeyStoreFile() != null
        && picoCliTlsServerOptions.getKeyStorePasswordFile() != null) {
      return Optional.of(picoCliTlsServerOptions);
    }
    return Optional.empty();
  }

  @Override
  public int getIdleConnectionTimeoutSeconds() {
    return idleConnectionTimeoutSeconds;
  }

  @Override
  public Boolean isSwaggerUIEnabled() {
    return swaggerUiEnabled;
  }

  @Override
  public Boolean isAccessLogsEnabled() {
    return accessLogsEnabled;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("configFile", configFile)
        .add("dataPath", dataPath)
        .add("keyStorePath", keyStorePath)
        .add("logLevel", logLevel)
        .add("httpListenHost", httpListenHost)
        .add("httpListenPort", httpListenPort)
        .add("httpHostAllowList", httpHostAllowList)
        .add("metricsEnabled", metricsEnabled)
        .add("metricsHost", metricsHost)
        .add("metricsPort", metricsPort)
        .add("metricCategories", metricCategories)
        .add("metricsHostAllowList", metricsHostAllowList)
        .add("picoCliTlsServerOptions", picoCliTlsServerOptions)
        .add("idleConnectionTimeoutSeconds", idleConnectionTimeoutSeconds)
        .toString();
  }

  @Override
  public void run() {
    throw new ParameterException(spec.commandLine(), "Missing required subcommand");
  }

  @Override
  public void validateArgs() {
    // custom validation for TLS options as we removed ArgGroups since they don't work with config
    // files
    final PicoCliTlsServerOptionsValidator picoCliTlsServerOptionsValidator =
        new PicoCliTlsServerOptionsValidator(spec, picoCliTlsServerOptions);
    picoCliTlsServerOptionsValidator.validate();
  }

  public static class Web3signerMetricCategoryConverter extends MetricCategoryConverter {

    public Web3signerMetricCategoryConverter() {
      addCategories(Web3SignerMetricCategory.class);
      addCategories(StandardMetricCategory.class);
      addCategories(SlashingMetricCategory.class);
    }
  }
}
