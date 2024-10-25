/*
 * Copyright 2024 ConsenSys AG.
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

import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.networks.Eth2NetworkConfiguration;
import tech.pegasys.web3signer.commandline.subcommands.Eth2SubCommand;

import picocli.CommandLine;

/** Mixin class to hold network overrides for the PicoCLI parser. */
public class PicoCliNetworkOverrides {
  @CommandLine.Option(
      names = {"--Xnetwork-altair-fork-epoch"},
      hidden = true,
      paramLabel = "<epoch>",
      description = "Override the Altair fork activation epoch.",
      arity = "1",
      converter = Eth2SubCommand.UInt64Converter.class)
  private UInt64 altairForkEpoch;

  @CommandLine.Option(
      names = {"--Xnetwork-bellatrix-fork-epoch"},
      hidden = true,
      paramLabel = "<epoch>",
      description = "Override the Bellatrix fork activation epoch.",
      arity = "1",
      converter = Eth2SubCommand.UInt64Converter.class)
  private UInt64 bellatrixForkEpoch;

  @CommandLine.Option(
      names = {"--Xnetwork-capella-fork-epoch"},
      hidden = true,
      paramLabel = "<epoch>",
      description = "Override the Capella fork activation epoch.",
      arity = "1",
      converter = Eth2SubCommand.UInt64Converter.class)
  private UInt64 capellaForkEpoch;

  @CommandLine.Option(
      names = {"--Xnetwork-deneb-fork-epoch"},
      hidden = true,
      paramLabel = "<epoch>",
      description = "Override the Deneb fork activation epoch.",
      arity = "1",
      converter = Eth2SubCommand.UInt64Converter.class)
  private UInt64 denebForkEpoch;

  @CommandLine.Option(
      names = {"--Xnetwork-electra-fork-epoch"},
      hidden = true,
      paramLabel = "<epoch>",
      description = "Override the Electra fork activation epoch.",
      arity = "1",
      converter = Eth2SubCommand.UInt64Converter.class)
  private UInt64 electraForkEpoch;

  @CommandLine.Option(
      names = {"--Xtrusted-setup"},
      hidden = true,
      paramLabel = "<STRING>",
      description =
          "The trusted setup which is needed for KZG commitments. Only required when creating a custom network. This value should be a file or URL pointing to a trusted setup.",
      arity = "1")
  private String trustedSetup = null; // Depends on network configuration

  public Eth2NetworkConfiguration.Builder applyOverrides(
      final Eth2NetworkConfiguration.Builder builder) {
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
    return builder;
  }
}
