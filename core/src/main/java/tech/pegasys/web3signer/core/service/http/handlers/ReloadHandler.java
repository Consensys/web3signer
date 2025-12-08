/*
 * Copyright 2023 ConsenSys AG.
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
package tech.pegasys.web3signer.core.service.http.handlers;

import tech.pegasys.web3signer.signing.ArtifactSignerProvider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.vertx.core.Handler;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ReloadHandler implements Handler<RoutingContext> {
  private static final Logger LOG = LogManager.getLogger();

  private final List<ArtifactSignerProvider> orderedArtifactSignerProviders;
  private final WorkerExecutor workerExecutor;
  private final AtomicBoolean reloadInProgress = new AtomicBoolean(false);

  public ReloadHandler(
      final List<ArtifactSignerProvider> orderedArtifactSignerProviders,
      final WorkerExecutor workerExecutor) {
    this.orderedArtifactSignerProviders = orderedArtifactSignerProviders;
    this.workerExecutor = workerExecutor;
  }

  @Override
  public void handle(final RoutingContext routingContext) {
    // Check if reload is already in progress
    if (!reloadInProgress.compareAndSet(false, true)) {
      routingContext
          .response()
          .setStatusCode(409) // Conflict
          .putHeader("Content-Type", "application/json")
          .end(
              Json.encode(
                  Map.of(
                      "status", "error",
                      "message",
                          "A reload operation is already in progress. Please try again later.")));
      return;
    }

    LOG.debug("Reload operation initiated");

    workerExecutor
        .executeBlocking(
            () -> {
              for (ArtifactSignerProvider signerProvider : orderedArtifactSignerProviders) {
                try {
                  signerProvider.load().get();
                } catch (final InterruptedException e) {
                  Thread.currentThread().interrupt();
                  LOG.error("Interrupted while reloading signer", e);
                  throw new RuntimeException("Reload interrupted", e);
                } catch (final ExecutionException e) {
                  LOG.error("Error reloading signer", e);
                  throw new RuntimeException("Reload failed", e);
                }
              }
              return null;
            },
            false) // unordered is fine since we have pool size 1
        .onSuccess(
            result -> {
              reloadInProgress.set(false);
              LOG.info("Reload operation completed successfully");
            })
        .onFailure(
            err -> {
              reloadInProgress.set(false);
              LOG.error("Reload operation failed", err);
            });

    // Respond immediately - reload happens in background
    routingContext
        .response()
        .setStatusCode(202) // Accepted (not using 200 - OK)
        .putHeader("Content-Type", "application/json")
        .end(
            Json.encode(
                Map.of(
                    "status", "accepted",
                    "message",
                        "Reload operation accepted and is running in the background. Check /healthcheck for status.")));
  }
}
