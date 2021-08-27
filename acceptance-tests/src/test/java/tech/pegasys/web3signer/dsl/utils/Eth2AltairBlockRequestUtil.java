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

public class Eth2AltairBlockRequestUtil {
  private final Spec spec = TestSpecFactory.createMinimalAltair();
  private final DataStructureUtil dataStructureUtil = new DataStructureUtil(spec);
  private final SigningRootUtil signingRootUtil = new SigningRootUtil(spec);
  private final ForkInfo tekuForkInfo = dataStructureUtil.randomForkInfo();
  private final Fork tekuFork = new Fork(tekuForkInfo.getFork());
  private final tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.ForkInfo forkInfo =
      new tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.ForkInfo(
          tekuFork, tekuForkInfo.getGenesisValidatorsRoot());
  private final BeaconBlock beaconBlock = dataStructureUtil.randomBeaconBlock(10);
  private final Bytes signingRoot =
      signingRootUtil.signingRootForSignBlock(beaconBlock, tekuForkInfo);

  public Eth2SigningRequestBody createRandomAltairBlockRequest() {
    final BlockRequest blockRequest =
        new BlockRequest(SpecMilestone.ALTAIR, getBeaconBlockAltair());

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

  private tech.pegasys.teku.api.schema.BeaconBlock getBeaconBlockAltair() {
    return new BeaconBlockAltair(
        beaconBlock.getSlot(),
        beaconBlock.getProposerIndex(),
        beaconBlock.getParentRoot(),
        beaconBlock.getStateRoot(),
        getBeaconBlockBodyAltair(beaconBlock.getBody()));
  }

  private BeaconBlockBodyAltair getBeaconBlockBodyAltair(
      final tech.pegasys.teku.spec.datastructures.blocks.blockbody.BeaconBlockBody body) {
    return new BeaconBlockBodyAltair(
        tech.pegasys.teku.spec.datastructures.blocks.blockbody.versions.altair.BeaconBlockBodyAltair
            .required(body));
  }
}
