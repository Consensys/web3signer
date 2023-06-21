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

import org.apache.tuweni.bytes.Bytes;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class EthMessageUtilTest {
  @Test
  void hexStringToEthMessage() {
    final String message = "0xdeadbeaf";
    Assertions.assertThat(EthMessageUtil.getEthereumMessage(message))
        .isEqualTo(
            Bytes.fromHexString(
                "0x19457468657265756d205369676e6564204d6573736167653a0a34deadbeaf"));
  }

  @Test
  void emptyStringToEthMessage() {
    final String message = "";
    Assertions.assertThat(EthMessageUtil.getEthereumMessage(message))
        .isEqualTo(Bytes.fromHexString("0x19457468657265756d205369676e6564204d6573736167653a0a30"));
  }

  @Test
  void literalStringToEthMessage() {
    final String message = "hello world";
    Assertions.assertThat(EthMessageUtil.getEthereumMessage(message))
        .isEqualTo(
            Bytes.fromHexString(
                "0x19457468657265756d205369676e6564204d6573736167653a0a313168656c6c6f20776f726c64"));
  }
}
