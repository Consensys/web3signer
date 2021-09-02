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

import tech.pegasys.teku.api.schema.Fork;
import tech.pegasys.teku.api.schema.altair.BeaconBlockAltair;
import tech.pegasys.teku.api.schema.altair.BeaconBlockBodyAltair;
import tech.pegasys.teku.core.signatures.SigningRootUtil;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.SpecMilestone;
import tech.pegasys.teku.spec.TestSpecFactory;
import tech.pegasys.teku.spec.datastructures.blocks.BeaconBlock;
import tech.pegasys.teku.spec.datastructures.state.ForkInfo;
import tech.pegasys.teku.spec.util.DataStructureUtil;
import tech.pegasys.web3signer.core.service.http.ArtifactType;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.BlockRequest;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.Eth2SigningRequestBody;

import org.apache.tuweni.bytes.Bytes;

public class Eth2BlockSigningRequestUtil {
  private final SpecMilestone specMilestone;
  private final DataStructureUtil dataStructureUtil;
  private final SigningRootUtil signingRootUtil;
  private final ForkInfo tekuForkInfo;
  private final Fork tekuFork;
  private final tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.ForkInfo forkInfo;
  private final BeaconBlock beaconBlock;
  private final Bytes signingRoot;

  public Eth2BlockSigningRequestUtil(final SpecMilestone specMilestone) {
    final Spec spec;
    switch (specMilestone) {
      case ALTAIR:
        spec = TestSpecFactory.createMinimalAltair();
        break;
      case PHASE0:
        spec = TestSpecFactory.createMinimalPhase0();
        break;
      default:
        throw new IllegalStateException("Spec Milestone not yet supported: " + specMilestone);
    }
    this.specMilestone = specMilestone;
    dataStructureUtil = new DataStructureUtil(spec);
    signingRootUtil = new SigningRootUtil(spec);
    tekuForkInfo = dataStructureUtil.randomForkInfo();
    tekuFork = new Fork(tekuForkInfo.getFork());
    forkInfo =
        new tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.ForkInfo(
            tekuFork, tekuForkInfo.getGenesisValidatorsRoot());
    beaconBlock = dataStructureUtil.randomBeaconBlock(0);
    signingRoot = signingRootUtil.signingRootForSignBlock(beaconBlock, tekuForkInfo);
  }

  public Eth2SigningRequestBody createBlockV2Request() {
    final BlockRequest blockRequest = new BlockRequest(specMilestone, getBeaconBlock());

    return new Eth2SigningRequestBody(
        ArtifactType.BLOCK_V2,
        signingRoot,
        forkInfo,
        null,
        blockRequest,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  public Eth2SigningRequestBody createLegacyBlockRequest() {
    if (specMilestone != SpecMilestone.PHASE0) {
      throw new IllegalStateException(
          "Only PHASE0 spec is supported to create legacy BLOCK type signing request");
    }

    return new Eth2SigningRequestBody(
        ArtifactType.BLOCK,
        signingRoot,
        forkInfo,
        getBeaconBlock(),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private tech.pegasys.teku.api.schema.BeaconBlock getBeaconBlock() {
    if (specMilestone == SpecMilestone.ALTAIR) {
      return new BeaconBlockAltair(
          beaconBlock.getSlot(),
          beaconBlock.getProposerIndex(),
          beaconBlock.getParentRoot(),
          beaconBlock.getStateRoot(),
          getBeaconBlockBodyAltair(beaconBlock.getBody()));
    } else if (specMilestone == SpecMilestone.PHASE0) {
      return new tech.pegasys.teku.api.schema.BeaconBlock(beaconBlock);
    }

    throw new IllegalStateException("Spec milestone not yet supported: " + specMilestone);
  }

  private BeaconBlockBodyAltair getBeaconBlockBodyAltair(
      final tech.pegasys.teku.spec.datastructures.blocks.blockbody.BeaconBlockBody body) {
    return new BeaconBlockBodyAltair(
        tech.pegasys.teku.spec.datastructures.blocks.blockbody.versions.altair.BeaconBlockBodyAltair
            .required(body));
  }
}
