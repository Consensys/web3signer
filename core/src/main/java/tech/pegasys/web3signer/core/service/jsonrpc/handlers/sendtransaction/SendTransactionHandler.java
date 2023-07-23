/*
 * Copyright 2019 ConsenSys AG.
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
package tech.pegasys.web3signer.core.service.jsonrpc.handlers.sendtransaction;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static tech.pegasys.web3signer.core.service.jsonrpc.response.JsonRpcError.INVALID_PARAMS;
import static tech.pegasys.web3signer.core.service.jsonrpc.response.JsonRpcError.SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT;
import static tech.pegasys.web3signer.core.util.ResponseCodeSelector.jsonRPCErrorCode;
import static tech.pegasys.web3signer.signing.util.IdentifierUtils.normaliseIdentifier;

import tech.pegasys.web3signer.core.service.VertxRequestTransmitterFactory;
import tech.pegasys.web3signer.core.service.http.handlers.signing.SignerForIdentifier;
import tech.pegasys.web3signer.core.service.jsonrpc.JsonRpcRequest;
import tech.pegasys.web3signer.core.service.jsonrpc.JsonRpcRequestHandler;
import tech.pegasys.web3signer.core.service.jsonrpc.exceptions.JsonRpcException;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.sendtransaction.transaction.Transaction;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.sendtransaction.transaction.TransactionFactory;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.signing.TransactionSerializer;
import tech.pegasys.web3signer.signing.SecpArtifactSignature;

import io.vertx.core.json.DecodeException;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SendTransactionHandler implements JsonRpcRequestHandler {

  private static final Logger LOG = LogManager.getLogger();

  private final long chainId;
  private final TransactionFactory transactionFactory;
  private final VertxRequestTransmitterFactory vertxTransmitterFactory;

  private final SignerForIdentifier<SecpArtifactSignature> secpSigner;
  private static final int MAX_NONCE_RETRIES = 10;

  public SendTransactionHandler(
      final long chainId,
      final TransactionFactory transactionFactory,
      final VertxRequestTransmitterFactory vertxTransmitterFactory,
      final SignerForIdentifier<SecpArtifactSignature> secpSigner) {
    this.chainId = chainId;
    this.transactionFactory = transactionFactory;
    this.vertxTransmitterFactory = vertxTransmitterFactory;
    this.secpSigner = secpSigner;
  }

  @Override
  public void handle(final RoutingContext context, final JsonRpcRequest request) {
    LOG.debug("Transforming request {}, {}", request.getId(), request.getMethod());
    final Transaction transaction;
    try {
      transaction = transactionFactory.createTransaction(context, request);
    } catch (final NumberFormatException e) {
      LOG.debug("Parsing values failed for request: {}", request.getParams(), e);
      final JsonRpcException jsonRpcException = new JsonRpcException(INVALID_PARAMS);
      context.fail(jsonRPCErrorCode(jsonRpcException), jsonRpcException);
      return;
    } catch (final JsonRpcException e) {
      context.fail(jsonRPCErrorCode(e), e);
      return;
    } catch (final IllegalArgumentException | DecodeException e) {
      LOG.debug("JSON Deserialization failed for request: {}", request.getParams(), e);
      final JsonRpcException jsonRpcException = new JsonRpcException(INVALID_PARAMS);
      context.fail(jsonRPCErrorCode(jsonRpcException), jsonRpcException);
      return;
    }

    if (!secpSigner.isSignerAvailable(normaliseIdentifier(transaction.sender()))) {
      LOG.debug("From address ({}) does not match any available account", transaction.sender());
      context.fail(
          BAD_REQUEST.code(), new JsonRpcException(SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT));
      return;
    }

    sendTransaction(transaction, context, secpSigner, request);
  }

  private void sendTransaction(
      final Transaction transaction,
      final RoutingContext routingContext,
      final SignerForIdentifier<SecpArtifactSignature> secpSigner,
      final JsonRpcRequest request) {

    final TransactionSerializer transactionSerializer =
        new TransactionSerializer(secpSigner, chainId);

    final TransactionTransmitter transmitter =
        createTransactionTransmitter(transaction, transactionSerializer, routingContext, request);
    transmitter.send();
  }

  private TransactionTransmitter createTransactionTransmitter(
      final Transaction transaction,
      final TransactionSerializer transactionSerializer,
      final RoutingContext routingContext,
      final JsonRpcRequest request) {

    if (!transaction.isNonceUserSpecified()) {
      LOG.debug("Nonce not present in request {}", request.getId());
      return new RetryingTransactionTransmitter(
          transaction,
          transactionSerializer,
          vertxTransmitterFactory,
          new NonceTooLowRetryMechanism(MAX_NONCE_RETRIES),
          routingContext);
    } else {
      LOG.debug("Nonce supplied by client, forwarding request");
      return new TransactionTransmitter(
          transaction, transactionSerializer, vertxTransmitterFactory, routingContext);
    }
  }
}
