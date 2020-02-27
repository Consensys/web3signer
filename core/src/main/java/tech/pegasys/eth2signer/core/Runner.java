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

import tech.pegasys.eth2signer.core.http.LogErrorHandler;
import tech.pegasys.eth2signer.core.http.SigningRequestHandler;
import tech.pegasys.eth2signer.core.metrics.MetricsEndpoint;
import tech.pegasys.eth2signer.core.metrics.VertxMetricsAdapterFactory;
import tech.pegasys.eth2signer.core.multikey.MultiKeyArtifactSignerProvider;
import tech.pegasys.eth2signer.core.multikey.SigningMetadataTomlConfigLoader;
import tech.pegasys.eth2signer.core.signing.ArtifactSignerProvider;
import tech.pegasys.eth2signer.core.utils.JsonDecoder;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperledger.besu.plugin.services.MetricsSystem;

public class Runner implements Runnable {

  private static final Logger LOG = LogManager.getLogger();

  private static final String TEXT = HttpHeaderValues.TEXT_PLAIN.toString() + "; charset=utf-8";
  private static final String JSON = HttpHeaderValues.APPLICATION_JSON.toString();
  private final Config config;

  public Runner(final Config config) {
    this.config = config;
  }

  @Override
  public void run() {
    final MetricsEndpoint metricsEndpoint =
        new MetricsEndpoint(
            config.isMetricsEnabled(),
            config.getMetricsPort(),
            config.getMetricsNetworkInterface(),
            config.getMetricCategories());

    final VertxOptions vertxOptions =
        new VertxOptions()
            .setMetricsOptions(
                new MetricsOptions()
                    .setEnabled(true)
                    .setFactory(
                        new VertxMetricsAdapterFactory(metricsEndpoint.getMetricsSystem())));
    final Vertx vertx = Vertx.vertx(vertxOptions);

    try {
      metricsEndpoint.start(vertx);

      final Handler<HttpServerRequest> requestHandler =
          createRouter(vertx, metricsEndpoint.getMetricsSystem());
      final HttpServer httpServer = createServerAndWait(vertx, requestHandler);
      LOG.info("Server is up, and listening on {}", httpServer.actualPort());

      persistPortInformation(httpServer.actualPort());
    } catch (final ExecutionException | InterruptedException e) {
      vertx.close();
      throw new RuntimeException("Failed to create Http Server", e.getCause());
    } catch (final Throwable t) {
      metricsEndpoint.stop();
      vertx.close();
      throw t;
    }
  }

  private Handler<HttpServerRequest> createRouter(
      final Vertx vertx, final MetricsSystem metricsSystem) {
    final Router router = Router.router(vertx);
    final LogErrorHandler errorHandler = new LogErrorHandler();

    router
        .route(HttpMethod.GET, "/upcheck")
        .produces(TEXT)
        .handler(BodyHandler.create())
        .handler(ResponseContentTypeHandler.create())
        .failureHandler(errorHandler)
        .handler(routingContext -> routingContext.response().end("OK"));

    final SigningMetadataTomlConfigLoader configLoader =
        new SigningMetadataTomlConfigLoader(config.getKeyConfigPath());
    final ArtifactSignerProvider signerProvider = new MultiKeyArtifactSignerProvider(configLoader);

    final SigningRequestHandler signingHandler =
        new SigningRequestHandler(signerProvider, createJsonDecoder());

    router
        .routeWithRegex(HttpMethod.POST, "/signer/" + "(attestation|block)")
        .produces(JSON)
        .handler(ResponseContentTypeHandler.create())
        .failureHandler(errorHandler)
        .handler(signingHandler);

    return router;
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

  private void persistPortInformation(final int listeningPort) {
    if (config.getDataPath() == null) {
      return;
    }

    final File portsFile = new File(config.getDataPath().toFile(), "eth2signer.ports");
    portsFile.deleteOnExit();

    final Properties properties = new Properties();
    properties.setProperty("http-port", String.valueOf(listeningPort));

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

  private JsonDecoder createJsonDecoder() {
    // Force Transaction Deserialization to fail if missing expected properties
    final ObjectMapper jsonObjectMapper = new ObjectMapper();
    jsonObjectMapper.configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, true);
    jsonObjectMapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, true);

    return new JsonDecoder(jsonObjectMapper);
  }
}
