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
package tech.pegasys.eth2signer.core.signing;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.eth2signer.core.signing.FilecoinAddress.Network;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FilecoinAddressTest {

  @ParameterizedTest
  @CsvSource(
      "ad58df696e2d4e91ea86c881e938ba4ea81b395e12797b84b9cf314b9546705e839c7a99d606b247ddb4f9ac7a3414dd,f3vvmn62lofvhjd2ugzca6sof2j2ubwok6cj4xxbfzz4yuxfkgobpihhd2thlanmsh3w2ptld2gqkn2jvlss4a")
  void blsAddress(final String publicKey, final String expectedAddress) {
    final Bytes publicKeyBytes = Bytes.fromHexString(publicKey);
    final FilecoinAddress filecoinAddress =
        FilecoinAddress.blsAddress(Network.MAINNET, publicKeyBytes);
    assertThat(filecoinAddress.toString()).isEqualTo(expectedAddress);
  }

  @ParameterizedTest
  @CsvSource(
      "047687b910379bf28cbe3aea674b12000c6b7dba46ffc05f6c94fe2a22bbcc2602ff7f5c76f21ca55d36959152b0e1e887917c393576eef093f61ebd3ad06f7fda,f12fiakbhe2gwd5cnmrenekasyn6v5tnaxaqizq6a")
  void secpAddress(final String publicKey, final String expectedAddress) {
    final Bytes publicKeyBytes = Bytes.fromHexString(publicKey);
    final FilecoinAddress filecoinAddress =
        FilecoinAddress.secpAddress(Network.MAINNET, publicKeyBytes);
    assertThat(filecoinAddress.toString()).isEqualTo(expectedAddress);
  }
}
