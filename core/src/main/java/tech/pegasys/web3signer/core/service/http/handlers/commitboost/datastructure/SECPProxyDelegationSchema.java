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
import tech.pegasys.web3signer.signing.secp256k1.EthPublicKeyUtils;

import java.security.interfaces.ECPublicKey;

import org.apache.tuweni.bytes.Bytes;

public class SECPProxyDelegationSchema
    extends ContainerSchema2<SECPProxyDelegation, SszPublicKey, SszSECPPublicKey> {
  public SECPProxyDelegationSchema() {
    super(
        "SECPProxyDelegationSchema",
        namedSchema("delegator", SszPublicKeySchema.INSTANCE),
        namedSchema("proxy", SszSECPPublicKeySchema.INSTANCE));
  }

  public SECPProxyDelegation create(final ProxyDelegation proxyDelegation) {
    final BLSPublicKey delegator = BLSPublicKey.fromHexString(proxyDelegation.blsPublicKey());
    final ECPublicKey proxy =
        EthPublicKeyUtils.bytesToECPublicKey(Bytes.fromHexString(proxyDelegation.proxyPublicKey()));
    return new SECPProxyDelegation(this, delegator, proxy);
  }

  @Override
  public SECPProxyDelegation createFromBackingNode(final TreeNode treeNode) {
    return new SECPProxyDelegation(this, treeNode);
  }
}
