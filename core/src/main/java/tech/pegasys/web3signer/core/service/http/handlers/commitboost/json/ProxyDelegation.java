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
package tech.pegasys.web3signer.core.service.http.handlers.commitboost.json;

import tech.pegasys.teku.infrastructure.ssz.Merkleizable;
import tech.pegasys.web3signer.core.service.http.handlers.commitboost.datastructure.BLSProxyDelegationSchema;
import tech.pegasys.web3signer.core.service.http.handlers.commitboost.datastructure.SECPProxyDelegationSchema;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProxyDelegation(
    @JsonProperty(value = "delegator", required = true) String blsPublicKey,
    @JsonProperty(value = "proxy", required = true) String proxyPublicKey) {

  public Merkleizable toMerkleizable(final ProxyKeySignatureScheme scheme) {
    return scheme == ProxyKeySignatureScheme.BLS
        ? new BLSProxyDelegationSchema().create(this)
        : new SECPProxyDelegationSchema().create(this);
  }
}
