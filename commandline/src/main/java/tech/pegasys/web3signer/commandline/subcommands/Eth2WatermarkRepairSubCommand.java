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
import tech.pegasys.web3signer.slashingprotection.dao.HighWatermark;
import tech.pegasys.web3signer.slashingprotection.dao.LowWatermarkDao;
import tech.pegasys.web3signer.slashingprotection.dao.MetadataDao;
import tech.pegasys.web3signer.slashingprotection.dao.Validator;
import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;

@Command(
    name = "watermark-repair",
    description =
        "Updates the slashing protection low watermark or high watermark for all validators. "
            + "This will not move the low watermark lower, the low watermark can only be increased."
            + "If setting the high watermark, care should be taken to set this to a future epoch and slot.",
    subcommands = {HelpCommand.class},
    mixinStandardHelpOptions = true)
public class Eth2WatermarkRepairSubCommand implements Runnable {
  private static final Logger LOG = LogManager.getLogger();

  @CommandLine.ParentCommand private Eth2SubCommand eth2Config;

  @Option(
      names = {"--epoch"},
      paramLabel = "<epoch>",
      description =
          "Low watermark to set the attestation source and target to. (Sets high watermark epoch when --set-high-watermark=true).",
      arity = "1")
  Long epoch;

  @Option(
      names = "--slot",
      paramLabel = "<slot>",
      description =
          "Low watermark to set the block slot to. (Sets high watermark slot when --set-high-watermark=true).",
      arity = "1")
  Long slot;

  @Option(
      names = "--set-high-watermark",
      paramLabel = "<BOOL>",
      description =
          "Sets high watermark to given epoch and slot. (Sets low watermark when --set-high-watermark=false)."
              + " (Default: ${DEFAULT-VALUE})")
  boolean setHighWatermark = false;

  @Option(
      names = "--remove-high-watermark",
      paramLabel = "<BOOL>",
      description =
          "Removes high watermark. When set to true, all other subcommand options are ignored."
              + " (Default: ${DEFAULT-VALUE})")
  boolean removeHighWatermark = false;

  @Override
  public void run() {
    final LowWatermarkDao lowWatermarkDao = new LowWatermarkDao();
    final MetadataDao metadataDao = new MetadataDao();
    final ValidatorsDao validatorsDao = new ValidatorsDao();

    final SlashingProtectionContext slashingProtectionContext =
        SlashingProtectionContextFactory.create(eth2Config.getSlashingProtectionParameters());
    final Jdbi jdbi = slashingProtectionContext.getSlashingProtectionJdbi();

    if (removeHighWatermark) {
      jdbi.useTransaction(metadataDao::deleteHighWatermark);
      LOG.info("Removed high watermark for all validators");
    } else if (setHighWatermark) {
      jdbi.useTransaction(h -> setHighWatermark(h, metadataDao));
      LOG.info("Updated high watermark for all validators");
    } else {
      final List<Validator> allValidators =
          jdbi.inTransaction(h -> validatorsDao.findAllValidators(h).collect(Collectors.toList()));

      LOG.info("Updating low watermark for all {} validators...", allValidators.size());

      allValidators.stream()
          .parallel()
          .forEach(
              validator ->
                  jdbi.useTransaction(h -> setLowWatermark(h, validator, lowWatermarkDao)));

      LOG.info("Updated low watermark for all {} validators", allValidators.size());
    }
  }

  private void setHighWatermark(Handle h, MetadataDao metadataDao) {
    metadataDao.updateHighWatermark(
        h, new HighWatermark(UInt64.valueOf(slot), UInt64.valueOf(epoch)));
  }

  private void setLowWatermark(Handle h, Validator validator, LowWatermarkDao lowWatermarkDao) {
    lowWatermarkDao.updateSlotWatermarkFor(h, validator.getId(), UInt64.valueOf(slot));
    lowWatermarkDao.updateEpochWatermarksFor(
        h, validator.getId(), UInt64.valueOf(epoch), UInt64.valueOf(epoch));
  }
}
