/*
 * Copyright 2024 ConsenSys AG.
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
package tech.pegasys.web3signer.core.service.http.handlers.commitboost;

import tech.pegasys.teku.infrastructure.bytes.Bytes4;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.datastructures.state.ForkData;
import tech.pegasys.teku.spec.datastructures.state.SigningData;

import com.google.common.annotations.VisibleForTesting;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

/**
 * Generates the signing root for a given object root using the commit boost domain.
 *
 * <p>The commit boost domain is computed using the genesis validators root and the genesis fork
 * version.
 */
public class SigningRootGenerator {
  private static final Bytes4 COMMIT_BOOST_DOMAIN = Bytes4.fromHexString("0x6d6d6f43");
  private static final Bytes32 GENESIS_VALIDATORS_ROOT = Bytes32.ZERO;

  private final Bytes32 domain;

  public SigningRootGenerator(final Spec eth2Spec) {
    final Bytes4 genesisForkVersion = eth2Spec.getGenesisSpec().getConfig().getGenesisForkVersion();
    final Bytes32 forkHashTreeRoot =
        new ForkData(genesisForkVersion, GENESIS_VALIDATORS_ROOT).hashTreeRoot();
    domain =
        Bytes32.wrap(
            Bytes.concatenate(
                COMMIT_BOOST_DOMAIN.getWrappedBytes(), forkHashTreeRoot.slice(0, 28)));
  }

  /**
   * Computes the signing root for a given object root using commit boost domain.
   *
   * @param objectRoot the object root to compute the signing root for
   * @return the signing data object root
   */
  public Bytes32 computeSigningRoot(final Bytes32 objectRoot) {
    return new SigningData(objectRoot, domain).hashTreeRoot();
  }

  @VisibleForTesting
  Bytes32 getDomain() {
    return domain;
  }
}
