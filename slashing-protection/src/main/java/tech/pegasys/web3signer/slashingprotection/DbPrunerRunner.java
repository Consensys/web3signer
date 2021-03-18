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

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DbPrunerRunner implements Closeable {
  private static final Logger LOG = LogManager.getLogger();
  private final SlashingProtectionParameters slashingProtectionParameters;
  private final SlashingProtection slashingProtection;
  private final ScheduledExecutorService executorService;

  public DbPrunerRunner(
      final SlashingProtectionParameters slashingProtectionParameters,
      final SlashingProtection slashingProtection) {
    this.slashingProtectionParameters = slashingProtectionParameters;
    this.slashingProtection = slashingProtection;
    this.executorService = Executors.newScheduledThreadPool(1);
  }

  public void start() {
    executorService.scheduleAtFixedRate(
        this::runPruning,
        slashingProtectionParameters.getPruningInterval(),
        slashingProtectionParameters.getPruningInterval(),
        TimeUnit.HOURS);
  }

  public void stop() {
    executorService.shutdown();
  }

  public void runOnce() {
    executorService.schedule(this::runPruning, 0, TimeUnit.SECONDS);
  }

  @Override
  public void close() throws IOException {
    stop();
  }

  private void runPruning() {
    try {
      slashingProtection.prune();
    } catch (Exception e) {
      // We only log the error as retrying on the scheduled prune might fix the error
      LOG.info("Pruning slashing protection database failed with error", e);
    }
  }
}
