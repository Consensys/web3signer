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
import static tech.pegasys.web3signer.core.service.http.handlers.ContentTypes.JSON_UTF_8;
import static tech.pegasys.web3signer.core.service.http.handlers.ContentTypes.TEXT_PLAIN_UTF_8;
import static tech.pegasys.web3signer.core.util.DepositSigningRootUtil.computeDomain;
import static tech.pegasys.web3signer.signing.util.IdentifierUtils.normaliseIdentifier;

import tech.pegasys.teku.api.schema.AttestationData;
import tech.pegasys.teku.api.schema.altair.ContributionAndProof;
import tech.pegasys.teku.api.schema.altair.SyncCommitteeContribution;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.constants.Domain;
import tech.pegasys.teku.spec.datastructures.operations.versions.altair.SyncAggregatorSelectionDataSchema;
import tech.pegasys.teku.spec.logic.common.util.SyncCommitteeUtil;
import tech.pegasys.teku.spec.signatures.SigningRootUtil;
import tech.pegasys.web3signer.core.metrics.SlashingProtectionMetrics;
import tech.pegasys.web3signer.core.service.http.ArtifactType;
import tech.pegasys.web3signer.core.service.http.handlers.signing.SignerForIdentifier;
import tech.pegasys.web3signer.core.service.http.metrics.HttpApiMetrics;
import tech.pegasys.web3signer.core.util.DepositSigningRootUtil;
import tech.pegasys.web3signer.slashingprotection.SlashingProtection;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.MIMEHeader;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt64;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer.TimingContext;

public class Eth2SignForIdentifierHandler implements Handler<RoutingContext> {

  private static final Logger LOG = LogManager.getLogger();
  private final SignerForIdentifier<?> signerForIdentifier;
  private final HttpApiMetrics httpMetrics;
  private final SlashingProtectionMetrics slashingMetrics;
  private final Optional<SlashingProtection> slashingProtection;
  private final ObjectMapper objectMapper;
  private final Spec eth2Spec;
  private final SigningRootUtil signingRootUtil;

  public static final int NOT_FOUND = 404;
  public static final int BAD_REQUEST = 400;
  public static final int SLASHING_PROTECTION_ENFORCED = 412;

  public Eth2SignForIdentifierHandler(
      final SignerForIdentifier<?> signerForIdentifier,
      final HttpApiMetrics httpMetrics,
      final SlashingProtectionMetrics slashingMetrics,
      final Optional<SlashingProtection> slashingProtection,
      final ObjectMapper objectMapper,
      final Spec eth2Spec) {
    this.signerForIdentifier = signerForIdentifier;
    this.httpMetrics = httpMetrics;
    this.slashingMetrics = slashingMetrics;
    this.slashingProtection = slashingProtection;
    this.objectMapper = objectMapper;
    this.eth2Spec = eth2Spec;
    this.signingRootUtil = new SigningRootUtil(eth2Spec);
  }

  @Override
  public void handle(final RoutingContext routingContext) {
    try (final TimingContext ignored = httpMetrics.getSigningTimer().startTimer()) {
      LOG.trace("{} || {}", routingContext.normalizedPath(), routingContext.getBody());
      final RequestParameters params = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
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
              routingContext.fail(NOT_FOUND);
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
        routingContext.fail(SLASHING_PROTECTION_ENFORCED);
      }
    } catch (final IllegalArgumentException e) {
      handleInvalidRequest(routingContext, e);
    }
  }

  private void handleInvalidRequest(final RoutingContext routingContext, final Exception e) {
    httpMetrics.getMalformedRequestCounter().inc();
    LOG.debug("Invalid signing request - " + routingContext.getBodyAsString(), e);
    routingContext.fail(BAD_REQUEST);
  }

  private boolean maySign(
      final Bytes publicKey,
      final Bytes signingRoot,
      final Eth2SigningRequestBody eth2SigningRequestBody) {
    final ForkInfo forkInfo = eth2SigningRequestBody.getForkInfo();
    switch (eth2SigningRequestBody.getType()) {
      case BLOCK:
      case BLOCK_V2:
        try (final TimingContext ignored =
            slashingMetrics.getDatabaseTimer().labels("block").startTimer()) {
          return maySignBlock(
              publicKey, signingRoot, getBlockSlot(eth2SigningRequestBody), forkInfo);
        }
      case ATTESTATION:
        final AttestationData attestation = eth2SigningRequestBody.getAttestation();
        try (final TimingContext ignored =
            slashingMetrics.getDatabaseTimer().labels("attestation").startTimer()) {
          return slashingProtection
              .get()
              .maySignAttestation(
                  publicKey,
                  signingRoot,
                  toUInt64(attestation.source.epoch),
                  toUInt64(attestation.target.epoch),
                  forkInfo.getGenesisValidatorsRoot());
        }
      default:
        return true;
    }
  }

  private UInt64 getBlockSlot(final Eth2SigningRequestBody eth2SigningRequestBody) {
    final UInt64 blockSlot;
    if (eth2SigningRequestBody.getType() == ArtifactType.BLOCK) {
      blockSlot = UInt64.valueOf(eth2SigningRequestBody.getBlock().slot.bigIntegerValue());
    } else {
      final BlockRequest blockRequest = eth2SigningRequestBody.getBlockRequest();
      switch (blockRequest.getVersion()) {
        case PHASE0:
        case ALTAIR:
          blockSlot = UInt64.valueOf(blockRequest.getBeaconBlock().slot.bigIntegerValue());
          break;
        case BELLATRIX:
        default:
          blockSlot = UInt64.valueOf(blockRequest.getBeaconBlockHeader().slot.bigIntegerValue());
          break;
      }
    }

    return blockSlot;
  }

  private boolean maySignBlock(
      final Bytes publicKey,
      final Bytes signingRoot,
      final UInt64 blockSlot,
      final ForkInfo forkInfo) {
    return slashingProtection
        .get()
        .maySignBlock(publicKey, signingRoot, blockSlot, forkInfo.getGenesisValidatorsRoot());
  }

  private Bytes computeSigningRoot(final Eth2SigningRequestBody body) {
    switch (body.getType()) {
      case BLOCK:
        checkArgument(body.getBlock() != null, "block must be specified");
        return signingRootUtil.signingRootForSignBlock(
            body.getBlock().asInternalBeaconBlock(eth2Spec),
            body.getForkInfo().asInternalForkInfo());
      case BLOCK_V2:
        checkArgument(body.getBlockRequest() != null, "beacon_block must be specified");
        final Bytes blockV2SigningRoot;
        switch (body.getBlockRequest().getVersion()) {
          case PHASE0:
          case ALTAIR:
            blockV2SigningRoot =
                signingRootUtil.signingRootForSignBlock(
                    body.getBlockRequest().getBeaconBlock().asInternalBeaconBlock(eth2Spec),
                    body.getForkInfo().asInternalForkInfo());
            break;
          case BELLATRIX:
          default:
            blockV2SigningRoot =
                signingRootUtil.signingRootForSignBlockHeader(
                    body.getBlockRequest().getBeaconBlockHeader().asInternalBeaconBlockHeader(),
                    body.getForkInfo().asInternalForkInfo());
            break;
        }

        return blockV2SigningRoot;
      case ATTESTATION:
        checkArgument(body.getAttestation() != null, "attestation must be specified");
        return signingRootUtil.signingRootForSignAttestationData(
            body.getAttestation().asInternalAttestationData(),
            body.getForkInfo().asInternalForkInfo());
      case AGGREGATE_AND_PROOF:
        checkArgument(body.getAggregateAndProof() != null, "aggregateAndProof must be specified");
        return signingRootUtil.signingRootForSignAggregateAndProof(
            body.getAggregateAndProof().asInternalAggregateAndProof(eth2Spec),
            body.getForkInfo().asInternalForkInfo());
      case AGGREGATION_SLOT:
        checkArgument(body.getAggregationSlot() != null, "aggregationSlot must be specified");
        return signingRootUtil.signingRootForSignAggregationSlot(
            body.getAggregationSlot().getSlot(), body.getForkInfo().asInternalForkInfo());
      case RANDAO_REVEAL:
        checkArgument(body.getRandaoReveal() != null, "randaoReveal must be specified");
        return signingRootUtil.signingRootForRandaoReveal(
            body.getRandaoReveal().getEpoch(), body.getForkInfo().asInternalForkInfo());
      case VOLUNTARY_EXIT:
        checkArgument(body.getVoluntaryExit() != null, "voluntaryExit must be specified");
        return signingRootUtil.signingRootForSignVoluntaryExit(
            body.getVoluntaryExit().asInternalVoluntaryExit(),
            body.getForkInfo().asInternalForkInfo());
      case DEPOSIT:
        checkArgument(body.getDeposit() != null, "deposit must be specified");
        final Bytes32 depositDomain =
            computeDomain(Domain.DEPOSIT, body.getDeposit().getGenesisForkVersion(), Bytes32.ZERO);
        return DepositSigningRootUtil.computeSigningRoot(
            body.getDeposit().asInternalDepositMessage(), depositDomain);
      case SYNC_COMMITTEE_MESSAGE:
        final SyncCommitteeMessage syncCommitteeMessage = body.getSyncCommitteeMessage();
        checkArgument(syncCommitteeMessage != null, "SyncCommitteeMessage must be specified");
        return signingRootFromSyncCommitteeUtils(
            syncCommitteeMessage.getSlot(),
            utils ->
                utils.getSyncCommitteeMessageSigningRoot(
                    syncCommitteeMessage.getBeaconBlockRoot(),
                    eth2Spec.computeEpochAtSlot(syncCommitteeMessage.getSlot()),
                    body.getForkInfo().asInternalForkInfo()));
      case SYNC_COMMITTEE_SELECTION_PROOF:
        final SyncAggregatorSelectionData syncAggregatorSelectionData =
            body.getSyncAggregatorSelectionData();
        checkArgument(
            syncAggregatorSelectionData != null, "SyncAggregatorSelectionData is required");
        return signingRootFromSyncCommitteeUtils(
            syncAggregatorSelectionData.getSlot(),
            utils ->
                utils.getSyncAggregatorSelectionDataSigningRoot(
                    asInternalSyncAggregatorSelectionData(syncAggregatorSelectionData),
                    body.getForkInfo().asInternalForkInfo()));
      case SYNC_COMMITTEE_CONTRIBUTION_AND_PROOF:
        final ContributionAndProof contributionAndProof = body.getContributionAndProof();
        checkArgument(contributionAndProof != null, "ContributionAndProof is required");
        return signingRootFromSyncCommitteeUtils(
            contributionAndProof.contribution.slot,
            utils ->
                utils.getContributionAndProofSigningRoot(
                    asInternalContributionAndProof(contributionAndProof),
                    body.getForkInfo().asInternalForkInfo()));
      case VALIDATOR_REGISTRATION:
        final ValidatorRegistration validatorRegistration = body.getValidatorRegistration();
        checkArgument(validatorRegistration != null, "ValidatorRegistration is required");
        return signingRootUtil.signingRootForValidatorRegistration(
            validatorRegistration.asInternalValidatorRegistration());
      default:
        throw new IllegalStateException("Signing root unimplemented for type " + body.getType());
    }
  }

  private tech.pegasys.teku.spec.datastructures.operations.versions.altair
          .SyncAggregatorSelectionData
      asInternalSyncAggregatorSelectionData(
          final SyncAggregatorSelectionData syncAggregatorSelectionData) {
    return SyncAggregatorSelectionDataSchema.INSTANCE.create(
        syncAggregatorSelectionData.getSlot(), syncAggregatorSelectionData.getSubcommitteeIndex());
  }

  private tech.pegasys.teku.spec.datastructures.operations.versions.altair.ContributionAndProof
      asInternalContributionAndProof(final ContributionAndProof contributionAndProof) {
    final tech.pegasys.teku.spec.datastructures.operations.versions.altair.SyncCommitteeContribution
        syncCommitteeContribution =
            SyncCommitteeContribution.asInternalSyncCommitteeContribution(
                eth2Spec, contributionAndProof.contribution);
    return eth2Spec
        .getSyncCommitteeUtilRequired(contributionAndProof.contribution.slot)
        .createContributionAndProof(
            contributionAndProof.aggregatorIndex,
            syncCommitteeContribution,
            contributionAndProof.selectionProof.asInternalBLSSignature());
  }

  private Bytes signingRootFromSyncCommitteeUtils(
      final tech.pegasys.teku.infrastructure.unsigned.UInt64 slot,
      final Function<SyncCommitteeUtil, Bytes> createSigningRoot) {
    return createSigningRoot.apply(eth2Spec.getSyncCommitteeUtilRequired(slot));
  }

  private UInt64 toUInt64(final tech.pegasys.teku.infrastructure.unsigned.UInt64 uInt64) {
    return UInt64.valueOf(uInt64.bigIntegerValue());
  }

  private void respondWithSignature(final RoutingContext routingContext, final String signature) {
    final String acceptableContentType =
        getAcceptableContentType(routingContext.parsedHeaders().accept());
    LOG.trace("Acceptable Content Type {}", acceptableContentType);
    final String body =
        acceptableContentType.equals(JSON_UTF_8)
            ? new JsonObject().put("signature", signature).encode()
            : signature;
    routingContext.response().putHeader(CONTENT_TYPE, acceptableContentType).end(body);
  }

  private Eth2SigningRequestBody getSigningRequest(final RequestParameters params)
      throws JsonProcessingException {
    final String body = params.body().toString();
    return objectMapper.readValue(body, Eth2SigningRequestBody.class);
  }

  private String getAcceptableContentType(final List<MIMEHeader> mimeHeaders) {
    return mimeHeaders.stream()
        .filter(this::isJsonCompatibleHeader)
        .findAny()
        .map(mimeHeader -> JSON_UTF_8)
        .orElse(TEXT_PLAIN_UTF_8);
  }

  private boolean isJsonCompatibleHeader(final MIMEHeader mimeHeader) {
    final String mimeComponent = mimeHeader.component() + "/" + mimeHeader.subComponent();
    return Objects.equals("application/json", mimeComponent)
        || Objects.equals("*/*", mimeComponent);
  }
}
