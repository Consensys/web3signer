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

import static tech.pegasys.web3signer.commandline.DefaultCommandValues.MANDATORY_HOST_FORMAT_HELP;
import static tech.pegasys.web3signer.commandline.DefaultCommandValues.MANDATORY_LONG_FORMAT_HELP;
import static tech.pegasys.web3signer.commandline.DefaultCommandValues.MANDATORY_PATH_FORMAT_HELP;
import static tech.pegasys.web3signer.commandline.DefaultCommandValues.MANDATORY_PORT_FORMAT_HELP;
import static tech.pegasys.web3signer.commandline.util.RequiredOptionsUtil.checkIfRequiredOptionsAreInitialized;

import tech.pegasys.web3signer.commandline.annotations.RequiredOption;
import tech.pegasys.web3signer.commandline.config.client.PicoCliClientTlsOptions;
import tech.pegasys.web3signer.core.Eth1Runner;
import tech.pegasys.web3signer.core.Runner;
import tech.pegasys.web3signer.core.config.Eth1Config;
import tech.pegasys.web3signer.core.config.client.ClientTlsOptions;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Optional;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

@Command(
    name = Eth1SubCommand.COMMAND_NAME,
    description = "Handle Ethereum-1 SECP256k1 signing operations and public key reporting",
    subcommands = {HelpCommand.class},
    mixinStandardHelpOptions = true)
public class Eth1SubCommand extends ModeSubCommand implements Eth1Config {

  public static final String COMMAND_NAME = "eth1";

  @CommandLine.Spec private CommandLine.Model.CommandSpec spec; // injected by picocli

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @CommandLine.Option(
      names = "--downstream-http-host",
      description = "The endpoint to which received requests are forwarded (default: 127.0.0.1)",
      paramLabel = MANDATORY_HOST_FORMAT_HELP,
      arity = "1")
  private String downstreamHttpHost = "127.0.0.1";

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @RequiredOption
  @CommandLine.Option(
      names = "--downstream-http-port",
      description = "The endpoint to which received requests are forwarded",
      paramLabel = MANDATORY_PORT_FORMAT_HELP,
      arity = "1")
  private Integer downstreamHttpPort;

  private String downstreamHttpPath = "/";

  @CommandLine.Option(
      names = {"--downstream-http-path"},
      description = "The path to which received requests are forwarded (default: /)",
      defaultValue = "/",
      paramLabel = MANDATORY_PATH_FORMAT_HELP,
      arity = "1")
  public void setDownstreamHttpPath(final String path) {
    try {
      final URI uri = new URI(path);
      if (!uri.getPath().equals(path)) {
        throw new CommandLine.ParameterException(
            spec.commandLine(), "Illegal characters detected in --downstream-http-path");
      }
    } catch (final URISyntaxException e) {
      throw new CommandLine.ParameterException(
          spec.commandLine(), "Illegal characters detected in --downstream-http-path");
    }
    this.downstreamHttpPath = path;
  }

  @SuppressWarnings("FieldMayBeFinal")
  @CommandLine.Option(
      names = {"--downstream-http-request-timeout"},
      description = "Timeout in milliseconds to wait for downstream request (default: 5000)",
      paramLabel = MANDATORY_LONG_FORMAT_HELP,
      arity = "1")
  private long downstreamHttpRequestTimeout = Duration.ofSeconds(5).toMillis();

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @CommandLine.Option(
      names = {"--downstream-http-proxy-host"},
      description = "Hostname for proxy connect, no proxy if null (default: null)",
      paramLabel = MANDATORY_HOST_FORMAT_HELP,
      arity = "1")
  private String httpProxyHost = null;

  @CommandLine.Option(
      names = {"--downstream-http-proxy-port"},
      paramLabel = MANDATORY_PORT_FORMAT_HELP,
      description = "Port for proxy connect (default: 80)",
      arity = "1")
  private final Integer httpProxyPort = 80;

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @CommandLine.Option(
      names = {"--downstream-http-proxy-username"},
      paramLabel = "<username>",
      description = "Username for proxy connect, no authentication if null (default: null)",
      arity = "1")
  private String httpProxyUsername = null;

  @SuppressWarnings("FieldMayBeFinal") // Because PicoCLI requires Strings to not be final.
  @CommandLine.Option(
      names = {"--downstream-http-proxy-password"},
      paramLabel = "<password>",
      description = "Password for proxy connect, no authentication if null (default: null)",
      arity = "1")
  private String httpProxyPassword = null;

  @CommandLine.Mixin private PicoCliClientTlsOptions clientTlsOptions;

  @Override
  public Runner createRunner() {
    return new Eth1Runner(config, this);
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }

  @Override
  protected void validateArgs() {
    checkIfRequiredOptionsAreInitialized(this);
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
  public String getDownstreamHttpPath() {
    return downstreamHttpPath;
  }

  @Override
  public Duration getDownstreamHttpRequestTimeout() {
    return Duration.ofMillis(downstreamHttpRequestTimeout);
  }

  @Override
  public String getHttpProxyHost() {
    return httpProxyHost;
  }

  @Override
  public Integer getHttpProxyPort() {
    return httpProxyPort;
  }

  @Override
  public String getHttpProxyUsername() {
    return httpProxyUsername;
  }

  @Override
  public String getHttpProxyPassword() {
    return httpProxyPassword;
  }

  @Override
  public Optional<ClientTlsOptions> getClientTlsOptions() {
    return clientTlsOptions.isTlsEnabled() ? Optional.of(clientTlsOptions) : Optional.empty();
  }
}
