/*
 * Copyright 2024 ConsenSys AG.
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
package tech.pegasys.web3signer.core.routes.eth1;

import tech.pegasys.web3signer.core.Context;
import tech.pegasys.web3signer.core.Runner;
import tech.pegasys.web3signer.core.WebClientOptionsFactory;
import tech.pegasys.web3signer.core.config.Eth1Config;
import tech.pegasys.web3signer.core.routes.Web3SignerRoute;
import tech.pegasys.web3signer.core.service.DownstreamPathCalculator;
import tech.pegasys.web3signer.core.service.VertxRequestTransmitter;
import tech.pegasys.web3signer.core.service.VertxRequestTransmitterFactory;
import tech.pegasys.web3signer.core.service.http.handlers.signing.SignerForIdentifier;
import tech.pegasys.web3signer.core.service.jsonrpc.Eth1JsonDecoderFactory;
import tech.pegasys.web3signer.core.service.jsonrpc.JsonDecoder;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.Eth1AccountsHandler;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.HttpResponseFactory;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.JsonRpcErrorHandler;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.JsonRpcHandler;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.PassThroughHandler;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.RequestMapper;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.internalresponse.EthSignResultProvider;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.internalresponse.EthSignTransactionResultProvider;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.internalresponse.EthSignTypedDataResultProvider;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.internalresponse.InternalResponseHandler;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.sendtransaction.SendTransactionHandler;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.sendtransaction.transaction.TransactionFactory;
import tech.pegasys.web3signer.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.signing.config.SecpArtifactSignerProviderAdapter;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;

public class JsonRpcRoute implements Web3SignerRoute {
  private static final String ROOT_PATH = "/";
  private static final JsonDecoder JSON_DECODER = Eth1JsonDecoderFactory.create();
  private static final HttpResponseFactory HTTP_RESPONSE_FACTORY = new HttpResponseFactory();

  private final Context context;
  private final VertxRequestTransmitterFactory transmitterFactory;
  private final RequestMapper requestMapper;

  public JsonRpcRoute(final Context context, final Eth1Config eth1Config) {
    this.context = context;

    // we need signerProvider which is an instance of SecpArtifactSignerProviderAdapter which uses
    // eth1 address as identifier
    final ArtifactSignerProvider signerProvider =
        context.getArtifactSignerProviders().stream()
            .filter(provider -> provider instanceof SecpArtifactSignerProviderAdapter)
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No SecpArtifactSignerProviderAdapter found in Context for eth1 mode"));

    // use same instance of downstreamHttpClient and path calculator for all requests
    final HttpClient downstreamHttpClient =
        createDownstreamHttpClient(eth1Config, context.getVertx());
    final DownstreamPathCalculator downstreamPathCalculator =
        new DownstreamPathCalculator(eth1Config.getDownstreamHttpPath());

    transmitterFactory =
        responseBodyHandler ->
            new VertxRequestTransmitter(
                context.getVertx(),
                downstreamHttpClient,
                eth1Config.getDownstreamHttpRequestTimeout(),
                downstreamPathCalculator,
                responseBodyHandler);

    requestMapper =
        createRequestMapper(transmitterFactory, signerProvider, eth1Config.getChainId().id());
  }

  @Override
  public void register() {
    context
        .getRouter()
        .route(HttpMethod.POST, ROOT_PATH)
        .produces(Runner.JSON)
        .handler(ResponseContentTypeHandler.create())
        .handler(BodyHandler.create())
        .failureHandler(new JsonRpcErrorHandler(new HttpResponseFactory()))
        .blockingHandler(
            new JsonRpcHandler(HTTP_RESPONSE_FACTORY, requestMapper, JSON_DECODER), false);

    // proxy everything else to Besu using passThrough handler
    context
        .getRouter()
        .route()
        .handler(BodyHandler.create())
        .handler(new PassThroughHandler(transmitterFactory, JSON_DECODER));
  }

  private static HttpClient createDownstreamHttpClient(
      final Eth1Config eth1Config, final Vertx vertx) {
    final WebClientOptions webClientOptions =
        new WebClientOptionsFactory().createWebClientOptions(eth1Config);
    return vertx.createHttpClient(webClientOptions);
  }

  private static RequestMapper createRequestMapper(
      final VertxRequestTransmitterFactory transmitterFactory,
      final ArtifactSignerProvider signerProviderMappedToEth1Address,
      final long chainId) {
    final PassThroughHandler defaultHandler =
        new PassThroughHandler(transmitterFactory, JSON_DECODER);
    final SignerForIdentifier secpSigner =
        new SignerForIdentifier(signerProviderMappedToEth1Address);
    final TransactionFactory transactionFactory =
        new TransactionFactory(chainId, JSON_DECODER, transmitterFactory);
    final SendTransactionHandler sendTransactionHandler =
        new SendTransactionHandler(chainId, transactionFactory, transmitterFactory, secpSigner);

    final RequestMapper requestMapper = new RequestMapper(defaultHandler);
    requestMapper.addHandler(
        "eth_accounts",
        new InternalResponseHandler<>(
            HTTP_RESPONSE_FACTORY,
            new Eth1AccountsHandler(signerProviderMappedToEth1Address::availableIdentifiers)));
    requestMapper.addHandler(
        "eth_sign",
        new InternalResponseHandler<>(
            HTTP_RESPONSE_FACTORY, new EthSignResultProvider(secpSigner)));
    requestMapper.addHandler(
        "eth_signTypedData",
        new InternalResponseHandler<>(
            HTTP_RESPONSE_FACTORY, new EthSignTypedDataResultProvider(secpSigner)));
    requestMapper.addHandler(
        "eth_signTransaction",
        new InternalResponseHandler<>(
            HTTP_RESPONSE_FACTORY,
            new EthSignTransactionResultProvider(chainId, secpSigner, JSON_DECODER)));
    requestMapper.addHandler("eth_sendTransaction", sendTransactionHandler);
    requestMapper.addHandler(
        "health_status", new InternalResponseHandler<>(HTTP_RESPONSE_FACTORY, request -> "OK"));

    return requestMapper;
  }
}
