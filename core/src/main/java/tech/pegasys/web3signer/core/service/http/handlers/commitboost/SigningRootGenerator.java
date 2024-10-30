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
import tech.pegasys.teku.infrastructure.ssz.Merkleizable;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.web3signer.core.service.http.handlers.commitboost.json.ProxyDelegation;
import tech.pegasys.web3signer.core.service.http.handlers.commitboost.json.ProxyKeySignatureScheme;
import tech.pegasys.web3signer.core.util.Web3SignerSigningRootUtil;

import com.google.common.annotations.VisibleForTesting;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public class SigningRootGenerator {
  private static final Bytes4 COMMIT_BOOST_DOMAIN = Bytes4.fromHexString("0x6d6d6f43");
  private final Bytes32 domain;

  public SigningRootGenerator(final Spec eth2Spec, final Bytes32 genesisValidatorsRoot) {
    final Bytes4 genesisForkVersion = eth2Spec.getGenesisSpec().getConfig().getGenesisForkVersion();
    domain =
        Web3SignerSigningRootUtil.computeDomain(
            COMMIT_BOOST_DOMAIN, genesisForkVersion, genesisValidatorsRoot);
  }

  public Bytes computeSigningRoot(
      final ProxyDelegation proxyDelegation, final ProxyKeySignatureScheme scheme) {
    final Merkleizable proxyDelegationMerkleizable = proxyDelegation.toMerkleizable(scheme);

    return Web3SignerSigningRootUtil.computeSigningRoot(proxyDelegationMerkleizable, domain);
  }

  @VisibleForTesting
  Bytes32 getDomain() {
    return domain;
  }
}
