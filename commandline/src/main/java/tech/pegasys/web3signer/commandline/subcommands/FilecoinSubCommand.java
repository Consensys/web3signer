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

import java.util.Collection;
import java.util.List;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import picocli.CommandLine.Option;
import tech.pegasys.web3signer.core.FilecoinRunner;
import tech.pegasys.web3signer.core.Runner;

import picocli.CommandLine.Command;
import tech.pegasys.web3signer.core.config.Config;
import tech.pegasys.web3signer.core.multikey.DefaultArtifactSignerProvider;
import tech.pegasys.web3signer.core.multikey.SignerLoader;
import tech.pegasys.web3signer.core.multikey.metadata.AbstractArtifactSignerFactory;
import tech.pegasys.web3signer.core.multikey.metadata.BlsArtifactSignerFactory;
import tech.pegasys.web3signer.core.multikey.metadata.Secp256k1ArtifactSignerFactory;
import tech.pegasys.web3signer.core.multikey.metadata.parser.YamlSignerParser;
import tech.pegasys.web3signer.core.signing.ArtifactSigner;
import tech.pegasys.web3signer.core.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.core.signing.FcBlsArtifactSigner;
import tech.pegasys.web3signer.core.signing.FcSecpArtifactSigner;
import tech.pegasys.web3signer.core.signing.filecoin.FilecoinNetwork;

@Command(
    name = FilecoinSubCommand.COMMAND_NAME,
    description = "Handle Filecoin signing operations and address reporting",
    mixinStandardHelpOptions = true)
public class FilecoinSubCommand extends ModeSubCommand {

  public static final String COMMAND_NAME = "filecoin";

  @Option(
      names = {"--filecoin-network"},
      description = "Filecoin network to use for addresses (default: ${DEFAULT-VALUE})",
      paramLabel = "<network name>",
      arity = "1")
  private final FilecoinNetwork filecoinNetwork = FilecoinNetwork.TESTNET;

  @Override
  public Runner createRunner() {
    return new FilecoinRunner(config, filecoinNetwork);
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }
}
