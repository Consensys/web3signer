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

import static tech.pegasys.eth2signer.core.signing.ArtifactSignatureType.BLS;
import static tech.pegasys.eth2signer.core.signing.ArtifactSignatureType.SECP256K1;

import tech.pegasys.eth2signer.core.config.ClientAuthConstraints;
import tech.pegasys.eth2signer.core.config.Config;
import tech.pegasys.eth2signer.core.config.TlsOptions;
import tech.pegasys.eth2signer.core.http.HostAllowListHandler;
import tech.pegasys.eth2signer.core.http.handlers.GetPublicKeysHandler;
import tech.pegasys.eth2signer.core.http.handlers.LogErrorHandler;
import tech.pegasys.eth2signer.core.http.handlers.SignForPublicKeyHandler;
import tech.pegasys.eth2signer.core.http.handlers.UpcheckHandler;
import tech.pegasys.eth2signer.core.metrics.MetricsEndpoint;
import tech.pegasys.eth2signer.core.metrics.VertxMetricsAdapterFactory;
import tech.pegasys.eth2signer.core.multikey.DirectoryBackedArtifactSignerProvider;
import tech.pegasys.eth2signer.core.multikey.SecpArtifactSignerProvider;
import tech.pegasys.eth2signer.core.multikey.metadata.ArtifactSignerFactory;
import tech.pegasys.eth2signer.core.multikey.metadata.parser.YamlSignerParser;
import tech.pegasys.eth2signer.core.signing.ArtifactSignerProvider;
import tech.pegasys.eth2signer.core.signing.BlsArtifactSignature;
import tech.pegasys.eth2signer.core.signing.SecpArtifactSignature;
import tech.pegasys.eth2signer.core.util.FileUtil;
import tech.pegasys.eth2signer.core.utils.ByteUtils;
import tech.pegasys.signers.hashicorp.HashicorpConnectionFactory;
import tech.pegasys.signers.secp256k1.api.Signature;
import tech.pegasys.signers.secp256k1.multikey.MultiKeyTransactionSignerProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.net.tls.VertxTrustOptions;
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
  private static final String SWAGGER_ENDPOINT = "/swagger-ui";

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

      final DirectoryBackedArtifactSignerProvider blsSignerProvider =
          createSignerProvider(metricsSystem, vertx);
      blsSignerProvider.cacheAllSigners();

      final MultiKeyTransactionSignerProvider multiKeyTransactionSignerProvider =
          MultiKeyTransactionSignerProvider.create(config.getKeyConfigPath());
      final SecpArtifactSignerProvider secpSignerProvider =
          new SecpArtifactSignerProvider(multiKeyTransactionSignerProvider);

      final OpenAPI3RouterFactory openApiRouterFactory =
          createOpenApiRouterFactory(vertx, blsSignerProvider, secpSignerProvider);
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
      final Vertx vertx,
      final ArtifactSignerProvider blsSignerProvider,
      final ArtifactSignerProvider secpSignerProvider)
      throws InterruptedException, ExecutionException {
    final LogErrorHandler errorHandler = new LogErrorHandler();
    final OpenAPI3RouterFactory openAPI3RouterFactory = getOpenAPI3RouterFactory(vertx);
    openAPI3RouterFactory
        .getOptions()
        .setMountResponseContentTypeHandler(false); // manually set content-type

    openAPI3RouterFactory.addHandlerByOperationId(UPCHECK_OPERATION_ID, new UpcheckHandler());
    openAPI3RouterFactory.addFailureHandlerByOperationId(UPCHECK_OPERATION_ID, errorHandler);

    openAPI3RouterFactory.addHandlerByOperationId(
        GET_PUBLIC_KEYS_OPERATION_ID, new GetPublicKeysHandler(blsSignerProvider));
    openAPI3RouterFactory.addFailureHandlerByOperationId(
        GET_PUBLIC_KEYS_OPERATION_ID, errorHandler);

    openAPI3RouterFactory.addHandlerByOperationId(
        SIGN_FOR_PUBLIC_KEY_OPERATION_ID,
        new SignForPublicKeyHandler<>(blsSignerProvider, this::formatBlsSignature, BLS));
    openAPI3RouterFactory.addHandlerByOperationId(
        SIGN_FOR_PUBLIC_KEY_OPERATION_ID,
        new SignForPublicKeyHandler<>(secpSignerProvider, this::formatSecpSignature, SECP256K1));
    openAPI3RouterFactory.addFailureHandlerByOperationId(
        SIGN_FOR_PUBLIC_KEY_OPERATION_ID, errorHandler);

    return openAPI3RouterFactory;
  }

  private String formatBlsSignature(final BlsArtifactSignature signature) {
    return signature.getSignatureData().toString();
  }

  private String formatSecpSignature(final SecpArtifactSignature signature) {
    final Signature signatureData = signature.getSignatureData();
    final Bytes outputSignature =
        Bytes.concatenate(
            Bytes32.leftPad(Bytes.wrap(ByteUtils.bigIntegerToBytes(signatureData.getR()))),
            Bytes32.leftPad(Bytes.wrap(ByteUtils.bigIntegerToBytes(signatureData.getS()))),
            Bytes.wrap(ByteUtils.bigIntegerToBytes(signatureData.getV())));
    return outputSignature.toHexString();
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
        .route(HttpMethod.GET, SWAGGER_ENDPOINT + "/eth2signer.yaml")
        .produces(CONTENT_TYPE_YAML)
        .handler(ResponseContentTypeHandler.create())
        .handler(routingContext -> routingContext.response().end(openApiSpecYaml));

    router
        .routeWithRegex(HttpMethod.GET, SWAGGER_ENDPOINT + "|" + SWAGGER_ENDPOINT + "/*")
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
              whitelistFile ->
                  result.setTrustOptions(
                      VertxTrustOptions.whitelistClients(
                          whitelistFile.toPath(), constraints.isCaAuthorizedClientAllowed())));
    } catch (final IllegalArgumentException e) {
      throw new InitializationException("Illegally formatted client fingerprint file.");
    }

    return result;
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
