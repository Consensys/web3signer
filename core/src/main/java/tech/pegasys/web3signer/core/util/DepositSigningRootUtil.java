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
package tech.pegasys.web3signer.core.util;

import tech.pegasys.teku.spec.datastructures.state.ForkData;
import tech.pegasys.teku.spec.datastructures.state.SigningData;
import tech.pegasys.teku.ssz.Merkleizable;
import tech.pegasys.teku.ssz.type.Bytes4;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public class DepositSigningRootUtil {
  public static Bytes compute_signing_root(final Merkleizable object, final Bytes32 domain) {
    return new SigningData(object.hashTreeRoot(), domain).hashTreeRoot();
  }

  public static Bytes32 compute_domain(
      final Bytes4 domain_type, final Bytes4 fork_version, final Bytes32 genesis_validators_root) {
    final Bytes32 fork_data_root = compute_fork_data_root(fork_version, genesis_validators_root);
    return compute_domain(domain_type, fork_data_root);
  }

  private static Bytes32 compute_domain(final Bytes4 domain_type, final Bytes32 fork_data_root) {
    return Bytes32.wrap(
        Bytes.concatenate(domain_type.getWrappedBytes(), fork_data_root.slice(0, 28)));
  }

  private static Bytes32 compute_fork_data_root(
      final Bytes4 current_version, final Bytes32 genesis_validators_root) {
    return new ForkData(current_version, genesis_validators_root).hashTreeRoot();
  }
}
