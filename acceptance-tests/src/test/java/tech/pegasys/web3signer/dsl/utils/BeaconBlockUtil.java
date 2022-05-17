/*
 * Copyright 2022 ConsenSys AG.
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

import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.config.SpecConfigBellatrix;
import tech.pegasys.teku.spec.datastructures.blocks.BeaconBlock;
import tech.pegasys.teku.spec.datastructures.blocks.blockbody.BeaconBlockBody;
import tech.pegasys.teku.spec.datastructures.blocks.blockbody.BeaconBlockBodySchema;
import tech.pegasys.teku.spec.datastructures.execution.ExecutionPayload;
import tech.pegasys.teku.spec.schemas.SchemaDefinitionsBellatrix;
import tech.pegasys.teku.spec.util.DataStructureUtil;

import java.util.Random;

import org.apache.tuweni.bytes.Bytes32;

public class BeaconBlockUtil {
  private final DataStructureUtil util;
  private int seed = 92892824;

  public BeaconBlockUtil(final Spec spec) {
    util = new DataStructureUtil(spec);
  }

  public BeaconBlock randomBeaconBlock(final UInt64 slotNum) {
    final UInt64 proposerIndex = util.randomUInt64();
    final Bytes32 previousRoot = util.randomBytes32();
    final Bytes32 stateRoot = util.randomBytes32();
    final BeaconBlockBody body = randomBeaconBlockBody(slotNum);
    return new BeaconBlock(
        util.getSpec().atSlot(slotNum).getSchemaDefinitions().getBeaconBlockSchema(),
        slotNum,
        proposerIndex,
        previousRoot,
        stateRoot,
        body);
  }

  private BeaconBlockBody randomBeaconBlockBody(final UInt64 slotNum) {
    final BeaconBlockBodySchema<?> schema =
        util.getSpec().atSlot(slotNum).getSchemaDefinitions().getBeaconBlockBodySchema();
    return schema.createBlockBody(
        (builder) ->
            builder
                .randaoReveal(util.randomSignature())
                .eth1Data(util.randomEth1Data())
                .graffiti(Bytes32.ZERO)
                .proposerSlashings(
                    util.randomSszList(
                        schema.getProposerSlashingsSchema(), util::randomProposerSlashing, 1L))
                .attesterSlashings(
                    util.randomSszList(
                        schema.getAttesterSlashingsSchema(), util::randomAttesterSlashing, 1L))
                .attestations(
                    util.randomSszList(schema.getAttestationsSchema(), util::randomAttestation, 3L))
                .deposits(
                    util.randomSszList(
                        schema.getDepositsSchema(), util::randomDepositWithoutIndex, 1L))
                .voluntaryExits(
                    util.randomSszList(
                        schema.getVoluntaryExitsSchema(), util::randomSignedVoluntaryExit, 1L))
                .syncAggregate(() -> util.randomSyncAggregateIfRequiredBySchema(schema))
                .executionPayload(() -> randomExecutionPayloadIfRequiredBySchema(schema, slotNum)));
  }

  private ExecutionPayload randomExecutionPayloadIfRequiredBySchema(
      final BeaconBlockBodySchema<?> schema, final UInt64 slotNum) {
    return schema.toVersionBellatrix().map(__ -> randomExecutionPayload(slotNum)).orElse(null);
  }

  private ExecutionPayload randomExecutionPayload(final UInt64 slotNum) {
    final SpecConfigBellatrix specConfigBellatrix =
        SpecConfigBellatrix.required(util.getSpec().getGenesisSpecConfig());
    return SchemaDefinitionsBellatrix.required(
            util.getSpec().atSlot(slotNum).getSchemaDefinitions())
        .getExecutionPayloadSchema()
        .create(
            util.randomBytes32(),
            util.randomBytes20(),
            util.randomBytes32(),
            util.randomBytes32(),
            util.randomBytes(specConfigBellatrix.getBytesPerLogsBloom()),
            util.randomBytes32(),
            util.randomUInt64(),
            util.randomUInt64(),
            util.randomUInt64(),
            util.randomUInt64(),
            util.randomBytes(randomInt(specConfigBellatrix.getMaxExtraDataBytes())),
            util.randomUInt256(),
            util.randomBytes32(),
            util.randomExecutionPayloadTransactions());
  }

  private int randomInt(final int bound) {
    return new Random(nextSeed()).nextInt(bound);
  }

  private int nextSeed() {
    return seed++;
  }
}
