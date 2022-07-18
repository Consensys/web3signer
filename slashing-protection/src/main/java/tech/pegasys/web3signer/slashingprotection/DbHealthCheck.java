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

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DbHealthCheck implements Runnable {

  private final Optional<SlashingProtectionContext> slashingProtectionContext;
  private final AtomicBoolean isDbDown;

  public DbHealthCheck(final Optional<SlashingProtectionContext> slashingProtectionContext) {
    this.slashingProtectionContext = slashingProtectionContext;
    this.isDbDown = new AtomicBoolean(false);
  }

  @Override
  public void run() {
    slashingProtectionContext.ifPresent(
        protectionContext ->
            Executors.newScheduledThreadPool(1)
                .scheduleAtFixedRate(
                    () -> {
                      Future<Integer> future =
                          Executors.newCachedThreadPool()
                              .submit(
                                  () ->
                                      protectionContext
                                          .getSlashingProtectionJdbi()
                                          .withHandle(
                                              h ->
                                                  h.createQuery("SELECT 1")
                                                      .mapTo(Integer.class)
                                                      .one()));
                      try {
                        // Check db health with timeout.
                        future.get(3000, TimeUnit.MILLISECONDS);
                        isDbDown.set(false);
                      } catch (Exception e) {
                        // Have exception in database health check (timeout or error).
                        isDbDown.set(true);
                      } finally {
                        future.cancel(true);
                      }
                    },
                    3000,
                    3000,
                    TimeUnit.MILLISECONDS));
  }

  public boolean isDbDown() {
    return isDbDown.get();
  }
}
