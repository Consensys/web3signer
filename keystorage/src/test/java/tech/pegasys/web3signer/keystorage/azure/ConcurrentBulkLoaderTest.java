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

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.web3signer.keystorage.common.MappedResults;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class ConcurrentBulkLoaderTest {

  private static final BulkLoadOptions FAST_OPTIONS =
      new BulkLoadOptions(8, Duration.ofSeconds(60));

  @Test
  void loadsEveryItem() {
    final MappedResults<String> results = load(items(50), item -> value(item));

    assertThat(results.getValues()).hasSize(50);
    assertThat(results.getErrorCount()).isZero();
  }

  @Test
  void keepsEveryValueReturnedForASingleItem() {
    final MappedResults<String> results =
        load(items(3), item -> MappedResults.newInstance(Set.of(item + "-a", item + "-b"), 0));

    assertThat(results.getValues()).hasSize(6);
    assertThat(results.getErrorCount()).isZero();
  }

  @Test
  void mappingErrorsReportedByTheWorkAreCounted() {
    final MappedResults<String> results =
        load(items(4), item -> MappedResults.newInstance(Set.of(item), 2));

    assertThat(results.getValues()).hasSize(4);
    assertThat(results.getErrorCount()).isEqualTo(8);
  }

  @Test
  void aFailureIsNotRetried() {
    final AtomicInteger attempts = new AtomicInteger();

    final MappedResults<String> results =
        load(
            items(1),
            item -> {
              attempts.incrementAndGet();
              throw new RuntimeException("failed");
            });

    assertThat(results.getValues()).isEmpty();
    assertThat(results.getErrorCount()).isOne();
    assertThat(attempts).hasValue(1);
  }

  @Test
  void everyItemContributesEitherAValueOrAnError() {
    final MappedResults<String> results =
        load(
            items(300),
            item -> {
              if (ThreadLocalRandom.current().nextInt(4) == 0) {
                throw new RuntimeException("unclassified");
              }
              return value(item);
            });

    assertThat(results.getValues().size() + results.getErrorCount()).isEqualTo(300);
  }

  @Test
  void concurrencyNeverExceedsTheConfiguredMaximum() {
    final AtomicInteger inFlight = new AtomicInteger();
    final AtomicInteger peak = new AtomicInteger();

    final MappedResults<String> results =
        load(
            items(200),
            item -> {
              peak.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
              try {
                Thread.sleep(Duration.ofMillis(2));
              } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
              } finally {
                inFlight.decrementAndGet();
              }
              return value(item);
            });

    assertThat(results.getErrorCount()).isZero();
    assertThat(peak.get()).isPositive().isLessThanOrEqualTo(FAST_OPTIONS.maxConcurrency());
  }

  @Test
  void anExpiredDeadlineIsReportedRatherThanSilentlyDroppingItems() {
    final BulkLoadOptions shortDeadline = new BulkLoadOptions(2, Duration.ofMillis(150));

    final MappedResults<String> results =
        new ConcurrentBulkLoader(shortDeadline)
            .load(
                "test",
                items(500),
                Function.identity(),
                item -> {
                  try {
                    Thread.sleep(Duration.ofMillis(5));
                  } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                  return value(item);
                });

    assertThat(results.getValues()).hasSizeLessThan(500);
    assertThat(results.getErrorCount()).isPositive();
  }

  @Test
  void aListingFailureIsReportedAsAnError() {
    final Stream<String> failingListing =
        IntStream.range(0, 100)
            .mapToObj(
                index -> {
                  if (index == 10) {
                    throw new RuntimeException("listing failed");
                  }
                  return "item-" + index;
                });

    final MappedResults<String> results =
        new ConcurrentBulkLoader(FAST_OPTIONS)
            .load("test", failingListing, Function.identity(), ConcurrentBulkLoaderTest::value);

    assertThat(results.getValues()).hasSize(10);
    assertThat(results.getErrorCount()).isOne();
  }

  @Test
  void anInterruptedLoadIsReportedRatherThanSilentlyDroppingItems() throws InterruptedException {
    final CountDownLatch started = new CountDownLatch(1);
    final AtomicInteger errorCount = new AtomicInteger(-1);
    final AtomicInteger loadedCount = new AtomicInteger(-1);

    final Thread loader =
        new Thread(
            () -> {
              final MappedResults<String> results =
                  load(
                      items(500),
                      item -> {
                        started.countDown();
                        try {
                          Thread.sleep(Duration.ofMillis(50));
                        } catch (final InterruptedException e) {
                          Thread.currentThread().interrupt();
                        }
                        return value(item);
                      });
              loadedCount.set(results.getValues().size());
              errorCount.set(results.getErrorCount());
            });
    loader.start();
    assertThat(started.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
    loader.interrupt();
    loader.join(Duration.ofSeconds(30));

    assertThat(loadedCount.get()).isNotNegative().isLessThan(500);
    assertThat(errorCount.get()).isPositive();
  }

  private static MappedResults<String> load(
      final Stream<String> items, final Function<String, MappedResults<String>> work) {
    return new ConcurrentBulkLoader(FAST_OPTIONS).load("test", items, Function.identity(), work);
  }

  private static Stream<String> items(final int count) {
    return IntStream.range(0, count).mapToObj(index -> "item-" + index);
  }

  private static MappedResults<String> value(final String item) {
    return MappedResults.newInstance(List.of(item), 0);
  }
}
