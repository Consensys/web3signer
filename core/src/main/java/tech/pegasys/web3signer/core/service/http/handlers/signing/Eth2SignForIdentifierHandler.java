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

import static com.google.common.base.Preconditions.checkArgument;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static tech.pegasys.web3signer.core.service.http.handlers.ContentTypes.TEXT_PLAIN_UTF_8;
import static tech.pegasys.web3signer.core.util.IdentifierUtils.normaliseIdentifier;

import io.netty.handler.logging.LogLevel;
import org.apache.logging.log4j.Level;
import tech.pegasys.web3signer.core.service.http.Eth2SigningRequestBody;
import tech.pegasys.web3signer.core.service.http.metrics.HttpApiMetrics;
import tech.pegasys.web3signer.slashingprotection.SlashingProtection;

import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.RequestParameters;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer.TimingContext;

public class Eth2SignForIdentifierHandler implements Handler<RoutingContext> {

  private static final Logger LOG = LogManager.getLogger();
  private final SignerForIdentifier<?> signerForIdentifier;
  private final HttpApiMetrics metrics;
  private final Optional<SlashingProtection> slashingProtection;
  private final ObjectMapper objectMapper;

  public Eth2SignForIdentifierHandler(
      final SignerForIdentifier<?> signerForIdentifier,
      final HttpApiMetrics metrics,
      final Optional<SlashingProtection> slashingProtection,
      final ObjectMapper objectMapper) {
    this.signerForIdentifier = signerForIdentifier;
    this.metrics = metrics;
    this.slashingProtection = slashingProtection;
    this.objectMapper = objectMapper;
  }

  @Override
  public void handle(final RoutingContext routingContext) {
    try (final TimingContext ignored = metrics.startHttpOperation()) {
      if (!LOG.getLevel().equals(Level.TRACE)) {
        LOG.debug("Received on {}", routingContext.normalisedPath());
      } else {
        LOG.trace("{} || {}", routingContext.normalisedPath(),
            routingContext.getBody());
      }
      final RequestParameters params = routingContext.get("parsedParameters");
      final String identifier = params.pathParameter("identifier").toString();
      final Eth2SigningRequestBody eth2SigningRequestBody;
      try {
        eth2SigningRequestBody = getSigningRequest(params);
      } catch (final IllegalArgumentException | JsonProcessingException e) {
        handleInvalidRequest(routingContext, e);
        return;
      }

      signerForIdentifier
          .sign(normaliseIdentifier(identifier), eth2SigningRequestBody.getSigningRoot())
          .ifPresentOrElse(
              signature -> {
                if (slashingProtection.isPresent()) {
                  try {
                    if (maySign(Bytes.fromHexString(identifier), eth2SigningRequestBody)) {
                      respondWithSignature(routingContext, signature);
                    } else {
                      LOG.debug("Signing not allowed due to slashing protection rules failing");
                      routingContext.fail(403);
                    }
                  } catch (final IllegalArgumentException e) {
                    handleInvalidRequest(routingContext, e);
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

  private void handleInvalidRequest(final RoutingContext routingContext, final Exception e) {
    metrics.getMalformedRequestCounter().inc();
    LOG.debug("Invalid signing request", e);
    routingContext.fail(400);
  }

  private boolean maySign(
      final Bytes publicKey, final Eth2SigningRequestBody eth2SigningRequestBody) {
    checkArgument(eth2SigningRequestBody.getType() != null, "Type must be specified");
    switch (eth2SigningRequestBody.getType()) {
      case BLOCK:
        checkArgument(eth2SigningRequestBody.getSlot() != null, "Slot must be specified");
        return slashingProtection
            .get()
            .maySignBlock(
                publicKey,
                eth2SigningRequestBody.getSigningRoot(),
                eth2SigningRequestBody.getSlot());
      case ATTESTATION:
        checkArgument(
            eth2SigningRequestBody.getSourceEpoch() != null, "Source epoch must be specified");
        checkArgument(
            eth2SigningRequestBody.getTargetEpoch() != null, "Target epoch must be specified");
        return slashingProtection
            .get()
            .maySignAttestation(
                publicKey,
                eth2SigningRequestBody.getSigningRoot(),
                eth2SigningRequestBody.getSourceEpoch(),
                eth2SigningRequestBody.getTargetEpoch());
      default:
        return true;
    }
  }

  private void respondWithSignature(final RoutingContext routingContext, final String signature) {
    routingContext.response().putHeader(CONTENT_TYPE, TEXT_PLAIN_UTF_8).end(signature);
  }

  private Eth2SigningRequestBody getSigningRequest(final RequestParameters params)
      throws JsonProcessingException {
    final String body = params.body().toString();
    return objectMapper.readValue(body, Eth2SigningRequestBody.class);
  }
}
