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

import static tech.pegasys.web3signer.core.config.HealthCheckNames.DEFAULT_CHECK;
import static tech.pegasys.web3signer.core.config.HealthCheckNames.KEYS_CHECK_UNEXPECTED;
import static tech.pegasys.web3signer.core.service.http.OpenApiOperationsId.HEALTHCHECK;
import static tech.pegasys.web3signer.core.service.http.OpenApiOperationsId.UPCHECK;

import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.web3signer.common.ApplicationInfo;
import tech.pegasys.web3signer.core.config.ClientAuthConstraints;
import tech.pegasys.web3signer.core.config.Config;
import tech.pegasys.web3signer.core.config.TlsOptions;
import tech.pegasys.web3signer.core.metrics.MetricsEndpoint;
import tech.pegasys.web3signer.core.metrics.vertx.VertxMetricsAdapterFactory;
import tech.pegasys.web3signer.core.service.http.HostAllowListHandler;
import tech.pegasys.web3signer.core.service.http.SwaggerUIRoute;
import tech.pegasys.web3signer.core.service.http.handlers.LogErrorHandler;
import tech.pegasys.web3signer.core.service.http.handlers.PublicKeysListHandler;
import tech.pegasys.web3signer.core.service.http.handlers.UpcheckHandler;
import tech.pegasys.web3signer.core.util.FileUtil;
import tech.pegasys.web3signer.core.util.OpenApiSpecsExtractor;
import tech.pegasys.web3signer.signing.ArtifactSignerProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.impl.BlockingHandlerDecorator;
import io.vertx.ext.web.openapi.RouterBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.tuweni.net.tls.VertxTrustOptions;
import org.hyperledger.besu.metrics.StandardMetricCategory;
import org.hyperledger.besu.plugin.services.MetricsSystem;

public abstract class Runner implements Runnable {

  private static final Logger LOG = LogManager.getLogger();

  protected final Config config;

  private HealthCheckHandler healthCheckHandler;

  protected Runner(final Config config) {
    this.config = config;
  }

  @Override
  public void run() {
    if (config.getLogLevel() != null) {
      System.out.println("Setting logging level to " + config.getLogLevel().name());
      Configurator.setRootLevel(config.getLogLevel());
    }

    final MetricsEndpoint metricsEndpoint =
        new MetricsEndpoint(
            config.isMetricsEnabled(),
            config.getMetricsPort(),
            config.getMetricsNetworkInterface(),
            config.getMetricCategories(),
            config.getMetricsHostAllowList());

    final MetricsSystem metricsSystem = metricsEndpoint.getMetricsSystem();

    final Vertx vertx = Vertx.vertx(createVertxOptions(metricsSystem));
    final LogErrorHandler errorHandler = new LogErrorHandler();
    healthCheckHandler = HealthCheckHandler.create(vertx);

    final ArtifactSignerProvider artifactSignerProvider =
        createArtifactSignerProvider(vertx, metricsSystem);

    try {
      createVersionMetric(metricsSystem);
      metricsEndpoint.start(vertx);
      try {
        artifactSignerProvider.load().get(); // wait for signers to get loaded ...
      } catch (final InterruptedException | ExecutionException e) {
        LOG.error("Error loading signers", e);
        registerHealthCheckProcedure(
            KEYS_CHECK_UNEXPECTED, promise -> promise.complete(Status.KO()));
      }

      final OpenApiSpecsExtractor openApiSpecsExtractor =
          new OpenApiSpecsExtractor.OpenApiSpecsExtractorBuilder()
              .withConvertRelativeRefToAbsoluteRef(true)
              .withForceDeleteOnJvmExit(true)
              .build();
      final Path openApiSpec =
          openApiSpecsExtractor
              .getSpecFilePathAtDestination(getOpenApiSpecResource())
              .orElseThrow(
                  () ->
                      new RuntimeException(
                          "Unable to load OpenApi spec " + getOpenApiSpecResource()));
      // vertx needs a scheme present (file://) to determine this is an absolute path
      final URI openApiSpecUri = openApiSpec.toUri();
      final RouterBuilder routerBuilder = getRouterBuilder(vertx, openApiSpecUri.toString());
      // register access log handler first
      if (config.isAccessLogsEnabled()) {
        routerBuilder.rootHandler(LoggerHandler.create(LoggerFormat.DEFAULT));
      }

      routerBuilder.rootHandler(
          CorsHandler.create(buildCorsRegexFromConfig())
              .allowedHeader("*")
              .allowedMethod(HttpMethod.GET)
              .allowedMethod(HttpMethod.POST)
              .allowedMethod(HttpMethod.DELETE)
              .allowedMethod(HttpMethod.OPTIONS));

      /*
       Add our own instance of BodyHandler as the default BodyHandler doesn't seem to handle large json bodies.
       BodyHandler must be first handler after platform and security handlers
      */
      routerBuilder.rootHandler(BodyHandler.create());
      registerUpcheckRoute(routerBuilder, errorHandler);
      registerHttpHostAllowListHandler(routerBuilder);

      routerBuilder
          .operation(HEALTHCHECK.name())
          .handler(healthCheckHandler)
          .failureHandler(errorHandler);

      registerHealthCheckProcedure(DEFAULT_CHECK, promise -> promise.complete(Status.OK()));

      final Context context =
          new Context(routerBuilder, metricsSystem, errorHandler, vertx, artifactSignerProvider);

      final Router router = populateRouter(context);
      if (config.isSwaggerUIEnabled()) {
        new SwaggerUIRoute(router).register();
      }

      final HttpServer httpServer = createServerAndWait(vertx, router);
      final String tlsStatus = config.getTlsOptions().isPresent() ? "enabled" : "disabled";
      LOG.info(
          "Web3Signer has started with TLS {}, and ready to handle signing requests on {}:{}",
          tlsStatus,
          config.getHttpListenHost(),
          httpServer.actualPort());

      persistPortInformation(httpServer.actualPort(), metricsEndpoint.getPort());
    } catch (final InitializationException e) {
      throw e;
    } catch (final Throwable e) {
      if (artifactSignerProvider != null) {
        artifactSignerProvider.close();
      }
      vertx.close();
      metricsEndpoint.stop();
      LOG.error("Failed to initialise application", e);
    }
  }

  private void createVersionMetric(final MetricsSystem metricsSystem) {
    metricsSystem
        .createLabelledGauge(
            StandardMetricCategory.PROCESS, "release", "Release information", "version")
        .labels(() -> 1, ApplicationInfo.version());
  }

  private VertxOptions createVertxOptions(final MetricsSystem metricsSystem) {
    return new VertxOptions()
        .setMetricsOptions(
            new MetricsOptions()
                .setEnabled(true)
                .setFactory(new VertxMetricsAdapterFactory(metricsSystem)));
  }

  protected abstract ArtifactSignerProvider createArtifactSignerProvider(
      final Vertx vertx, final MetricsSystem metricsSystem);

  protected abstract Router populateRouter(final Context context);

  protected abstract String getOpenApiSpecResource();

  public static RouterBuilder getRouterBuilder(final Vertx vertx, final String specUrl)
      throws InterruptedException, ExecutionException {
    final CompletableFuture<RouterBuilder> completableFuture = new CompletableFuture<>();
    RouterBuilder.create(
        vertx,
        specUrl,
        ar -> {
          if (ar.succeeded()) {
            completableFuture.complete(ar.result());
          } else {
            completableFuture.completeExceptionally(ar.cause());
          }
        });

    final RouterBuilder routerBuilder = completableFuture.get();

    // disable automatic response content handler as it doesn't handle some corner cases.
    // Our handlers must set content type header manually.
    routerBuilder.getOptions().setMountResponseContentTypeHandler(false);
    // vertx-json-schema fails to createRouter for unknown string type formats
    routerBuilder.getSchemaParser().withStringFormatValidator("uint64", Runner::validateUInt64);
    return routerBuilder;
  }

  private static boolean validateUInt64(final String value) {
    try {
      UInt64.valueOf(value);
      return true;
    } catch (RuntimeException e) {
      LOG.warn("Validation failed for uint64 value: {}", value);
      return false;
    }
  }

  protected void addPublicKeysListHandler(
      final RouterBuilder routerBuilder,
      final ArtifactSignerProvider artifactSignerProvider,
      final String operationId,
      final LogErrorHandler errorHandler) {
    routerBuilder
        .operation(operationId)
        .handler(
            new BlockingHandlerDecorator(new PublicKeysListHandler(artifactSignerProvider), false))
        .failureHandler(errorHandler);
  }

  protected void addReloadHandler(
      final RouterBuilder routerBuilder,
      final ArtifactSignerProvider artifactSignerProvider,
      final String operationId,
      final LogErrorHandler errorHandler) {
    routerBuilder
        .operation(operationId)
        .handler(
            routingContext -> {
              artifactSignerProvider.load();
              routingContext.response().setStatusCode(200).end();
            })
        .failureHandler(errorHandler);
  }

  private void registerUpcheckRoute(
      final RouterBuilder routerBuilder, final LogErrorHandler errorHandler) {
    routerBuilder
        .operation(UPCHECK.name())
        .handler(new BlockingHandlerDecorator(new UpcheckHandler(), false))
        .failureHandler(errorHandler);
  }

  protected void registerHealthCheckProcedure(
      final String name, final Handler<Promise<Status>> procedure) {
    healthCheckHandler.register(name, procedure);
  }

  private void registerHttpHostAllowListHandler(final RouterBuilder routerBuilder) {
    routerBuilder.rootHandler(new HostAllowListHandler(config.getHttpHostAllowList()));
  }

  private HttpServer createServerAndWait(
      final Vertx vertx, final Handler<HttpServerRequest> requestHandler)
      throws ExecutionException, InterruptedException {
    final HttpServerOptions serverOptions =
        new HttpServerOptions()
            .setPort(config.getHttpListenPort())
            .setHost(config.getHttpListenHost())
            .setIdleTimeout(config.getIdleConnectionTimeoutSeconds())
            .setIdleTimeoutUnit(TimeUnit.SECONDS)
            .setReuseAddress(true)
            .setReusePort(true);
    final HttpServerOptions tlsServerOptions = applyConfigTlsSettingsTo(serverOptions);
    final HttpServer httpServer = vertx.createHttpServer(tlsServerOptions);
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

  private HttpServerOptions applyConfigTlsSettingsTo(final HttpServerOptions input) {

    if (config.getTlsOptions().isEmpty()) {
      return input;
    }

    HttpServerOptions result = new HttpServerOptions(input);
    result.setSsl(true);
    final TlsOptions tlsConfig = config.getTlsOptions().get();

    result = applyTlsKeyStore(result, tlsConfig);

    if (tlsConfig.getClientAuthConstraints().isPresent()) {
      result = applyClientAuthentication(result, tlsConfig.getClientAuthConstraints().get());
    }

    return result;
  }

  private static HttpServerOptions applyTlsKeyStore(
      final HttpServerOptions input, final TlsOptions tlsConfig) {
    final HttpServerOptions result = new HttpServerOptions(input);

    try {
      final String keyStorePathname =
          tlsConfig.getKeyStoreFile().toPath().toAbsolutePath().toString();
      final String password =
          FileUtil.readFirstLineFromFile(tlsConfig.getKeyStorePasswordFile().toPath());
      result.setPfxKeyCertOptions(new PfxOptions().setPath(keyStorePathname).setPassword(password));
      return result;
    } catch (final NoSuchFileException e) {
      throw new InitializationException(
          "Requested file " + e.getMessage() + " does not exist at specified location.", e);
    } catch (final AccessDeniedException e) {
      throw new InitializationException(
          "Current user does not have permissions to access " + e.getMessage(), e);
    } catch (final IOException e) {
      throw new InitializationException("Failed to load TLS files " + e.getMessage(), e);
    }
  }

  private static HttpServerOptions applyClientAuthentication(
      final HttpServerOptions input, final ClientAuthConstraints constraints) {
    final HttpServerOptions result = new HttpServerOptions(input);

    result.setClientAuth(ClientAuth.REQUIRED);
    try {
      constraints
          .getKnownClientsFile()
          .ifPresent(
              allowlistFile ->
                  result.setTrustOptions(
                      VertxTrustOptions.allowlistClients(
                          allowlistFile.toPath(), constraints.isCaAuthorizedClientAllowed())));
    } catch (final IllegalArgumentException e) {
      throw new InitializationException("Illegally formatted client fingerprint file.");
    }

    return result;
  }

  private void persistPortInformation(final int httpPort, final Optional<Integer> metricsPort) {
    if (config.getDataPath() == null) {
      return;
    }

    final File portsFile = new File(config.getDataPath().toFile(), "web3signer.ports");
    portsFile.deleteOnExit();

    final Properties properties = new Properties();
    properties.setProperty("http-port", String.valueOf(httpPort));
    metricsPort.ifPresent(port -> properties.setProperty("metrics-port", String.valueOf(port)));

    LOG.debug(
        "Writing web3signer.ports file: {}, with contents: {}",
        portsFile.getAbsolutePath(),
        properties);
    try (final FileOutputStream fileOutputStream = new FileOutputStream(portsFile)) {
      properties.store(
          fileOutputStream,
          "This file contains the ports used by the running instance of Web3Signer. "
              + "This file will be deleted after the node is shutdown.");
    } catch (final Exception e) {
      LOG.warn("Error writing ports file", e);
    }
  }

  private String buildCorsRegexFromConfig() {
    if (config.getCorsAllowedOrigins().isEmpty()) {
      return "";
    }
    if (config.getCorsAllowedOrigins().contains("*")) {
      return ".*";
    } else {
      final StringJoiner stringJoiner = new StringJoiner("|");
      config.getCorsAllowedOrigins().stream().filter(s -> !s.isEmpty()).forEach(stringJoiner::add);
      return stringJoiner.toString();
    }
  }
}
