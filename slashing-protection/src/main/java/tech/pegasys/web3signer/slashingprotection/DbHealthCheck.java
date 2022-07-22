/*
 * Copyright 2022 ConsenSys AG.
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DbHealthCheck implements Runnable {

  private final SlashingProtectionContext slashingProtectionContext;
  private final long dbHealthCheckTimeoutMilliseconds;
  private final AtomicBoolean isDbUp = new AtomicBoolean(true);

  public DbHealthCheck(
      final SlashingProtectionContext slashingProtectionContext,
      final long dbHealthCheckTimeoutMilliseconds) {
    this.slashingProtectionContext = slashingProtectionContext;
    this.dbHealthCheckTimeoutMilliseconds = dbHealthCheckTimeoutMilliseconds;
  }

  @Override
  public void run() {
    try {
      CompletableFuture<Integer> future =
          CompletableFuture.supplyAsync(
              () ->
                  slashingProtectionContext
                      .getSlashingProtectionJdbi()
                      .withHandle(h -> h.execute("SELECT 1")));
      // Check db health with timeout.
      future.get(this.dbHealthCheckTimeoutMilliseconds, TimeUnit.MILLISECONDS);
      isDbUp.set(true);
    } catch (Throwable e) {
      // Have exception in database health check (timeout or error).
      isDbUp.set(false);
    }
  }

  public boolean isDbUp() {
    return isDbUp.get();
  }
}
