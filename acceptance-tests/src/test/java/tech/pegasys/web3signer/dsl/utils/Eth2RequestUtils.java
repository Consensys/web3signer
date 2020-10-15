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
package tech.pegasys.web3signer.dsl.utils;

import static java.util.Collections.emptyList;

import tech.pegasys.teku.api.schema.AttestationData;
import tech.pegasys.teku.api.schema.BLSSignature;
import tech.pegasys.teku.api.schema.BeaconBlock;
import tech.pegasys.teku.api.schema.BeaconBlockBody;
import tech.pegasys.teku.api.schema.Checkpoint;
import tech.pegasys.teku.api.schema.Eth1Data;
import tech.pegasys.teku.api.schema.Fork;
import tech.pegasys.teku.core.signatures.SigningRootUtil;
import tech.pegasys.teku.datastructures.state.ForkInfo;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.ssz.SSZTypes.Bytes4;
import tech.pegasys.web3signer.core.service.http.ArtifactType;
import tech.pegasys.web3signer.core.service.http.Eth2SigningRequestBody;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public class Eth2RequestUtils {

  public static Eth2SigningRequestBody createAttestationRequest(
      final int sourceEpoch, final int targetEpoch, final UInt64 slot) {
    // TODO remove duplication of forkInfo
    final Fork fork =
        new Fork(
            Bytes4.fromHexString("0x00000001"),
            Bytes4.fromHexString("0x00000001"),
            UInt64.valueOf(1));
    final Bytes32 genesisValidatorsRoot =
        Bytes32.fromHexString("0x270d43e74ce340de4bca2b1936beca0f4f5408d9e78aec4850920baf659d5b69");
    final ForkInfo forkInfo = new ForkInfo(fork.asInternalFork(), genesisValidatorsRoot);
    final AttestationData attestationData =
        new AttestationData(
            UInt64.valueOf(32),
            slot,
            Bytes32.fromHexString(
                "0xb2eedb01adbd02c828d5eec09b4c70cbba12ffffba525ebf48aca33028e8ad89"),
            new Checkpoint(UInt64.valueOf(sourceEpoch), Bytes32.ZERO),
            new Checkpoint(
                UInt64.valueOf(targetEpoch),
                Bytes32.fromHexString(
                    "0xb2eedb01adbd02c828d5eec09b4c70cbba12ffffba525ebf48aca33028e8ad89")));
    final Bytes signingRoot =
        SigningRootUtil.signingRootForSignAttestationData(
            attestationData.asInternalAttestationData(), forkInfo);
    return new Eth2SigningRequestBody(
        ArtifactType.ATTESTATION,
        signingRoot,
        genesisValidatorsRoot,
        fork,
        null,
        attestationData,
        null,
        null,
        null,
        null);
  }

  public static Eth2SigningRequestBody createBlockRequest() {
    return createBlockRequest(UInt64.ZERO, Bytes32.fromHexString("0x"));
  }

  public static Eth2SigningRequestBody createBlockRequest(
      final UInt64 slot, final Bytes32 stateRoot) {
    final Fork fork =
        new Fork(
            Bytes4.fromHexString("0x00000001"),
            Bytes4.fromHexString("0x00000001"),
            UInt64.valueOf(1));
    final Bytes32 genesisValidatorsRoot =
        Bytes32.fromHexString("0x270d43e74ce340de4bca2b1936beca0f4f5408d9e78aec4850920baf659d5b69");
    final ForkInfo forkInfo = new ForkInfo(fork.asInternalFork(), genesisValidatorsRoot);
    final BeaconBlock block =
        new BeaconBlock(
            slot,
            UInt64.valueOf(5),
            Bytes32.fromHexString(
                "0xb2eedb01adbd02c828d5eec09b4c70cbba12ffffba525ebf48aca33028e8ad89"),
            stateRoot,
            new BeaconBlockBody(
                BLSSignature.fromHexString(
                    "0xa686652aed2617da83adebb8a0eceea24bb0d2ccec9cd691a902087f90db16aa5c7b03172a35e874e07e3b60c5b2435c0586b72b08dfe5aee0ed6e5a2922b956aa88ad0235b36dfaa4d2255dfeb7bed60578d982061a72c7549becab19b3c12f"),
                new Eth1Data(
                    Bytes32.fromHexString(
                        "0x6a0f9d6cb0868daa22c365563bb113b05f7568ef9ee65fdfeb49a319eaf708cf"),
                    UInt64.valueOf(8),
                    Bytes32.fromHexString(
                        "0x4242424242424242424242424242424242424242424242424242424242424242")),
                Bytes32.fromHexString(
                    "0x74656b752f76302e31322e31302d6465762d6338316361363235000000000000"),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList()));
    final Bytes signingRoot =
        SigningRootUtil.signingRootForSignBlock(block.asInternalBeaconBlock(), forkInfo);
    return new Eth2SigningRequestBody(
        ArtifactType.BLOCK,
        signingRoot,
        genesisValidatorsRoot,
        fork,
        block,
        null,
        null,
        null,
        null,
        null);
  }
}
