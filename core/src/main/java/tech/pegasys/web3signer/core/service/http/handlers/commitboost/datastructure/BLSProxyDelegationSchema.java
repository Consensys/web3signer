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
import tech.pegasys.teku.infrastructure.ssz.containers.ContainerSchema2;
import tech.pegasys.teku.infrastructure.ssz.tree.TreeNode;
import tech.pegasys.teku.spec.datastructures.type.SszPublicKey;
import tech.pegasys.teku.spec.datastructures.type.SszPublicKeySchema;
import tech.pegasys.web3signer.core.service.http.handlers.commitboost.json.ProxyDelegation;

public class BLSProxyDelegationSchema
    extends ContainerSchema2<BLSProxyDelegation, SszPublicKey, SszPublicKey> {
  public BLSProxyDelegationSchema() {
    super(
        "BLSProxyDelegationSchema",
        namedSchema("delegator", SszPublicKeySchema.INSTANCE),
        namedSchema("proxy", SszPublicKeySchema.INSTANCE));
  }

  public BLSProxyDelegation create(final ProxyDelegation proxyDelegation) {
    final BLSPublicKey delegator = BLSPublicKey.fromHexString(proxyDelegation.blsPublicKey());
    final BLSPublicKey proxy = BLSPublicKey.fromHexString(proxyDelegation.proxyPublicKey());
    return new BLSProxyDelegation(this, delegator, proxy);
  }

  @Override
  public BLSProxyDelegation createFromBackingNode(TreeNode treeNode) {
    return new BLSProxyDelegation(this, treeNode);
  }
}
