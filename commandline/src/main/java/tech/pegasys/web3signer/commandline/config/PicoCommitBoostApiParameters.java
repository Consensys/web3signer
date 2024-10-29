/*
 * Copyright 2022 ConsenSys AG.
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
package tech.pegasys.web3signer.commandline.config;

import static tech.pegasys.web3signer.commandline.DefaultCommandValues.PATH_FORMAT_HELP;

import tech.pegasys.teku.networks.Eth2NetworkConfiguration;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.BeaconState;
import tech.pegasys.teku.spec.datastructures.util.ChainDataLoader;
import tech.pegasys.web3signer.signing.config.CommitBoostParameters;

import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.apache.tuweni.bytes.Bytes32;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

public class PicoCommitBoostApiParameters implements CommitBoostParameters {
  @Spec private CommandSpec commandSpec; // injected by picocli

  @CommandLine.Option(
      names = {"--commit-boost-api-enabled"},
      paramLabel = "<BOOL>",
      description = "Enable the commit boost API (default: ${DEFAULT-VALUE}).",
      arity = "1")
  private boolean isCommitBoostApiEnabled = false;

  @Option(
      names = {"--proxy-keystores-path"},
      description =
          "The path to a writeable directory to store v3 and v4 proxy keystores for commit boost API.",
      paramLabel = PATH_FORMAT_HELP)
  private Path proxyKeystoresPath;

  @Option(
      names = {"--proxy-keystores-password-file"},
      description =
          "The path to the password file used to encrypt/decrypt proxy keystores for commit boost API.",
      paramLabel = PATH_FORMAT_HELP)
  private Path proxyKeystoresPasswordFile;

  // note: In the future, this can be moved to PicoCliNetworkOverrides if we want to determine gvr
  // for slashing protection etc. in advance before getting the first signing call from CL.
  @Option(
      names = {"--genesis-state"},
      paramLabel = "<STRING>",
      description =
          "The genesis state. This value should be a file or URL pointing to an SSZ-encoded finalized checkpoint "
              + "state. It is used to determine the value of genesis validators root for Commit Boost API.",
      arity = "1")
  private String genesisState;

  private Bytes32 genesisValidatorsRoot;

  @Override
  public boolean isEnabled() {
    return isCommitBoostApiEnabled;
  }

  @Override
  public Path getProxyKeystoresPath() {
    return proxyKeystoresPath;
  }

  @Override
  public Path getProxyKeystoresPasswordFile() {
    return proxyKeystoresPasswordFile;
  }

  @Override
  public Bytes32 getGenesisValidatorsRoot() {
    if (genesisValidatorsRoot == null) {
      throw new IllegalStateException("Genesis validators root not set");
    }
    return genesisValidatorsRoot;
  }

  /**
   * Apply genesis state overrides to the network configuration only if commit boost API is enabled.
   *
   * @param builder The network configuration builder to apply overrides to.
   */
  public void applyOverrides(final Eth2NetworkConfiguration.Builder builder) {
    if (isCommitBoostApiEnabled && StringUtils.isNotBlank(genesisState)) {
      builder.customGenesisState(genesisState);
    }
  }

  /**
   * Validate the parameters for the commit boost API and initialize parameters which will be used
   * during run operation.
   */
  public void validateParameters(final Eth2NetworkConfiguration eth2NetworkConfig)
      throws ParameterException {
    if (!isCommitBoostApiEnabled) {
      return;
    }

    if (proxyKeystoresPath == null) {
      throw new ParameterException(
          commandSpec.commandLine(),
          "Commit boost API is enabled, but --proxy-keystores-path not set");
    }

    if (proxyKeystoresPasswordFile == null) {
      throw new ParameterException(
          commandSpec.commandLine(),
          "Commit boost API is enabled, but --proxy-keystores-password-file not set");
    }

    loadGenesisValidatorsRoot(eth2NetworkConfig);
  }

  /** Load genesis state and obtain genesis validators root. */
  private void loadGenesisValidatorsRoot(final Eth2NetworkConfiguration eth2NetworkConfig) {
    final String parameterExceptionMessage =
        "Unable to load genesis state to determine genesis validators root. Please provide custom genesis state using --genesis-state";
    final String genesisState =
        eth2NetworkConfig
            .getNetworkBoostrapConfig()
            .getGenesisState()
            .orElseThrow(
                () -> new ParameterException(commandSpec.commandLine(), parameterExceptionMessage));
    try {
      final BeaconState beaconState =
          ChainDataLoader.loadState(eth2NetworkConfig.getSpec(), genesisState);
      genesisValidatorsRoot = beaconState.getGenesisValidatorsRoot();
    } catch (final Exception e) {
      throw new ParameterException(commandSpec.commandLine(), parameterExceptionMessage);
    }
  }
}
