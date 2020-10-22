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
package tech.pegasys.web3signer.core.service.http.handlers.signing.eth2;

import static com.google.common.base.Preconditions.checkArgument;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static tech.pegasys.teku.datastructures.util.BeaconStateUtil.compute_domain;
import static tech.pegasys.teku.datastructures.util.BeaconStateUtil.compute_signing_root;
import static tech.pegasys.teku.util.config.Constants.DOMAIN_DEPOSIT;
import static tech.pegasys.web3signer.core.service.http.handlers.ContentTypes.TEXT_PLAIN_UTF_8;
import static tech.pegasys.web3signer.core.util.IdentifierUtils.normaliseIdentifier;

import tech.pegasys.teku.api.schema.AttestationData;
import tech.pegasys.teku.api.schema.BeaconBlock;
import tech.pegasys.teku.core.signatures.SigningRootUtil;
import tech.pegasys.web3signer.core.metrics.SlashingProtectionMetrics;
import tech.pegasys.web3signer.core.service.http.handlers.signing.SignerForIdentifier;
import tech.pegasys.web3signer.core.service.http.metrics.HttpApiMetrics;
import tech.pegasys.web3signer.slashingprotection.SlashingProtection;

import java.util.Optional;
import java.util.function.Consumer;

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

      final Bytes signingRoot = computeSigningRoot(eth2SigningRequestBody);
      if (eth2SigningRequestBody.getSigningRoot() != null) {
        checkArgument(
            eth2SigningRequestBody.getSigningRoot().equals(signingRoot),
            "Signing root %s must match signing computed signing root %s from data",
            eth2SigningRequestBody.getSigningRoot(),
            signingRoot);
      }

      final String normalisedIdentifier = normaliseIdentifier(identifier);
      if (slashingProtection.isPresent()) {
        handleSigning(
            routingContext,
            signingRoot,
            normalisedIdentifier,
            signature ->
                signWithSlashingProtection(
                    routingContext, identifier, eth2SigningRequestBody, signingRoot, signature));
      } else {
        handleSigning(
            routingContext,
            signingRoot,
            normalisedIdentifier,
            signature -> respondWithSignature(routingContext, signature));
      }
    }
  }

  private void handleSigning(
      final RoutingContext routingContext,
      final Bytes signingRoot,
      final String normalisedIdentifier,
      final Consumer<String> signatureConsumer) {
    signerForIdentifier
        .sign(normalisedIdentifier, signingRoot)
        .ifPresentOrElse(
            signatureConsumer,
            () -> {
              httpMetrics.getMissingSignerCounter().inc();
              routingContext.fail(404);
            });
  }

  private void signWithSlashingProtection(
      final RoutingContext routingContext,
      final String identifier,
      final Eth2SigningRequestBody eth2SigningRequestBody,
      final Bytes signingRoot,
      final String signature) {
    try {
      if (maySign(Bytes.fromHexString(identifier), signingRoot, eth2SigningRequestBody)) {
        slashingMetrics.incrementSigningsPermitted();
        respondWithSignature(routingContext, signature);
      } else {
        slashingMetrics.incrementSigningsPrevented();
        LOG.debug("Signing not allowed due to slashing protection rules failing");
        routingContext.fail(403);
      }
    } catch (final IllegalArgumentException e) {
      handleInvalidRequest(routingContext, e);
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
        final AttestationData attestation = eth2SigningRequestBody.getAttestation();
        return slashingProtection
            .get()
            .maySignAttestation(
                publicKey,
                signingRoot,
                toUInt64(attestation.source.epoch),
                toUInt64(attestation.target.epoch));
      default:
        return true;
    }
  }

  private Bytes computeSigningRoot(final Eth2SigningRequestBody body) {
    switch (body.getType()) {
      case BLOCK:
        checkArgument(body.getBlock() != null, "block must be specified");
        return SigningRootUtil.signingRootForSignBlock(
            body.getBlock().asInternalBeaconBlock(), body.getForkInfo().asInternalForkInfo());
      case ATTESTATION:
        checkArgument(body.getAttestation() != null, "attestation must be specified");
        return SigningRootUtil.signingRootForSignAttestationData(
            body.getAttestation().asInternalAttestationData(),
            body.getForkInfo().asInternalForkInfo());
      case AGGREGATE_AND_PROOF:
        checkArgument(body.getAggregateAndProof() != null, "aggregateAndProof must be specified");
        return SigningRootUtil.signingRootForSignAggregateAndProof(
            body.getAggregateAndProof().asInternalAggregateAndProof(),
            body.getForkInfo().asInternalForkInfo());
      case AGGREGATION_SLOT:
        checkArgument(body.getAggregationSlot() != null, "aggregationSlot must be specified");
        return SigningRootUtil.signingRootForSignAggregationSlot(
            body.getAggregationSlot().getSlot(), body.getForkInfo().asInternalForkInfo());
      case RANDAO_REVEAL:
        checkArgument(body.getRandaoReveal() != null, "randaoReveal must be specified");
        return SigningRootUtil.signingRootForRandaoReveal(
            body.getRandaoReveal().getEpoch(), body.getForkInfo().asInternalForkInfo());
      case VOLUNTARY_EXIT:
        checkArgument(body.getVoluntaryExit() != null, "voluntaryExit must be specified");
        return SigningRootUtil.signingRootForSignVoluntaryExit(
            body.getVoluntaryExit().asInternalVoluntaryExit(),
            body.getForkInfo().asInternalForkInfo());
      case DEPOSIT:
        checkArgument(body.getDeposit() != null, "deposit must be specified");
        return compute_signing_root(
            body.getDeposit().asInternalDepositMessage(), compute_domain(DOMAIN_DEPOSIT));
      default:
        throw new IllegalStateException("Signing root unimplemented for type " + body.getType());
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
