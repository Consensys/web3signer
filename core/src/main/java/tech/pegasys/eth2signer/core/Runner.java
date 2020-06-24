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
package tech.pegasys.eth2signer.core;

import tech.pegasys.eth2signer.core.http.HostAllowListHandler;
import tech.pegasys.eth2signer.core.http.handlers.GetPublicKeysHandler;
import tech.pegasys.eth2signer.core.http.handlers.LogErrorHandler;
import tech.pegasys.eth2signer.core.http.handlers.SignForPublicKeyHandler;
import tech.pegasys.eth2signer.core.http.handlers.UpcheckHandler;
import tech.pegasys.eth2signer.core.metrics.MetricsEndpoint;
import tech.pegasys.eth2signer.core.metrics.VertxMetricsAdapterFactory;
import tech.pegasys.eth2signer.core.multikey.DirectoryBackedArtifactSignerProvider;
import tech.pegasys.eth2signer.core.multikey.metadata.ArtifactSignerFactory;
import tech.pegasys.eth2signer.core.multikey.metadata.parser.YamlSignerParser;
import tech.pegasys.signers.hashicorp.HashicorpConnectionFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.hyperledger.besu.plugin.services.MetricsSystem;

public class Runner implements Runnable {

  private static final Logger LOG = LogManager.getLogger();

  private static final String CONTENT_TYPE_TEXT_HTML = "text/html; charset=utf-8";
  private static final String CONTENT_TYPE_YAML = "text/x-yaml";

  public static final String OPENAPI_INDEX_RESOURCE = "openapi/index.html";
  public static final String OPENAPI_SPEC_RESOURCE = "openapi/eth2signer.yaml";

  // operationId as defined in eth2signer.yaml
  private static final String UPCHECK_OPERATION_ID = "upcheck";
  private static final String GET_PUBLIC_KEYS_OPERATION_ID = "getPublicKeys";
  private static final String SIGN_FOR_PUBLIC_KEY_OPERATION_ID = "signForPublicKey";

  private final Config config;

  public Runner(final Config config) {
    this.config = config;
  }

  @Override
  public void run() {
    // set log level per CLI flags
    System.out.println("Setting logging level to " + config.getLogLevel().name());
    Configurator.setAllLevels("", config.getLogLevel());

    final MetricsEndpoint metricsEndpoint =
        new MetricsEndpoint(
            config.isMetricsEnabled(),
            config.getMetricsPort(),
            config.getMetricsNetworkInterface(),
            config.getMetricCategories(),
            config.getMetricsHostAllowList());

    final MetricsSystem metricsSystem = metricsEndpoint.getMetricsSystem();
    final MetricsOptions metricsOptions =
        new MetricsOptions()
            .setEnabled(true)
            .setFactory(new VertxMetricsAdapterFactory(metricsSystem));
    final VertxOptions vertxOptions = new VertxOptions().setMetricsOptions(metricsOptions);
    final Vertx vertx = Vertx.vertx(vertxOptions);

    try {
      metricsEndpoint.start(vertx);

      final DirectoryBackedArtifactSignerProvider signerProvider =
          createSignerProvider(metricsSystem, vertx);
      signerProvider.cacheAllSigners();

      final OpenAPI3RouterFactory openApiRouterFactory =
          createOpenApiRouterFactory(vertx, signerProvider);
      registerHttpHostAllowListHandler(openApiRouterFactory);
      final Router router = openApiRouterFactory.getRouter();
      registerOpenApiSpecRoute(router); // serve static openapi spec

      final HttpServer httpServer = createServerAndWait(vertx, router);
      LOG.info("Server is up, and listening on {}", httpServer.actualPort());

      persistPortInformation(httpServer.actualPort(), metricsEndpoint.getPort());
    } catch (final Throwable e) {
      vertx.close();
      metricsEndpoint.stop();
      LOG.error("Failed to create Http Server", e);
    }
  }

  private void registerHttpHostAllowListHandler(final OpenAPI3RouterFactory openApiRouterFactory) {
    openApiRouterFactory.addGlobalHandler(new HostAllowListHandler(config.getHttpHostAllowList()));
  }

  private OpenAPI3RouterFactory createOpenApiRouterFactory(
      final Vertx vertx, final DirectoryBackedArtifactSignerProvider signerProvider)
      throws InterruptedException, ExecutionException {
    final LogErrorHandler errorHandler = new LogErrorHandler();
    final OpenAPI3RouterFactory openAPI3RouterFactory = getOpenAPI3RouterFactory(vertx);

    openAPI3RouterFactory.addHandlerByOperationId(UPCHECK_OPERATION_ID, new UpcheckHandler());
    openAPI3RouterFactory.addFailureHandlerByOperationId(UPCHECK_OPERATION_ID, errorHandler);

    openAPI3RouterFactory.addHandlerByOperationId(
        GET_PUBLIC_KEYS_OPERATION_ID, new GetPublicKeysHandler(signerProvider));
    openAPI3RouterFactory.addFailureHandlerByOperationId(
        GET_PUBLIC_KEYS_OPERATION_ID, errorHandler);

    openAPI3RouterFactory.addHandlerByOperationId(
        SIGN_FOR_PUBLIC_KEY_OPERATION_ID, new SignForPublicKeyHandler(signerProvider));
    openAPI3RouterFactory.addFailureHandlerByOperationId(
        SIGN_FOR_PUBLIC_KEY_OPERATION_ID, errorHandler);

    return openAPI3RouterFactory;
  }

  private OpenAPI3RouterFactory getOpenAPI3RouterFactory(final Vertx vertx)
      throws InterruptedException, ExecutionException {
    final CompletableFuture<OpenAPI3RouterFactory> completableFuture = new CompletableFuture<>();
    OpenAPI3RouterFactory.create(
        vertx,
        OPENAPI_SPEC_RESOURCE,
        ar -> {
          if (ar.succeeded()) {
            completableFuture.complete(ar.result());
          } else {
            completableFuture.completeExceptionally(ar.cause());
          }
        });

    return completableFuture.get();
  }

  private DirectoryBackedArtifactSignerProvider createSignerProvider(
      final MetricsSystem metricsSystem, final Vertx vertx) {
    final ArtifactSignerFactory artifactSignerFactory =
        new ArtifactSignerFactory(
            config.getKeyConfigPath(), metricsSystem, new HashicorpConnectionFactory(vertx));
    return new DirectoryBackedArtifactSignerProvider(
        config.getKeyConfigPath(),
        "yaml",
        new YamlSignerParser(artifactSignerFactory),
        config.getKeyCacheLimit());
  }

  private void registerOpenApiSpecRoute(final Router router) throws IOException {
    final URL indexResourceUrl = Resources.getResource(OPENAPI_INDEX_RESOURCE);
    final URL openApiSpecUrl = Resources.getResource(OPENAPI_SPEC_RESOURCE);
    final String indexHtml = Resources.toString(indexResourceUrl, Charsets.UTF_8);
    final String openApiSpecYaml = Resources.toString(openApiSpecUrl, Charsets.UTF_8);

    router
        .route(HttpMethod.GET, "/openapi/eth2signer.yaml")
        .produces(CONTENT_TYPE_YAML)
        .handler(ResponseContentTypeHandler.create())
        .handler(routingContext -> routingContext.response().end(openApiSpecYaml));

    router
        .route(HttpMethod.GET, "/openapi/*")
        .produces(CONTENT_TYPE_TEXT_HTML)
        .handler(ResponseContentTypeHandler.create())
        .handler(routingContext -> routingContext.response().end(indexHtml));
  }

  private HttpServer createServerAndWait(
      final Vertx vertx, final Handler<HttpServerRequest> requestHandler)
      throws ExecutionException, InterruptedException {
    final HttpServerOptions serverOptions =
        new HttpServerOptions()
            .setPort(config.getHttpListenPort())
            .setHost(config.getHttpListenHost())
            .setReuseAddress(true)
            .setReusePort(true);

    final HttpServer httpServer = vertx.createHttpServer(serverOptions);
    final CompletableFuture<Void> serverRunningFuture = new CompletableFuture<>();
    httpServer
        .requestHandler(requestHandler)
        .listen(
            result -> {
              if (result.succeeded()) {
                serverRunningFuture.complete(null);
              } else {
                serverRunningFuture.completeExceptionally(result.cause());
              }
            });
    serverRunningFuture.get();

    return httpServer;
  }

  private void persistPortInformation(final int httpPort, final Optional<Integer> metricsPort) {
    if (config.getDataPath() == null) {
      return;
    }

    final File portsFile = new File(config.getDataPath().toFile(), "eth2signer.ports");
    portsFile.deleteOnExit();

    final Properties properties = new Properties();
    properties.setProperty("http-port", String.valueOf(httpPort));
    metricsPort.ifPresent(port -> properties.setProperty("metrics-port", String.valueOf(port)));

    LOG.info(
        "Writing eth2signer.ports file: {}, with contents: {}",
        portsFile.getAbsolutePath(),
        properties);
    try (final FileOutputStream fileOutputStream = new FileOutputStream(portsFile)) {
      properties.store(
          fileOutputStream,
          "This file contains the ports used by the running instance of Eth2Signer. "
              + "This file will be deleted after the node is shutdown.");
    } catch (final Exception e) {
      LOG.warn("Error writing ports file", e);
    }
  }
}
