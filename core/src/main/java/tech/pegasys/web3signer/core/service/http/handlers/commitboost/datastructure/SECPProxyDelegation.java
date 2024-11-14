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
package tech.pegasys.web3signer.core.service.http.handlers.commitboost.datastructure;

import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.infrastructure.ssz.containers.Container2;
import tech.pegasys.teku.infrastructure.ssz.tree.TreeNode;
import tech.pegasys.teku.spec.datastructures.type.SszPublicKey;

import java.security.interfaces.ECPublicKey;

public class SECPProxyDelegation
    extends Container2<SECPProxyDelegation, SszPublicKey, SszSECPPublicKey> {

  public SECPProxyDelegation(
      final SECPProxyDelegationSchema schema,
      final BLSPublicKey delegator,
      final ECPublicKey proxy) {
    super(schema, new SszPublicKey(delegator), new SszSECPPublicKey(proxy));
  }

  SECPProxyDelegation(final SECPProxyDelegationSchema schema, final TreeNode backingNode) {
    super(schema, backingNode);
  }

  @Override
  public SECPProxyDelegationSchema getSchema() {
    return (SECPProxyDelegationSchema) super.getSchema();
  }
}
