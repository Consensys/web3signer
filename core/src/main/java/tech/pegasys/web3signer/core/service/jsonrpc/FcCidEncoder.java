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
package tech.pegasys.web3signer.core.service.jsonrpc;

import tech.pegasys.web3signer.signing.util.Blake2b;

import io.ipfs.cid.Cid;
import io.ipfs.cid.Cid.Codec;
import io.ipfs.multihash.Multihash;
import org.apache.tuweni.bytes.Bytes;

public class FcCidEncoder {

  public Bytes createCid(final Bytes data) {
    final Bytes hash = Blake2b.sum256(data);
    final Cid cid = Cid.buildCidV1(Codec.DagCbor, Multihash.Type.blake2b_256, hash.toArrayUnsafe());
    return Bytes.wrap(cid.toBytes());
  }
}
