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
package tech.pegasys.web3signer.core.service.http.handlers.commitboost.datastructure;

import tech.pegasys.teku.infrastructure.ssz.collections.impl.SszByteVectorImpl;
import tech.pegasys.teku.infrastructure.ssz.tree.TreeNode;
import tech.pegasys.web3signer.signing.secp256k1.EthPublicKeyUtils;

import java.security.interfaces.ECPublicKey;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.apache.tuweni.bytes.Bytes;

public class SszSECPPublicKey extends SszByteVectorImpl {

  private final Supplier<ECPublicKey> publicKey;

  public SszSECPPublicKey(final ECPublicKey publicKey) {
    super(
        SszSECPPublicKeySchema.INSTANCE,
        Bytes.fromHexString(EthPublicKeyUtils.toHexStringCompressed(publicKey)));
    this.publicKey = () -> publicKey;
  }

  SszSECPPublicKey(final TreeNode backingNode) {
    super(SszSECPPublicKeySchema.INSTANCE, backingNode);
    this.publicKey = Suppliers.memoize(() -> EthPublicKeyUtils.bytesToECPublicKey(getBytes()));
  }

  public ECPublicKey getECPublicKey() {
    return publicKey.get();
  }

  @Override
  public SszSECPPublicKeySchema getSchema() {
    return (SszSECPPublicKeySchema) super.getSchema();
  }
}
