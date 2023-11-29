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
package tech.pegasys.web3signer.dsl.utils;

import tech.pegasys.teku.api.schema.BeaconBlockHeader;
import tech.pegasys.teku.api.schema.Fork;
import tech.pegasys.teku.api.schema.altair.BeaconBlockAltair;
import tech.pegasys.teku.api.schema.altair.BeaconBlockBodyAltair;
import tech.pegasys.teku.api.schema.phase0.BeaconBlockPhase0;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.SpecMilestone;
import tech.pegasys.teku.spec.TestSpecFactory;
import tech.pegasys.teku.spec.datastructures.blocks.BeaconBlock;
import tech.pegasys.teku.spec.datastructures.state.ForkInfo;
import tech.pegasys.teku.spec.signatures.SigningRootUtil;
import tech.pegasys.web3signer.core.service.http.ArtifactType;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.BlockRequest;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.Eth2SigningRequestBody;

import org.apache.tuweni.bytes.Bytes;

public class Eth2BlockSigningRequestUtil {
  private final SpecMilestone specMilestone;
  private final DataStructureUtilAdapter beaconBlockUtil;
  private final SigningRootUtil signingRootUtil;
  private final ForkInfo tekuForkInfo;
  private final Fork tekuFork;
  private final tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.ForkInfo forkInfo;
  private final BeaconBlock beaconBlock;
  private final Bytes signingRoot;

  public Eth2BlockSigningRequestUtil(final SpecMilestone specMilestone) {
    final Spec spec = TestSpecFactory.createMinimal(specMilestone);
    this.specMilestone = specMilestone;
    beaconBlockUtil = new DataStructureUtilAdapter(spec);
    signingRootUtil = new SigningRootUtil(spec);
    tekuForkInfo = Eth2RequestUtils.forkInfo().asInternalForkInfo();
    tekuFork = new Fork(tekuForkInfo.getFork());
    forkInfo =
        new tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.ForkInfo(
            tekuFork, tekuForkInfo.getGenesisValidatorsRoot());
    beaconBlock = beaconBlockUtil.randomBeaconBlock(UInt64.valueOf(0L));
    signingRoot = signingRootUtil.signingRootForSignBlock(beaconBlock, tekuForkInfo);
  }

  public Eth2BlockSigningRequestUtil(
      final Spec spec, final UInt64 forkEpoch, final UInt64 beaconBlockSlot) {
    specMilestone = spec.atEpoch(forkEpoch).getMilestone();
    beaconBlockUtil = new DataStructureUtilAdapter(spec);
    signingRootUtil = new SigningRootUtil(spec);
    tekuForkInfo = Eth2RequestUtils.forkInfo(forkEpoch.longValue()).asInternalForkInfo();
    tekuFork = new Fork(tekuForkInfo.getFork());
    forkInfo =
        new tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.ForkInfo(
            tekuFork, tekuForkInfo.getGenesisValidatorsRoot());
    beaconBlock = beaconBlockUtil.randomBeaconBlock(beaconBlockSlot);
    signingRoot = signingRootUtil.signingRootForSignBlock(beaconBlock, tekuForkInfo);
  }

  public Eth2SigningRequestBody createBlockV2Request() {
    switch (specMilestone) {
      case PHASE0:
      case ALTAIR:
        return createBlockV2Request(new BlockRequest(specMilestone, getBeaconBlock()));
      case BELLATRIX:
      case CAPELLA:
      case DENEB:
        return createBlockV2Request(new BlockRequest(specMilestone, getBeaconBlockHeader()));
      default:
        throw new IllegalStateException(
            "Spec not yet implemented for BLOCKV2 Signing AT: " + specMilestone);
    }
  }

  public Eth2SigningRequestBody createBlockV2Request(final BlockRequest blockRequest) {
    return Eth2SigningRequestBodyBuilder.anEth2SigningRequestBody()
        .withType(ArtifactType.BLOCK_V2)
        .withSigningRoot(signingRoot)
        .withForkInfo(forkInfo)
        .withBlockRequest(blockRequest)
        .build();
  }

  public Eth2SigningRequestBody createLegacyBlockRequest() {
    if (specMilestone != SpecMilestone.PHASE0) {
      throw new IllegalStateException(
          "Only PHASE0 spec is supported to create legacy BLOCK type signing request");
    }

    return Eth2SigningRequestBodyBuilder.anEth2SigningRequestBody()
        .withType(ArtifactType.BLOCK)
        .withSigningRoot(signingRoot)
        .withForkInfo(forkInfo)
        .withBlock(getBeaconBlock())
        .build();
  }

  private BeaconBlockHeader getBeaconBlockHeader() {
    return new BeaconBlockHeader(
        beaconBlock.getSlot(),
        beaconBlock.getProposerIndex(),
        beaconBlock.getParentRoot(),
        beaconBlock.getStateRoot(),
        beaconBlock.getBodyRoot());
  }

  private tech.pegasys.teku.api.schema.BeaconBlock getBeaconBlock() {
    switch (specMilestone) {
      case PHASE0:
        return new BeaconBlockPhase0(beaconBlock);
      case ALTAIR:
        return new BeaconBlockAltair(
            beaconBlock.getSlot(),
            beaconBlock.getProposerIndex(),
            beaconBlock.getParentRoot(),
            beaconBlock.getStateRoot(),
            getBeaconBlockBodyAltair(beaconBlock.getBody()));
      default:
        throw new IllegalStateException("BeaconBlock only supported for PHASE0 and ALTAIR in AT");
    }
  }

  private BeaconBlockBodyAltair getBeaconBlockBodyAltair(
      final tech.pegasys.teku.spec.datastructures.blocks.blockbody.BeaconBlockBody body) {
    return new BeaconBlockBodyAltair(
        tech.pegasys.teku.spec.datastructures.blocks.blockbody.versions.altair.BeaconBlockBodyAltair
            .required(body));
  }
}
