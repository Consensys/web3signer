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
package tech.pegasys.web3signer.core.service.http.handlers.signing;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static tech.pegasys.web3signer.core.service.http.handlers.ContentTypes.TEXT_PLAIN_UTF_8;
import static tech.pegasys.web3signer.core.util.IdentifierUtils.normaliseIdentifier;

import tech.pegasys.web3signer.core.service.http.SignRequest;
import tech.pegasys.web3signer.core.service.http.SigningJsonRpcModule;
import tech.pegasys.web3signer.core.service.http.metrics.HttpApiMetrics;
import tech.pegasys.web3signer.slashingprotection.SlashingProtection;

import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.RequestParameters;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer.TimingContext;

@SuppressWarnings("UnusedVariable")
public class SignForIdentifierHandler implements Handler<RoutingContext> {

  private static final Logger LOG = LogManager.getLogger();
  private final SignerForIdentifier<?> signerForIdentifier;
  private final HttpApiMetrics metrics;
  private final Optional<SlashingProtection> slashingProtection;
  private final ObjectMapper objectMapper;

  public SignForIdentifierHandler(
      final SignerForIdentifier<?> signerForIdentifier,
      final HttpApiMetrics metrics,
      final SlashingProtection slashingProtection) {
    this.signerForIdentifier = signerForIdentifier;
    this.metrics = metrics;
    this.slashingProtection = Optional.ofNullable(slashingProtection);
    this.objectMapper =
        new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
            .registerModule(new SigningJsonRpcModule());
  }

  @Override
  public void handle(final RoutingContext routingContext) {
    try (final TimingContext ignored = metrics.getSigningTimer().startTimer()) {
      final RequestParameters params = routingContext.get("parsedParameters");
      final String identifier = params.pathParameter("identifier").toString();
      final SignRequest signRequest;
      try {
        signRequest = getSigningRequest(params);
      } catch (final IllegalArgumentException | JsonProcessingException e) {
        metrics.getMalformedRequestCounter().inc();
        routingContext.fail(400);
        return;
      }

      signerForIdentifier
          .sign(normaliseIdentifier(identifier), signRequest.getSigningRoot())
          .ifPresentOrElse(
              signature -> {
                if (slashingProtection.isPresent()) {
                  if (maySign(identifier, signRequest)) {
                    respondWithSignature(routingContext, signature);
                  }
                } else {
                  respondWithSignature(routingContext, signature);
                }
              },
              () -> {
                LOG.trace("Identifier not found {}", identifier);
                metrics.getMissingSignerCounter().inc();
                routingContext.fail(404);
              });
    }
  }

  private boolean maySign(final String publicKey, final SignRequest signRequest) {
    switch (signRequest.getType()) {
      case "block":
        return slashingProtection
            .get()
            .maySignBlock(publicKey, signRequest.getSigningRoot(), signRequest.getSlot());
      case "attestation":
        return slashingProtection
            .get()
            .maySignAttestation(
                publicKey,
                signRequest.getSigningRoot(),
                signRequest.getSourceEpoch(),
                signRequest.getTargetEpoch());
      default:
        return true;
    }
  }

  private void respondWithSignature(final RoutingContext routingContext, final String signature) {
    routingContext.response().putHeader(CONTENT_TYPE, TEXT_PLAIN_UTF_8).end(signature);
  }

  private SignRequest getSigningRequest(final RequestParameters params)
      throws JsonProcessingException {
    final String body = params.body().toString();
    return objectMapper.readValue(body, SignRequest.class);
  }
}
