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

import tech.pegasys.teku.api.schema.BeaconBlock;
import tech.pegasys.teku.core.signatures.SigningRootUtil;
import tech.pegasys.teku.datastructures.state.ForkInfo;
import tech.pegasys.web3signer.core.metrics.SlashingProtectionMetrics;
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
import org.apache.tuweni.units.bigints.UInt64;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer.TimingContext;

public class Eth2SignForIdentifierHandler implements Handler<RoutingContext> {

  private static final Logger LOG = LogManager.getLogger();
  private final SignerForIdentifier<?> signerForIdentifier;
  private final HttpApiMetrics httpMetrics;
  private final SlashingProtectionMetrics slashingMetrics;
  private final Optional<SlashingProtection> slashingProtection;
  private final ObjectMapper objectMapper;

  public Eth2SignForIdentifierHandler(
      final SignerForIdentifier<?> signerForIdentifier,
      final HttpApiMetrics httpMetrics,
      final SlashingProtectionMetrics slashingMetrics,
      final Optional<SlashingProtection> slashingProtection,
      final ObjectMapper objectMapper) {
    this.signerForIdentifier = signerForIdentifier;
    this.httpMetrics = httpMetrics;
    this.slashingMetrics = slashingMetrics;
    this.slashingProtection = slashingProtection;
    this.objectMapper = objectMapper;
  }

  @Override
  public void handle(final RoutingContext routingContext) {
    try (final TimingContext ignored = httpMetrics.getSigningTimer().startTimer()) {
      LOG.debug("{} || {}", routingContext.normalisedPath(), routingContext.getBody());
      final RequestParameters params = routingContext.get("parsedParameters");
      final String identifier = params.pathParameter("identifier").toString();
      final Eth2SigningRequestBody eth2SigningRequestBody;
      try {
        eth2SigningRequestBody = getSigningRequest(params);
      } catch (final IllegalArgumentException | JsonProcessingException e) {
        handleInvalidRequest(routingContext, e);
        return;
      }

      final Bytes calculatedSigningRoot = signingRoot(eth2SigningRequestBody);
      checkArgument(
          eth2SigningRequestBody.getSigningRoot().equals(calculatedSigningRoot),
          "Signing root %s must match signing computed signing root %s from data",
          eth2SigningRequestBody.getSigningRoot(),
          calculatedSigningRoot);
      if (slashingProtection.isPresent()) {
        signerForIdentifier
            .sign(normaliseIdentifier(identifier), calculatedSigningRoot)
            .ifPresentOrElse(
                signature -> {
                  try {
                    if (maySign(
                        Bytes.fromHexString(identifier),
                        calculatedSigningRoot,
                        eth2SigningRequestBody)) {
                      slashingMetrics.incrementSigningsPermitted(
                          eth2SigningRequestBody.getType().name());
                      respondWithSignature(routingContext, signature);
                    } else {
                      slashingMetrics.incrementSigningsPrevented(
                          eth2SigningRequestBody.getType().name());
                      LOG.debug("Signing not allowed due to slashing protection rules failing");
                      routingContext.fail(403);
                    }
                  } catch (final IllegalArgumentException e) {
                    handleInvalidRequest(routingContext, e);
                  }
                },
                () -> {
                  httpMetrics.getMissingSignerCounter().inc();
                  routingContext.fail(404);
                });
      } else {
        // TODO handle signingRoot not existing
        signerForIdentifier
            .sign(normaliseIdentifier(identifier), eth2SigningRequestBody.getSigningRoot())
            .ifPresentOrElse(
                signature -> respondWithSignature(routingContext, signature),
                () -> {
                  httpMetrics.getMissingSignerCounter().inc();
                  routingContext.fail(404);
                });
      }
    }
  }

  private void handleInvalidRequest(final RoutingContext routingContext, final Exception e) {
    httpMetrics.getMalformedRequestCounter().inc();
    LOG.debug("Invalid signing request - " + routingContext.getBodyAsString(), e);
    routingContext.fail(400);
  }

  private boolean maySign(
      final Bytes publicKey,
      final Bytes signingRoot,
      final Eth2SigningRequestBody eth2SigningRequestBody) {
    switch (eth2SigningRequestBody.getType()) {
      case BLOCK:
        final BeaconBlock beaconBlock = eth2SigningRequestBody.getBlock();
        final UInt64 blockSlot = UInt64.valueOf(beaconBlock.slot.bigIntegerValue());
        return slashingProtection.get().maySignBlock(publicKey, signingRoot, blockSlot);
      case ATTESTATION:
        return slashingProtection
            .get()
            .maySignAttestation(
                publicKey,
                signingRoot,
                toUInt64(eth2SigningRequestBody.getAttestation().source.epoch),
                toUInt64(eth2SigningRequestBody.getAttestation().target.epoch));
      default:
        return true;
    }
  }

  private Bytes signingRoot(final Eth2SigningRequestBody eth2SigningRequestBody) {
    switch (eth2SigningRequestBody.getType()) {
      case BLOCK:
        final BeaconBlock beaconBlock = eth2SigningRequestBody.getBlock();
        return SigningRootUtil.signingRootForSignBlock(
            beaconBlock.asInternalBeaconBlock(),
            new ForkInfo(
                eth2SigningRequestBody.getFork().asInternalFork(),
                eth2SigningRequestBody.getGenesisValidatorsRoot()));
      case ATTESTATION:
        return SigningRootUtil.signingRootForSignAttestationData(
            eth2SigningRequestBody.getAttestation().asInternalAttestationData(),
            new ForkInfo(
                eth2SigningRequestBody.getFork().asInternalFork(),
                eth2SigningRequestBody.getGenesisValidatorsRoot()));
      case AGGREGATE_AND_PROOF:
        return SigningRootUtil.signingRootForSignAggregateAndProof(
            eth2SigningRequestBody.getAggregateAndProof().asInternalAggregateAndProof(),
            new ForkInfo(
                eth2SigningRequestBody.getFork().asInternalFork(),
                eth2SigningRequestBody.getGenesisValidatorsRoot()));
      case AGGREGATION_SLOT:
        return SigningRootUtil.signingRootForSignAggregationSlot(
            eth2SigningRequestBody.getAggregationSlot().getSlot(),
            new ForkInfo(
                eth2SigningRequestBody.getFork().asInternalFork(),
                eth2SigningRequestBody.getGenesisValidatorsRoot()));
      case RANDAO_REVEAL:
        return SigningRootUtil.signingRootForRandaoReveal(
            eth2SigningRequestBody.getRandaoReveal().getEpoch(),
            new ForkInfo(
                eth2SigningRequestBody.getFork().asInternalFork(),
                eth2SigningRequestBody.getGenesisValidatorsRoot()));
      case VOLUNTARY_EXIT:
        return SigningRootUtil.signingRootForSignVoluntaryExit(
            eth2SigningRequestBody.getVoluntaryExit().asInternalVoluntaryExit(),
            new ForkInfo(
                eth2SigningRequestBody.getFork().asInternalFork(),
                eth2SigningRequestBody.getGenesisValidatorsRoot()));
      default:
        // TODO add support for deposit
        throw new IllegalStateException("Signing root unimplemented for type");
    }
  }

  private UInt64 toUInt64(final tech.pegasys.teku.infrastructure.unsigned.UInt64 uInt64) {
    return UInt64.valueOf(uInt64.bigIntegerValue());
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
