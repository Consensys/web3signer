/*
 * Copyright 2023 ConsenSys AG.
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

import static java.util.stream.Collectors.toList;

import tech.pegasys.teku.infrastructure.async.SafeFuture;
import tech.pegasys.teku.infrastructure.ssz.SszList;
import tech.pegasys.teku.infrastructure.ssz.schema.SszListSchema;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.SpecVersion;
import tech.pegasys.teku.spec.config.SpecConfigBellatrix;
import tech.pegasys.teku.spec.datastructures.blocks.BeaconBlock;
import tech.pegasys.teku.spec.datastructures.blocks.blockbody.BeaconBlockBody;
import tech.pegasys.teku.spec.datastructures.blocks.blockbody.BeaconBlockBodySchema;
import tech.pegasys.teku.spec.datastructures.execution.ExecutionPayload;
import tech.pegasys.teku.spec.datastructures.execution.versions.capella.Withdrawal;
import tech.pegasys.teku.spec.datastructures.operations.BlsToExecutionChange;
import tech.pegasys.teku.spec.datastructures.operations.SignedBlsToExecutionChange;
import tech.pegasys.teku.spec.schemas.SchemaDefinitionsBellatrix;
import tech.pegasys.teku.spec.schemas.SchemaDefinitionsCapella;
import tech.pegasys.teku.spec.util.DataStructureUtil;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.apache.tuweni.bytes.Bytes32;

/**
 * This class provide different implementation of randomBeaconBlock than Teku's DataStructureUtil.
 * Instead of using genesis schema definition, our implementation uses spec at slot number to derive
 * the schema definition.
 */
public class DataStructureUtilAdapter {
  private static final int MAX_EP_RANDOM_WITHDRAWALS = 4;
  private final DataStructureUtil util;
  private int seed = 92892824;
  private final Spec spec;

  public DataStructureUtilAdapter(final Spec spec) {
    this.spec = spec;
    util = new DataStructureUtil(spec);
  }

  public BeaconBlock randomBeaconBlock(final UInt64 slotNum) {
    final UInt64 proposerIndex = util.randomUInt64();
    final Bytes32 previousRoot = util.randomBytes32();
    final Bytes32 stateRoot = util.randomBytes32();
    final BeaconBlockBody body = randomBeaconBlockBody(slotNum);
    return new BeaconBlock(
        spec.atSlot(slotNum).getSchemaDefinitions().getBeaconBlockSchema(),
        slotNum,
        proposerIndex,
        previousRoot,
        stateRoot,
        body);
  }

  private BeaconBlockBody randomBeaconBlockBody(final UInt64 slotNum) {
    final BeaconBlockBodySchema<?> schema =
        spec.atSlot(slotNum).getSchemaDefinitions().getBeaconBlockBodySchema();
    return schema
        .createBlockBody(
            builder -> {
              builder
                  .randaoReveal(util.randomSignature())
                  .eth1Data(util.randomEth1Data())
                  .graffiti(Bytes32.ZERO)
                  .proposerSlashings(
                      util.randomSszList(
                          schema.getProposerSlashingsSchema(), util::randomProposerSlashing, 1))
                  .attesterSlashings(
                      util.randomSszList(
                          schema.getAttesterSlashingsSchema(), util::randomAttesterSlashing, 1))
                  .attestations(
                      util.randomSszList(
                          schema.getAttestationsSchema(), util::randomAttestation, 3))
                  .deposits(
                      util.randomSszList(
                          schema.getDepositsSchema(), util::randomDepositWithoutIndex, 1))
                  .voluntaryExits(
                      util.randomSszList(
                          schema.getVoluntaryExitsSchema(), util::randomSignedVoluntaryExit, 1));
              if (builder.supportsSyncAggregate()) {
                builder.syncAggregate(util.randomSyncAggregateIfRequiredBySchema(schema));
              }
              if (builder.supportsExecutionPayload()) {
                builder.executionPayload(randomExecutionPayload(spec.atSlot(slotNum)));
              }
              if (builder.supportsBlsToExecutionChanges()) {
                builder.blsToExecutionChanges(
                    randomSignedBlsToExecutionChangesList(spec.atSlot(slotNum)));
              }
              if (builder.supportsKzgCommitments()) {
                builder.blobKzgCommitments(util.randomBlobKzgCommitments());
              }
              return SafeFuture.completedFuture(builder).toVoid();
            })
        .join();
  }

  private ExecutionPayload randomExecutionPayload(final SpecVersion specVersion) {
    final SpecConfigBellatrix specConfigBellatrix =
        SpecConfigBellatrix.required(specVersion.getConfig());
    return SchemaDefinitionsBellatrix.required(specVersion.getSchemaDefinitions())
        .getExecutionPayloadSchema()
        .createExecutionPayload(
            builder ->
                builder
                    .parentHash(util.randomBytes32())
                    .feeRecipient(util.randomBytes20())
                    .stateRoot(util.randomBytes32())
                    .receiptsRoot(util.randomBytes32())
                    .logsBloom(util.randomBytes(specConfigBellatrix.getBytesPerLogsBloom()))
                    .prevRandao(util.randomBytes32())
                    .blockNumber(util.randomUInt64())
                    .gasLimit(util.randomUInt64())
                    .gasUsed(util.randomUInt64())
                    .timestamp(util.randomUInt64())
                    .extraData(util.randomBytes(specConfigBellatrix.getMaxExtraDataBytes()))
                    .baseFeePerGas(util.randomUInt256())
                    .blockHash(util.randomBytes32())
                    .transactions(util.randomExecutionPayloadTransactions())
                    .withdrawals(() -> randomExecutionPayloadWithdrawals(specVersion))
                    .excessBlobGas(util::randomUInt64)
                    .blobGasUsed(util::randomUInt64));
  }

  private List<Withdrawal> randomExecutionPayloadWithdrawals(final SpecVersion specVersion) {
    return IntStream.rangeClosed(0, randomInt(MAX_EP_RANDOM_WITHDRAWALS))
        .mapToObj(__ -> randomWithdrawal(specVersion))
        .collect(toList());
  }

  private Withdrawal randomWithdrawal(final SpecVersion specVersion) {
    return SchemaDefinitionsCapella.required(specVersion.getSchemaDefinitions())
        .getWithdrawalSchema()
        .create(
            util.randomUInt64(),
            util.randomValidatorIndex(),
            util.randomBytes20(),
            util.randomUInt64());
  }

  private SszList<SignedBlsToExecutionChange> randomSignedBlsToExecutionChangesList(
      final SpecVersion specVersion) {
    final SszListSchema<SignedBlsToExecutionChange, ?> signedBlsToExecutionChangeSchema =
        SchemaDefinitionsCapella.required(specVersion.getSchemaDefinitions())
            .getBeaconBlockBodySchema()
            .toVersionCapella()
            .orElseThrow()
            .getBlsToExecutionChangesSchema();
    final int maxBlsToExecutionChanges =
        specVersion.getConfig().toVersionCapella().orElseThrow().getMaxBlsToExecutionChanges();

    return util.randomSszList(
        signedBlsToExecutionChangeSchema,
        maxBlsToExecutionChanges,
        () -> randomSignedBlsToExecutionChange(specVersion));
  }

  private SignedBlsToExecutionChange randomSignedBlsToExecutionChange(
      final SpecVersion specVersion) {
    return SchemaDefinitionsCapella.required(specVersion.getSchemaDefinitions())
        .getSignedBlsToExecutionChangeSchema()
        .create(randomBlsToExecutionChange(specVersion), util.randomSignature());
  }

  private BlsToExecutionChange randomBlsToExecutionChange(final SpecVersion specVersion) {
    return SchemaDefinitionsCapella.required(specVersion.getSchemaDefinitions())
        .getBlsToExecutionChangeSchema()
        .create(util.randomValidatorIndex(), util.randomPublicKey(), util.randomBytes20());
  }

  private int randomInt(final int bound) {
    return new Random(nextSeed()).nextInt(bound);
  }

  private int nextSeed() {
    return seed++;
  }
}
