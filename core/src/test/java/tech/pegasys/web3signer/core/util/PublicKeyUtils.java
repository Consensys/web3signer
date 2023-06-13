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
package tech.pegasys.web3signer.core.util;

import tech.pegasys.web3signer.signing.secp256k1.EthPublicKeyUtils;

import java.security.interfaces.ECPublicKey;

import org.apache.tuweni.bytes.Bytes;

public class PublicKeyUtils {

  public static ECPublicKey createKeyFrom(final String hexString) {
    Bytes bytes = Bytes.fromHexString(hexString, 64);
    return EthPublicKeyUtils.createPublicKey(bytes);
  }
}
