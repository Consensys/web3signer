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
package tech.pegasys.web3signer.slashingprotection;

import java.util.concurrent.ScheduledExecutorService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DbPrunerRunner {
  private static final Logger LOG = LogManager.getLogger();
  private final SlashingProtectionParameters slashingProtectionParameters;
  private final SlashingProtectionPruner slashingProtection;
  private final ScheduledExecutorService executorService;

  public DbPrunerRunner(
      final SlashingProtectionParameters slashingProtectionParameters,
      final SlashingProtectionPruner slashingProtection,
      final ScheduledExecutorService executorService) {
    this.slashingProtectionParameters = slashingProtectionParameters;
    this.slashingProtection = slashingProtection;
    this.executorService = executorService;
  }

  public void schedule() {
    executorService.scheduleAtFixedRate(
        this::runPruning,
        slashingProtectionParameters.getPruningInterval(),
        slashingProtectionParameters.getPruningInterval(),
        slashingProtectionParameters.getPruningIntervalTimeUnit());
  }

  public void execute() {
    executorService.execute(this::runPruning);
  }

  private void runPruning() {
    try {
      slashingProtection.prune();
    } catch (final Exception e) {
      // We only log the error as retrying on the scheduled prune might fix the error
      LOG.info("Pruning slashing protection database failed with error", e);
    }
  }
}
