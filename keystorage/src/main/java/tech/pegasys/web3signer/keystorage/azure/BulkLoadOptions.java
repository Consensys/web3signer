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

import java.time.Duration;

import com.google.common.base.Preconditions;

/**
 * Tuning parameters for a bulk load performed by {@link ConcurrentBulkLoader}.
 *
 * @param maxConcurrency upper bound on concurrent requests.
 * @param deadline overall time budget for the load. Items not loaded within it are reported as
 *     errors rather than silently dropped.
 */
public record BulkLoadOptions(int maxConcurrency, Duration deadline) {

  public static final int DEFAULT_MAX_CONCURRENCY = 20;
  public static final Duration DEFAULT_DEADLINE = Duration.ofMinutes(15);

  /** Options applied when a caller does not configure bulk loading. */
  public static final BulkLoadOptions DEFAULT =
      new BulkLoadOptions(DEFAULT_MAX_CONCURRENCY, DEFAULT_DEADLINE);

  /** Validates the parameters. */
  public BulkLoadOptions {
    Preconditions.checkArgument(maxConcurrency > 0, "maxConcurrency must be positive");
    Preconditions.checkArgument(
        !deadline.isNegative() && !deadline.isZero(), "deadline must be positive");
  }
}
