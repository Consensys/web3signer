/*
 * Copyright 2021 ConsenSys AG.
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
package tech.pegasys.web3signer.core.service.http.handlers.keymanager.delete;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static tech.pegasys.web3signer.core.service.http.handlers.ContentTypes.JSON_UTF_8;

import tech.pegasys.web3signer.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.signing.ValidatorManager;
import tech.pegasys.web3signer.slashingprotection.SlashingProtection;

import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Handler;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DeleteKeystoresHandler implements Handler<RoutingContext> {

  private static final Logger LOG = LogManager.getLogger();
  public static final int SUCCESS = 200;
  public static final int BAD_REQUEST = 400;
  public static final int SERVER_ERROR = 500;

  private final ObjectMapper objectMapper;
  private final DeleteKeystoresProcessor processor;

  public DeleteKeystoresHandler(
      final ObjectMapper objectMapper,
      final Optional<SlashingProtection> slashingProtection,
      final ArtifactSignerProvider signerProvider,
      final ValidatorManager validatorManager) {
    this.objectMapper = objectMapper;
    processor = new DeleteKeystoresProcessor(slashingProtection, signerProvider, validatorManager);
  }

  @Override
  public void handle(final RoutingContext context) {
    // API spec - https://github.com/ethereum/keymanager-APIs/tree/master/flows#delete
    final DeleteKeystoresRequestBody parsedBody;
    try {
      parsedBody = parseRequestBody(context.body());
    } catch (final IllegalArgumentException | JsonProcessingException e) {
      handleInvalidRequest(context, e);
      return;
    }

    final DeleteKeystoresResponse response = processor.process(parsedBody);

    try {
      context
          .response()
          .putHeader(CONTENT_TYPE, JSON_UTF_8)
          .setStatusCode(SUCCESS)
          .end(objectMapper.writeValueAsString(response));
    } catch (Exception e) {
      context.fail(SERVER_ERROR, e);
    }
  }

  private DeleteKeystoresRequestBody parseRequestBody(final RequestBody requestBody)
      throws JsonProcessingException {
    final String body = requestBody.asString();
    return objectMapper.readValue(body, DeleteKeystoresRequestBody.class);
  }

  private void handleInvalidRequest(final RoutingContext routingContext, final Exception e) {
    LOG.debug("Invalid delete keystores request - " + routingContext.body().asString(), e);
    routingContext.fail(BAD_REQUEST);
  }
}
