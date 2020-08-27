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
package tech.pegasys.eth2signer.tests.comparison;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;

public class TestKey {

  @Test
  void testKey() {
    // SECP Key
    Bytes.fromHexString("0x65893d4dca4a27f5192d8f3b420f503279c660d2524bdae094a4825a975f152");
  }
}
