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
package tech.pegasys.eth2signer;

import static tech.pegasys.eth2signer.DefaultCommandValues.MANDATORY_HOST_FORMAT_HELP;
import static tech.pegasys.eth2signer.DefaultCommandValues.MANDATORY_LONG_FORMAT_HELP;
import static tech.pegasys.eth2signer.DefaultCommandValues.MANDATORY_PATH_FORMAT_HELP;
import static tech.pegasys.eth2signer.DefaultCommandValues.MANDATORY_PORT_FORMAT_HELP;

import tech.pegasys.eth2signer.config.InvalidCommandLineOptionsException;
import tech.pegasys.eth2signer.config.PicoCliTlsServerOptions;
import tech.pegasys.eth2signer.config.tls.client.PicoCliClientTlsOptions;
import tech.pegasys.eth2signer.core.config.Config;
import tech.pegasys.eth2signer.core.config.TlsOptions;
import tech.pegasys.eth2signer.core.config.tls.client.ClientTlsOptions;
import tech.pegasys.eth2signer.core.signing.ChainIdProvider;
import tech.pegasys.eth2signer.core.signing.ConfigurationChainId;

import java.net.InetAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import com.google.common.base.MoreObjects;
import org.apache.logging.log4j.Level;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;

@SuppressWarnings("FieldCanBeLocal") // because Picocli injected fields report false positives
@Command(
    description =
        "This command runs the EthSigner.\n"
            + "Documentation can be found at https://docs.ethsigner.pegasys.tech.",
    abbreviateSynopsis = true,
    name = "ethsigner",
    sortOptions = false,
    mixinStandardHelpOptions = true,
    versionProvider = VersionProvider.class,
    header = "Usage:",
    synopsisHeading = "%n",
    descriptionHeading = "%nDescription:%n%n",
    optionListHeading = "%nOptions:%n",
    footerHeading = "%n",
    subcommands = {HelpCommand.class},
    footer = "EthSigner is licensed under the Apache License 2.0")
public class EthSignerBaseCommand implements Config {

  @SuppressWarnings("FieldMayBeFinal")
  @Option(
      names = {"--chain-id"},
      description = "The Chain Id that will be the intended recipient for signed transactions",
      required = true,
      paramLabel = MANDATORY_LONG_FORMAT_HELP,
      arity = "1")
  private long chainId;

  @Option(
      names = {"--data-path"},
      description = "The path to a directory to store temporary files",
      paramLabel = MANDATORY_PATH_FORMAT_HELP,
      arity = "1")
  private Path dataPath;

  @Option(
      names = {"--logging", "-l"},
      paramLabel = "<LOG VERBOSITY LEVEL>",
      description =
          "Logging verbosity levels: OFF, FATAL, WARN, INFO, DEBUG, TRACE, ALL (default: INFO)")
  private final Level logLevel = Level.INFO;

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = {"--http-listen-host"},
      description = "Host for JSON-RPC HTTP to listen on (default: ${DEFAULT-VALUE})",
      paramLabel = MANDATORY_HOST_FORMAT_HELP,
      arity = "1")
  private String httpListenHost = InetAddress.getLoopbackAddress().getHostAddress();

  @Option(
      names = {"--http-listen-port"},
      description = "Port for JSON-RPC HTTP to listen on (default: ${DEFAULT-VALUE})",
      paramLabel = MANDATORY_PORT_FORMAT_HELP,
      arity = "1")
  private final Integer httpListenPort = 8545;

  @ArgGroup(exclusive = false)
  private PicoCliTlsServerOptions picoCliTlsServerOptions;

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = "--downstream-http-host",
      description =
          "The endpoint to which received requests are forwarded (default: ${DEFAULT-VALUE})",
      paramLabel = MANDATORY_HOST_FORMAT_HELP,
      arity = "1")
  private String downstreamHttpHost = InetAddress.getLoopbackAddress().getHostAddress();

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @Option(
      names = "--downstream-http-port",
      description = "The endpoint to which received requests are forwarded",
      paramLabel = MANDATORY_PORT_FORMAT_HELP,
      required = true,
      arity = "1")
  private Integer downstreamHttpPort;

  @SuppressWarnings("FieldMayBeFinal")
  @Option(
      names = {"--downstream-http-request-timeout"},
      description =
          "Timeout in milliseconds to wait for downstream request (default: ${DEFAULT-VALUE})",
      paramLabel = MANDATORY_LONG_FORMAT_HELP,
      arity = "1")
  private long downstreamHttpRequestTimeout = Duration.ofSeconds(5).toMillis();

  @ArgGroup(exclusive = false)
  private PicoCliClientTlsOptions clientTlsOptions;

  @Override
  public Level getLogLevel() {
    return logLevel;
  }

  @Override
  public String getDownstreamHttpHost() {
    return downstreamHttpHost;
  }

  @Override
  public Integer getDownstreamHttpPort() {
    return downstreamHttpPort;
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
  public ChainIdProvider getChainId() {
    return new ConfigurationChainId(chainId);
  }

  @Override
  public Path getDataPath() {
    return dataPath;
  }

  @Override
  public Duration getDownstreamHttpRequestTimeout() {
    return Duration.ofMillis(downstreamHttpRequestTimeout);
  }

  @Override
  public Optional<TlsOptions> getTlsOptions() {
    return Optional.ofNullable(picoCliTlsServerOptions);
  }

  @Override
  public Optional<ClientTlsOptions> getClientTlsOptions() {
    return Optional.ofNullable(clientTlsOptions);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("logLevel", logLevel)
        .add("downstreamHttpHost", downstreamHttpHost)
        .add("downstreamHttpPort", downstreamHttpPort)
        .add("downstreamHttpRequestTimeout", downstreamHttpRequestTimeout)
        .add("httpListenHost", httpListenHost)
        .add("httpListenPort", httpListenPort)
        .add("chainId", chainId)
        .add("dataPath", dataPath)
        .add("clientTlsOptions", clientTlsOptions)
        .toString();
  }

  void validateArgs() {
    if (getClientTlsOptions().isPresent()) {
      final boolean caAuth = getClientTlsOptions().get().isCaAuthEnabled();
      final Optional<Path> optionsKnownServerFile =
          getClientTlsOptions().get().getKnownServersFile();
      if (optionsKnownServerFile.isEmpty() && !caAuth) {
        throw new InvalidCommandLineOptionsException(
            "Missing required argument(s): --downstream-http-tls-known-servers-file must be specified if --downstream-http-tls-ca-auth-enabled=false");
      }
    }
  }
}
