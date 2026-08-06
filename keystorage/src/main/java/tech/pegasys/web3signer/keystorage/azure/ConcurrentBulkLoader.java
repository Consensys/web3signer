/*
 * Copyright 2026 ConsenSys AG.
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
package tech.pegasys.web3signer.keystorage.azure;

import tech.pegasys.web3signer.keystorage.common.MappedResults;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Loads a stream of items from a remote vault concurrently.
 *
 * <p>Items are consumed lazily, so a vault which pages its listing continues listing while earlier
 * items are being fetched. Concurrency is bounded by {@link BulkLoadOptions#maxConcurrency()}.
 * Retries for transient failures, such as throttling, are left to the vault SDK's own retry policy;
 * a failure reaching this loader is treated as final for that item.
 *
 * <p>Every item taken from the stream is accounted for: it contributes either a value or an error.
 * Interrupts and an expired deadline both abandon the load with a non-zero error count rather than
 * reporting a partial load as a complete one.
 */
public class ConcurrentBulkLoader {

  private static final Logger LOG = LogManager.getLogger();

  private static final Duration PROGRESS_INTERVAL = Duration.ofSeconds(5);
  private static final int MAX_LOGGED_FAILURES = 5;

  private static final String LABEL_DEADLINE = "abandoned: deadline exceeded";
  private static final String LABEL_INTERRUPTED = "abandoned: interrupted";
  private static final String LABEL_INCOMPLETE = "abandoned: did not complete";
  private static final String LABEL_LISTING = "listing failed";

  private final BulkLoadOptions options;

  /**
   * Creates a loader for a single vault.
   *
   * @param options concurrency and deadline parameters for the load.
   */
  public ConcurrentBulkLoader(final BulkLoadOptions options) {
    this.options = options;
  }

  /**
   * Applies {@code work} to every item of {@code items} concurrently.
   *
   * @param description used to identify this load in log messages.
   * @param items the items to load. Consumed lazily and closed on completion.
   * @param nameOf names an item for logging.
   * @param work loads a single item. May return several values, and its own error count, for one
   *     item.
   * @return the loaded values, and the number of errors encountered.
   * @param <T> the type of item being loaded.
   * @param <R> the type of value produced for an item.
   */
  public <T, R> MappedResults<R> load(
      final String description,
      final Stream<T> items,
      final Function<T, String> nameOf,
      final Function<T, MappedResults<R>> work) {
    return new Run<T, R>(description, nameOf, work).execute(items);
  }

  private final class Run<T, R> {
    private final String description;
    private final Function<T, String> nameOf;
    private final Function<T, MappedResults<R>> work;

    private final Set<R> values = ConcurrentHashMap.newKeySet();
    private final AtomicInteger listed = new AtomicInteger();
    private final AtomicInteger succeeded = new AtomicInteger();
    private final AtomicInteger failures = new AtomicInteger();
    private final AtomicInteger mappingErrors = new AtomicInteger();
    private final Map<String, AtomicInteger> failuresByLabel = new ConcurrentHashMap<>();
    private final Queue<String> loggedFailures = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean incompleteListing = new AtomicBoolean();
    // fair, so a producer waiting to dispatch is not starved by workers releasing and re-acquiring
    private final Semaphore limiter = new Semaphore(options.maxConcurrency(), true);

    private final long startNanos = System.nanoTime();
    private final long deadlineNanos = startNanos + options.deadline().toNanos();

    private Run(
        final String description,
        final Function<T, String> nameOf,
        final Function<T, MappedResults<R>> work) {
      this.description = description;
      this.nameOf = nameOf;
      this.work = work;
    }

    private MappedResults<R> execute(final Stream<T> items) {
      final Thread progressReporter = startProgressReporter();
      try (final Stream<T> stream = items;
          final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
        try {
          final Iterator<T> iterator = stream.iterator();
          while (iterator.hasNext()) {
            final T item = iterator.next();
            listed.incrementAndGet();
            if (!dispatch(executor, item)) {
              incompleteListing.set(true);
              break;
            }
          }
        } catch (final Exception e) {
          // a listing failure loses an unknown number of items, so the load cannot be trusted
          incompleteListing.set(true);
          recordFailure(LABEL_LISTING, "<listing>", e);
          LOG.error(
              "{}: failed to list items, {} listed before the failure",
              description,
              listed.get(),
              e);
        }
        LOG.debug("{}: listed {} items, awaiting in-flight work", description, listed.get());
      } finally {
        progressReporter.interrupt();
      }

      reconcile();
      final int errorCount = failures.get() + mappingErrors.get();
      logSummary(errorCount);
      return MappedResults.newInstance(values, errorCount);
    }

    /**
     * Acquires a permit and submits the item, blocking while the configured concurrency is
     * saturated. Blocking here is what applies backpressure to the listing.
     *
     * @return false when the remaining items should not be dispatched.
     */
    private boolean dispatch(final ExecutorService executor, final T item) {
      if (deadlineExpired()) {
        recordFailure(LABEL_DEADLINE, nameOf.apply(item), null);
        LOG.error(
            "{}: deadline of {} exceeded after {} items",
            description,
            options.deadline(),
            listed.get());
        return false;
      }
      try {
        limiter.acquire();
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        recordFailure(LABEL_INTERRUPTED, nameOf.apply(item), e);
        return false;
      }
      try {
        executor.submit(() -> process(item));
        return true;
      } catch (final RuntimeException e) {
        limiter.release();
        recordFailure(e.getClass().getSimpleName(), nameOf.apply(item), e);
        return false;
      }
    }

    private void process(final T item) {
      try {
        attempt(item);
      } finally {
        limiter.release();
      }
    }

    private void attempt(final T item) {
      final String name = nameOf.apply(item);
      try {
        final MappedResults<R> mapped = work.apply(item);
        values.addAll(mapped.getValues());
        mappingErrors.addAndGet(mapped.getErrorCount());
        succeeded.incrementAndGet();
      } catch (final Exception e) {
        recordFailure(e.getClass().getSimpleName(), name, e);
      }
    }

    private boolean deadlineExpired() {
      return System.nanoTime() - deadlineNanos >= 0;
    }

    private void recordFailure(final String label, final String name, final Exception cause) {
      failures.incrementAndGet();
      failuresByLabel.computeIfAbsent(label, _ -> new AtomicInteger()).incrementAndGet();
      if (loggedFailures.size() < MAX_LOGGED_FAILURES) {
        loggedFailures.add(name + ": " + label);
        LOG.warn("{}: failed to load '{}' - {}", description, name, label, cause);
      } else {
        LOG.debug("{}: failed to load '{}' - {}", description, name, label, cause);
      }
    }

    /** Ensures every listed item is accounted for, even if its task never ran to completion. */
    private void reconcile() {
      final int unaccounted = listed.get() - (succeeded.get() + failures.get());
      if (unaccounted > 0) {
        failures.addAndGet(unaccounted);
        failuresByLabel
            .computeIfAbsent(LABEL_INCOMPLETE, _ -> new AtomicInteger())
            .addAndGet(unaccounted);
      }
    }

    private void logSummary(final int errorCount) {
      final Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);
      LOG.info(
          "{}: loaded {} values from {} of the {} items {} in {}s",
          description,
          values.size(),
          succeeded.get(),
          listed.get(),
          incompleteListing.get() ? "listed before the load was abandoned" : "listed",
          elapsed.toSeconds());
      if (errorCount > 0) {
        LOG.error(
            "{}: {} items failed and {} values could not be mapped. Failures by cause: {}. First failures: {}",
            description,
            failures.get(),
            mappingErrors.get(),
            failuresByLabel,
            loggedFailures);
      }
      if (incompleteListing.get()) {
        LOG.error(
            "{}: the load was abandoned before every item had been listed, so an unknown number of"
                + " items were never attempted",
            description);
      }
    }

    private Thread startProgressReporter() {
      return Thread.ofVirtual()
          .name("bulk-load-progress")
          .start(
              () -> {
                try {
                  while (true) {
                    Thread.sleep(PROGRESS_INTERVAL);
                    LOG.info(
                        "{}: listed {}, loaded {}, failed {}",
                        description,
                        listed.get(),
                        succeeded.get(),
                        failures.get());
                  }
                } catch (final InterruptedException e) {
                  Thread.currentThread().interrupt();
                }
              });
    }
  }
}
