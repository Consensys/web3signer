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
package tech.pegasys.web3signer.core;

import tech.pegasys.web3signer.core.service.http.handlers.LogErrorHandler;
import tech.pegasys.web3signer.core.signing.ArtifactSignerProvider;

import io.vertx.core.Vertx;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import org.hyperledger.besu.plugin.services.MetricsSystem;

public class Context {
  private final OpenAPI3RouterFactory routerFactory;
  private final MetricsSystem metricsSystem;
  private final LogErrorHandler errorHandler;
  private final Vertx vertx;
  private final ArtifactSignerProvider artifactSignerProvider;

  public Context(
      final OpenAPI3RouterFactory routerFactory,
      final MetricsSystem metricsSystem,
      final LogErrorHandler errorHandler,
      final Vertx vertx,
      final ArtifactSignerProvider artifactSignerProvider) {
    this.routerFactory = routerFactory;
    this.metricsSystem = metricsSystem;
    this.errorHandler = errorHandler;
    this.vertx = vertx;
    this.artifactSignerProvider = artifactSignerProvider;
  }

  public OpenAPI3RouterFactory getRouterFactory() {
    return routerFactory;
  }

  public MetricsSystem getMetricsSystem() {
    return metricsSystem;
  }

  public LogErrorHandler getErrorHandler() {
    return errorHandler;
  }

  public Vertx getVertx() {
    return vertx;
  }

  public ArtifactSignerProvider getArtifactSignerProvider() {
    return artifactSignerProvider;
  }
}
