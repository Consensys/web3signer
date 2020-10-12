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

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static tech.pegasys.web3signer.core.service.http.handlers.ContentTypes.JSON_UTF_8;
import static tech.pegasys.web3signer.core.service.http.metrics.HttpApiMetrics.incSignerLoadCount;

import tech.pegasys.signers.secp256k1.azure.AzureKeyVaultSignerFactory;
import tech.pegasys.web3signer.core.config.Config;
import tech.pegasys.web3signer.core.multikey.DefaultArtifactSignerProvider;
import tech.pegasys.web3signer.core.multikey.SignerLoader;
import tech.pegasys.web3signer.core.multikey.metadata.AbstractArtifactSignerFactory;
import tech.pegasys.web3signer.core.multikey.metadata.BlsArtifactSignerFactory;
import tech.pegasys.web3signer.core.multikey.metadata.Secp256k1ArtifactSignerFactory;
import tech.pegasys.web3signer.core.multikey.metadata.parser.YamlSignerParser;
import tech.pegasys.web3signer.core.service.jsonrpc.FcJsonRpc;
import tech.pegasys.web3signer.core.service.jsonrpc.FcJsonRpcMetrics;
import tech.pegasys.web3signer.core.service.jsonrpc.FilecoinJsonRpcModule;
import tech.pegasys.web3signer.core.signing.ArtifactSigner;
import tech.pegasys.web3signer.core.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.core.signing.FcBlsArtifactSigner;
import tech.pegasys.web3signer.core.signing.FcSecpArtifactSigner;
import tech.pegasys.web3signer.core.signing.filecoin.FilecoinNetwork;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.arteam.simplejsonrpc.server.JsonRpcServer;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.handler.BodyHandler;
import org.hyperledger.besu.plugin.services.MetricsSystem;

public class FilecoinRunner extends Runner {

  private static final String FC_JSON_RPC_PATH = JSON_RPC_PATH + "/filecoin";
  private final FilecoinNetwork network;

  public FilecoinRunner(final Config config, final FilecoinNetwork network) {
    super(config);
    this.network = network;
  }

  @Override
  protected String getOpenApiSpecResource() {
    return "openapi/web3signer-filecoin.yaml";
  }

  @Override
  protected Router populateRouter(final Context context) {
    final ArtifactSignerProvider signerProvider =
        loadSigners(config, context.getVertx(), context.getMetricsSystem());
    incSignerLoadCount(context.getMetricsSystem(), signerProvider.availableIdentifiers().size());

    return registerFilecoinJsonRpcRoute(
        context.getRouterFactory(), context.getMetricsSystem(), signerProvider);
  }

  private Router registerFilecoinJsonRpcRoute(
      final OpenAPI3RouterFactory routerFactory,
      final MetricsSystem metricsSystem,
      final ArtifactSignerProvider fcSigners) {

    final Router router = routerFactory.getRouter();

    final FcJsonRpcMetrics fcJsonRpcMetrics = new FcJsonRpcMetrics(metricsSystem);
    final FcJsonRpc fileCoinJsonRpc = new FcJsonRpc(fcSigners, fcJsonRpcMetrics);
    final ObjectMapper mapper = new ObjectMapper().registerModule(new FilecoinJsonRpcModule());
    final JsonRpcServer jsonRpcServer = JsonRpcServer.withMapper(mapper);

    router
        .post(FC_JSON_RPC_PATH)
        .handler(fcJsonRpcMetrics::incTotalFilecoinRequests)
        .handler(BodyHandler.create())
        .blockingHandler(
            routingContext -> {
              final String body = routingContext.getBodyAsString();
              final String jsonRpcResponse = jsonRpcServer.handle(body, fileCoinJsonRpc);
              routingContext.response().putHeader(CONTENT_TYPE, JSON_UTF_8).end(jsonRpcResponse);
            },
            false);

    return router;
  }

  private ArtifactSignerProvider loadSigners(
      final Config config, final Vertx vertx, final MetricsSystem metricsSystem) {
    final AzureKeyVaultSignerFactory azureFactory = new AzureKeyVaultSignerFactory();

    final AbstractArtifactSignerFactory blsArtifactSignerFactory =
        new BlsArtifactSignerFactory(
            vertx,
            config.getKeyConfigPath(),
            metricsSystem,
            keyPair -> new FcBlsArtifactSigner(keyPair, network));

    final AbstractArtifactSignerFactory secpArtifactSignerFactory =
        new Secp256k1ArtifactSignerFactory(
            vertx,
            config.getKeyConfigPath(),
            azureFactory,
            signer -> new FcSecpArtifactSigner(signer, network),
            false);

    final Collection<ArtifactSigner> signers =
        SignerLoader.load(
            config.getKeyConfigPath(),
            "yaml",
            new YamlSignerParser(List.of(blsArtifactSignerFactory, secpArtifactSignerFactory)));

    return DefaultArtifactSignerProvider.create(signers);
  }
}
