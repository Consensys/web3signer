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
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.SpecMilestone;
import tech.pegasys.teku.spec.TestSpecFactory;
import tech.pegasys.teku.spec.datastructures.operations.AggregateAndProof;
import tech.pegasys.teku.spec.datastructures.state.ForkInfo;
import tech.pegasys.teku.spec.signatures.SigningRootUtil;
import tech.pegasys.teku.spec.util.DataStructureUtil;
import tech.pegasys.web3signer.core.service.http.ArtifactType;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.AggregateAndProofV2;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.Eth2SigningRequestBody;

import org.apache.tuweni.bytes.Bytes;

public class Eth2AggregateAndProofSigningRequestUtil {
  private final SpecMilestone specMilestone;
  private final DataStructureUtil dataStructureUtil;
  private final SigningRootUtil signingRootUtil;
  private final ForkInfo tekuForkInfo;
  private final Fork tekuFork;
  private final tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.ForkInfo forkInfo;
  private final Bytes signingRoot;
  private final AggregateAndProof aggregateAndProof;

  public Eth2AggregateAndProofSigningRequestUtil(final SpecMilestone specMilestone) {
    final Spec spec = TestSpecFactory.createMinimal(specMilestone);
    this.specMilestone = specMilestone;
    dataStructureUtil = new DataStructureUtil(spec);
    signingRootUtil = new SigningRootUtil(spec);
    tekuForkInfo = Eth2RequestUtils.forkInfo().asInternalForkInfo();
    tekuFork = new Fork(tekuForkInfo.getFork());
    forkInfo =
        new tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.ForkInfo(
            tekuFork, tekuForkInfo.getGenesisValidatorsRoot());
    aggregateAndProof = dataStructureUtil.randomAggregateAndProof();
    signingRoot =
        signingRootUtil.signingRootForSignAggregateAndProof(aggregateAndProof, tekuForkInfo);
  }

  public Eth2SigningRequestBody createAggregateAndProofV2Request() {
    final tech.pegasys.teku.api.schema.AggregateAndProof aggregateAndProof =
        new tech.pegasys.teku.api.schema.AggregateAndProof(this.aggregateAndProof);
    return Eth2SigningRequestBodyBuilder.anEth2SigningRequestBody()
        .withType(ArtifactType.AGGREGATE_AND_PROOF_V2)
        .withSigningRoot(signingRoot)
        .withForkInfo(forkInfo)
        .withAggregateAndProofV2(new AggregateAndProofV2(specMilestone, aggregateAndProof))
        .build();
  }
}
