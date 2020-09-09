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
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package tech.pegasys.web3signer.core;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static tech.pegasys.web3signer.core.service.http.handlers.ContentTypes.JSON_UTF_8;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.arteam.simplejsonrpc.server.JsonRpcServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import tech.pegasys.web3signer.core.config.Config;
import tech.pegasys.web3signer.core.service.jsonrpc.FcJsonRpc;
import tech.pegasys.web3signer.core.service.jsonrpc.FcJsonRpcMetrics;
import tech.pegasys.web3signer.core.service.jsonrpc.FilecoinJsonRpcModule;
import tech.pegasys.web3signer.core.signing.ArtifactSignerProvider;

public class FilecoinRunner extends Runner {

  private static final String FC_JSON_RPC_PATH = JSON_RPC_PATH + "/filecoin";

  public FilecoinRunner(final Config config) {
    super(config);
  }

  @Override
  protected void createHandler(final Context context) {
    registerFilecoinJsonRpcRoute(
        context.getRouterFactory().getRouter(),
        context.getMetricsSystem(),
        context.getSigners().getFcSignerProvider()
    );
  }

  private void registerFilecoinJsonRpcRoute(
      final Router router,
      final MetricsSystem metricsSystem,
      final ArtifactSignerProvider fcSigners) {

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
  }
}
