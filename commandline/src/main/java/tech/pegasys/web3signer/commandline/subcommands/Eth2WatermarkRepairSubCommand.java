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

import tech.pegasys.web3signer.slashingprotection.SlashingProtectionContext;
import tech.pegasys.web3signer.slashingprotection.SlashingProtectionContextFactory;
import tech.pegasys.web3signer.slashingprotection.dao.LowWatermarkDao;
import tech.pegasys.web3signer.slashingprotection.dao.Validator;
import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.core.Jdbi;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

@Command(
    name = "watermark-repair",
    description =
        "Updates the slashing protection low watermark for validators. "
            + "This will not move the low watermark lower, the watermark can only be increased.",
    subcommands = {HelpCommand.class},
    mixinStandardHelpOptions = true)
public class Eth2WatermarkRepairSubCommand implements Runnable {
  private static final Logger LOG = LogManager.getLogger();

  @Spec private CommandSpec commandSpec;

  @CommandLine.ParentCommand private Eth2SubCommand eth2Config;

  @Option(
      names = {"--epoch"},
      paramLabel = "<epoch>",
      description = "Low watermark to set the attestation source and target to.",
      arity = "1")
  Long epoch;

  @Option(
      names = "--slot",
      paramLabel = "<epoch>",
      description = "Low watermark to set the block slot to.",
      arity = "1")
  Long slot;

  @Option(
      names = "--validator-ids",
      paramLabel = "<publicKey>",
      description =
          "List of validator public keys as hexadecimal to apply low watermark update to. If none are specified"
              + " then all validators low watermarks will be updated (default: ${DEFAULT-VALUE}).",
      split = ",",
      arity = "0..*")
  List<String> validatorIds = new ArrayList<>();

  @Override
  public void run() {
    final LowWatermarkDao lowWatermarkDao = new LowWatermarkDao();
    final ValidatorsDao validatorsDao = new ValidatorsDao();
    final List<Bytes> validatorIdPublicKeys =
        validatorIds.stream()
            .filter(StringUtils::isNotEmpty)
            .map(this::convertToBytes)
            .collect(Collectors.toList());

    final SlashingProtectionContext slashingProtectionContext =
        SlashingProtectionContextFactory.create(eth2Config.getSlashingProtectionParameters());
    final Jdbi jdbi = slashingProtectionContext.getSlashingProtectionJdbi();

    final List<Validator> allValidators =
        jdbi.inTransaction(h -> validatorsDao.findAllValidators(h).collect(Collectors.toList()));

    final List<Validator> validators =
        validatorIdPublicKeys.isEmpty()
            ? allValidators
            : allValidators.stream()
                .filter(v -> validatorIdPublicKeys.contains(v.getPublicKey()))
                .collect(Collectors.toList());

    validators.stream()
        .parallel()
        .forEach(
            validator ->
                jdbi.useTransaction(
                    h -> {
                      lowWatermarkDao.updateSlotWatermarkFor(
                          h, validator.getId(), UInt64.valueOf(slot));
                      lowWatermarkDao.updateEpochWatermarksFor(
                          h, validator.getId(), UInt64.valueOf(epoch), UInt64.valueOf(epoch));
                    }));
    LOG.info("Updated low watermark for {} validators", validators.size());
  }

  private Bytes convertToBytes(final String value) {
    try {
      return Bytes.fromHexString(value);
    } catch (final IllegalArgumentException e) {
      throw new ParameterException(
          commandSpec.commandLine(),
          "Invalid value for validator public key " + value + ". Value must be in hexadecimal.");
    }
  }
}
