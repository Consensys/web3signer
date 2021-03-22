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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dsl.TestSlashingProtectionParameters;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt64;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PruningRunnerIntegrationTest extends IntegrationTestBase {

  public static final String PRUNING_THREAD_NAME = "slashing-db-pruner-0";
  private ScheduledExecutorService scheduledExecutorService;
  private TestSlashingProtectionParameters slashingProtectionParameters;
  private SlashingProtection pruningSlashingProtection;

  @BeforeEach
  void setupSlashingProtection() {
    slashingProtectionParameters =
        new TestSlashingProtectionParameters(databaseUrl, USERNAME, PASSWORD, 5, 1);
    scheduledExecutorService =
        new ScheduledThreadPoolExecutor(
            1, new ThreadFactoryBuilder().setNameFormat("slashing-db-pruner" + "-%d").build());
    pruningSlashingProtection =
        SlashingProtectionFactory.createSlashingProtection(slashingProtectionParameters);
    insertValidatorAndCreateSlashingData(pruningSlashingProtection, 10, 10, 1);
  }

  @AfterEach
  void tearDownSlashingProtection() {
    scheduledExecutorService.shutdownNow();
  }

  @Test
  void prunesValidatorsForExecuteOnOwnThread() {
    final TestSlashingProtection testSlashingProtection =
        new TestSlashingProtection(pruningSlashingProtection);
    final DbPrunerRunner dbPrunerRunner =
        new DbPrunerRunner(
            slashingProtectionParameters, testSlashingProtection, scheduledExecutorService);

    dbPrunerRunner.execute();

    final List<PruningStat> pruningStats = testSlashingProtection.getPruningStats();
    waitForPruningToFinish(pruningStats, 1);
    assertThat(pruningStats.get(0).getThread().getName()).isEqualTo(PRUNING_THREAD_NAME);
    assertThat(fetchAttestations(1)).hasSize(5);
    assertThat(fetchBlocks(1)).hasSize(5);
  }

  @Test
  void prunesValidatorsForExecuteHandlesErrors() {
    final TestSlashingProtection testSlashingProtection =
        new TestSlashingProtection(
            pruningSlashingProtection, createPrunerRunnerThatFailsOnFirstRun());
    final DbPrunerRunner dbPrunerRunner =
        new DbPrunerRunner(
            slashingProtectionParameters, testSlashingProtection, scheduledExecutorService);

    // run expecting error to occur and no pruning to happen
    dbPrunerRunner.execute();

    final List<PruningStat> pruningStats = testSlashingProtection.getPruningStats();
    waitForPruningToFinish(pruningStats, 1);
    final Set<Thread> currentThreads = Thread.getAllStackTraces().keySet();
    assertThat(currentThreads.stream().anyMatch(t -> t.getName().equals(PRUNING_THREAD_NAME)))
        .isTrue();
    assertThat(fetchAttestations(1)).hasSize(10);
    assertThat(fetchBlocks(1)).hasSize(10);

    // ensure that pruning still works after the failed execution
    dbPrunerRunner.execute();

    waitForPruningToFinish(pruningStats, 2);
    assertThat(pruningStats.get(1).getThread().getName()).isEqualTo(PRUNING_THREAD_NAME);
    assertThat(fetchAttestations(1)).hasSize(5);
    assertThat(fetchBlocks(1)).hasSize(5);
  }

  private Runnable createPrunerRunnerThatFailsOnFirstRun() {
    final AtomicInteger pruningCount = new AtomicInteger(0);
    return () -> {
      if (pruningCount.addAndGet(1) == 1) {
        throw new IllegalStateException("Pruning failed");
      } else {
        pruningSlashingProtection.prune();
      }
    };
  }

  @Test
  void prunesValidatorsForScheduledRunAreRunPeriodically() {
    final SlashingProtectionParameters slashingProtectionParameters =
        new TestSlashingProtectionParameters(databaseUrl, USERNAME, PASSWORD, 5, 1, 1);
    final TestSlashingProtection testSlashingProtection =
        new TestSlashingProtection(
            pruningSlashingProtection, createPrunerRunnerThatFailsOnFirstRun());
    final DbPrunerRunner dbPrunerRunner =
        new DbPrunerRunner(
            slashingProtectionParameters, testSlashingProtection, scheduledExecutorService);

    dbPrunerRunner.schedule();

    final List<PruningStat> pruningStats = testSlashingProtection.getPruningStats();
    waitForPruningToFinish(pruningStats, 3);
    assertThat(
            pruningStats.stream()
                .allMatch(ps -> ps.getThread().getName().equals(PRUNING_THREAD_NAME)))
        .isTrue();
    final LocalDateTime firstPruningStartTime = pruningStats.get(0).getStartTime();
    assertThat(pruningStats.get(1).getStartTime())
        .isEqualToIgnoringNanos(firstPruningStartTime.plusSeconds(1));
    assertThat(pruningStats.get(2).getStartTime())
        .isEqualToIgnoringNanos(firstPruningStartTime.plusSeconds(2));
    assertThat(fetchAttestations(1)).hasSize(5);
    assertThat(fetchBlocks(1)).hasSize(5);
  }

  @Test
  void prunesValidatorsForScheduledRunHandlesErrors() {
    final SlashingProtectionParameters slashingProtectionParameters =
        new TestSlashingProtectionParameters(databaseUrl, USERNAME, PASSWORD, 5, 1, 1);
    final TestSlashingProtection testSlashingProtection =
        new TestSlashingProtection(pruningSlashingProtection);
    final DbPrunerRunner dbPrunerRunner =
        new DbPrunerRunner(
            slashingProtectionParameters, testSlashingProtection, scheduledExecutorService);

    dbPrunerRunner.schedule();

    final List<PruningStat> pruningStats = testSlashingProtection.getPruningStats();
    waitForPruningToFinish(pruningStats, 2);
    assertThat(
            pruningStats.stream()
                .allMatch(ps -> ps.getThread().getName().equals(PRUNING_THREAD_NAME)))
        .isTrue();
    final LocalDateTime firstPruningStartTime = pruningStats.get(0).getStartTime();
    assertThat(pruningStats.get(1).getStartTime())
        .isEqualToIgnoringNanos(firstPruningStartTime.plusSeconds(1));
    assertThat(fetchAttestations(1)).hasSize(5);
    assertThat(fetchBlocks(1)).hasSize(5);
  }

  private void waitForPruningToFinish(final List<PruningStat> pruningStats, final int pruningSize) {
    Awaitility.await()
        .ignoreExceptions()
        .atMost(30, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(pruningStats).hasSize(pruningSize));
  }

  // Test version of slashing protection that delegates everything but also captures the thread that
  // pruning was run so we can verify this
  private static class TestSlashingProtection implements SlashingProtection {

    private final SlashingProtection delegate;
    private final Runnable pruningRunner;
    private final List<PruningStat> pruningStats = new ArrayList<>();

    public TestSlashingProtection(final SlashingProtection delegate) {
      this.delegate = delegate;
      this.pruningRunner = delegate::prune;
    }

    public TestSlashingProtection(final SlashingProtection delegate, final Runnable pruningRunner) {
      this.delegate = delegate;
      this.pruningRunner = pruningRunner;
    }

    @Override
    public boolean maySignAttestation(
        final Bytes publicKey,
        final Bytes signingRoot,
        final UInt64 sourceEpoch,
        final UInt64 targetEpoch,
        final Bytes32 genesisValidatorsRoot) {
      return delegate.maySignAttestation(
          publicKey, signingRoot, sourceEpoch, targetEpoch, genesisValidatorsRoot);
    }

    @Override
    public boolean maySignBlock(
        final Bytes publicKey,
        final Bytes signingRoot,
        final UInt64 blockSlot,
        final Bytes32 genesisValidatorsRoot) {
      return delegate.maySignBlock(publicKey, signingRoot, blockSlot, genesisValidatorsRoot);
    }

    @Override
    public void registerValidators(final List<Bytes> validators) {
      delegate.registerValidators(validators);
    }

    @Override
    public void export(final OutputStream output) {
      delegate.export(output);
    }

    @Override
    public void importData(final InputStream output) {
      delegate.importData(output);
    }

    @Override
    public void prune() {
      final Thread currentThread = Thread.currentThread();
      final LocalDateTime currentTime = LocalDateTime.now(ZoneId.systemDefault());
      pruningStats.add(new PruningStat(currentThread, currentTime));
      pruningRunner.run();
    }

    public List<PruningStat> getPruningStats() {
      return pruningStats;
    }
  }

  private static class PruningStat {
    private final Thread thread;
    private final LocalDateTime startTime;

    public PruningStat(final Thread thread, final LocalDateTime startTime) {
      this.thread = thread;
      this.startTime = startTime;
    }

    public Thread getThread() {
      return thread;
    }

    public LocalDateTime getStartTime() {
      return startTime;
    }
  }
}
