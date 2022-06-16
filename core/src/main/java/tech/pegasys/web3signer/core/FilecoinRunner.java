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

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS;
import static com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static tech.pegasys.web3signer.core.service.http.OpenApiOperationsId.RELOAD;
import static tech.pegasys.web3signer.core.service.http.handlers.ContentTypes.JSON_UTF_8;

import tech.pegasys.signers.aws.AwsSecretsManagerProvider;
import tech.pegasys.signers.hashicorp.HashicorpConnectionFactory;
import tech.pegasys.signers.secp256k1.azure.AzureKeyVaultSignerFactory;
import tech.pegasys.web3signer.core.config.Config;
import tech.pegasys.web3signer.core.service.jsonrpc.FcJsonRpc;
import tech.pegasys.web3signer.core.service.jsonrpc.FcJsonRpcMetrics;
import tech.pegasys.web3signer.core.service.jsonrpc.FilecoinJsonRpcModule;
import tech.pegasys.web3signer.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.signing.FcBlsArtifactSigner;
import tech.pegasys.web3signer.signing.FcSecpArtifactSigner;
import tech.pegasys.web3signer.signing.config.DefaultArtifactSignerProvider;
import tech.pegasys.web3signer.signing.config.SignerLoader;
import tech.pegasys.web3signer.signing.config.metadata.AbstractArtifactSignerFactory;
import tech.pegasys.web3signer.signing.config.metadata.BlsArtifactSignerFactory;
import tech.pegasys.web3signer.signing.config.metadata.Secp256k1ArtifactSignerFactory;
import tech.pegasys.web3signer.signing.config.metadata.interlock.InterlockKeyProvider;
import tech.pegasys.web3signer.signing.config.metadata.parser.YamlSignerParser;
import tech.pegasys.web3signer.signing.config.metadata.yubihsm.YubiHsmOpaqueDataProvider;
import tech.pegasys.web3signer.signing.filecoin.FilecoinNetwork;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.arteam.simplejsonrpc.server.JsonRpcServer;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.openapi.RouterBuilder;
import org.hyperledger.besu.plugin.services.MetricsSystem;

public class FilecoinRunner extends Runner {
  private static final int AWS_CACHE_MAXIMUM_SIZE = 1;
  private static final String FC_JSON_RPC_PATH = "/rpc/v0";
  private final FilecoinNetwork network;

  public FilecoinRunner(final Config config, final FilecoinNetwork network) {
    super(config);
    this.network = network;
  }

  @Override
  protected String getOpenApiSpecResource() {
    return "filecoin/web3signer.yaml";
  }

  @Override
  protected Router populateRouter(final Context context) {
    addReloadHandler(
        context.getRouterBuilder(),
        context.getArtifactSignerProvider(),
        RELOAD.name(),
        context.getErrorHandler());

    return registerFilecoinJsonRpcRoute(
        context.getRouterBuilder(),
        context.getMetricsSystem(),
        context.getArtifactSignerProvider());
  }

  private Router registerFilecoinJsonRpcRoute(
      final RouterBuilder routerBuilder,
      final MetricsSystem metricsSystem,
      final ArtifactSignerProvider fcSigners) {

    final Router router = routerBuilder.createRouter();

    final FcJsonRpcMetrics fcJsonRpcMetrics = new FcJsonRpcMetrics(metricsSystem);
    final FcJsonRpc fileCoinJsonRpc = new FcJsonRpc(fcSigners, fcJsonRpcMetrics);
    final ObjectMapper mapper =
        JsonMapper.builder()
            .enable(ACCEPT_CASE_INSENSITIVE_ENUMS)
            .enable(ACCEPT_CASE_INSENSITIVE_PROPERTIES)
            .disable(FAIL_ON_UNKNOWN_PROPERTIES)
            .addModule(new FilecoinJsonRpcModule())
            .build();
    final JsonRpcServer jsonRpcServer = JsonRpcServer.withMapper(mapper);

    router
        .post(FC_JSON_RPC_PATH)
        .handler(fcJsonRpcMetrics::incTotalFilecoinRequests)
        .blockingHandler(
            routingContext -> {
              final String body = routingContext.getBodyAsString();
              final String jsonRpcResponse = jsonRpcServer.handle(body, fileCoinJsonRpc);
              routingContext.response().putHeader(CONTENT_TYPE, JSON_UTF_8).end(jsonRpcResponse);
            },
            false);

    return router;
  }

  @Override
  protected ArtifactSignerProvider createArtifactSignerProvider(
      final Vertx vertx, final MetricsSystem metricsSystem) {
    return new DefaultArtifactSignerProvider(
        () -> {
          final AzureKeyVaultSignerFactory azureFactory = new AzureKeyVaultSignerFactory();
          final HashicorpConnectionFactory hashicorpConnectionFactory =
              new HashicorpConnectionFactory(vertx);

          try (final InterlockKeyProvider interlockKeyProvider = new InterlockKeyProvider(vertx);
              final YubiHsmOpaqueDataProvider yubiHsmOpaqueDataProvider =
                  new YubiHsmOpaqueDataProvider();
              final AwsSecretsManagerProvider awsSecretsManagerProvider =
                  new AwsSecretsManagerProvider(AWS_CACHE_MAXIMUM_SIZE)) {

            final AbstractArtifactSignerFactory blsArtifactSignerFactory =
                new BlsArtifactSignerFactory(
                    config.getKeyConfigPath(),
                    metricsSystem,
                    hashicorpConnectionFactory,
                    interlockKeyProvider,
                    yubiHsmOpaqueDataProvider,
                    awsSecretsManagerProvider,
                    (args) -> new FcBlsArtifactSigner(args.getKeyPair(), network));

            final AbstractArtifactSignerFactory secpArtifactSignerFactory =
                new Secp256k1ArtifactSignerFactory(
                    hashicorpConnectionFactory,
                    config.getKeyConfigPath(),
                    azureFactory,
                    interlockKeyProvider,
                    yubiHsmOpaqueDataProvider,
                    signer -> new FcSecpArtifactSigner(signer, network),
                    false);

            return new SignerLoader()
                .load(
                    config.getKeyConfigPath(),
                    "yaml",
                    new YamlSignerParser(
                        List.of(blsArtifactSignerFactory, secpArtifactSignerFactory)));
          }
        });
  }
}
