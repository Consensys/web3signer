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

  private static final String BLS_PRIVATE_KEY1 =
      "ad58df696e2d4e91ea86c881e938ba4ea81b395e12797b84b9cf314b9546705e839c7a99d606b247ddb4f9ac7a3414dd";
  private static final String BLS_PRIVATE_KEY2 =
      "b3294f0a2e29e0c66ebc235d2fedca5697bf784af605c75af608e6a63d5cd38ea85ca8989e0efde9188b382f9372460d";
  private static final String BLS_PRIVATE_KEY3 =
      "96a1a3e4ea7a14d49985e661b22401d44fed402d1d0925b243c923589c0fbc7e32cd04e29ed78d15d37d3aaa3fe6da33";
  private static final String BLS_PRIVATE_KEY4 =
      "86b454258c589475f7d16f5aac018a79f6c1169d20fc33921dd8b5ce1cac6c348f90a3603624f6aeb91b64518c2e8095";
  private static final String BLS_PRIVATE_KEY5 =
      "a7726b038022f75a384617585360cee629070a2d9d28712965e5f26ecc40858382803724ed34f2720336f09db631f074";
  private static final String BLS_ADDRESS_1 = "f3vvmn62lofvhjd2ugzca6sof2j2ubwok6cj4xxbfzz4yuxfkgobpihhd2thlanmsh3w2ptld2gqkn2jvlss4a";
  private static final String BLS_ADDRESS_2 = "f3wmuu6crofhqmm3v4enos73okk2l366ck6yc4owxwbdtkmpk42ohkqxfitcpa57pjdcftql4tojda2poeruwa";
  private static final String BLS_ADDRESS_3 = "f3s2q2hzhkpiknjgmf4zq3ejab2rh62qbndueslmsdzervrhapxr7dftie4kpnpdiv2n6tvkr743ndhrsw6d3a";
  private static final String BLS_ADDRESS_4 = "f3q22fijmmlckhl56rn5nkyamkph3mcfu5ed6dheq53c244hfmnq2i7efdma3cj5voxenwiummf2ajlsbxc65a";
  private static final String BLS_ADDRESS_5 = "f3u5zgwa4ael3vuocgc5mfgygo4yuqocrntuuhcklf4xzg5tcaqwbyfabxetwtj4tsam3pbhnwghyhijr5mixa";
  private static final String SECP_KEY_1 =
      "047687b910379bf28cbe3aea674b12000c6b7dba46ffc05f6c94fe2a22bbcc2602ff7f5c76f21ca55d36959152b0e1e887917c393576eef093f61ebd3ad06f7fda";
  private static final String SECP_ADDRESS_1 = "f12fiakbhe2gwd5cnmrenekasyn6v5tnaxaqizq6a";

  @ParameterizedTest
  @CsvSource({
    BLS_PRIVATE_KEY1 + "," + BLS_ADDRESS_1,
    BLS_PRIVATE_KEY2 + "," + BLS_ADDRESS_2,
    BLS_PRIVATE_KEY3 + "," + BLS_ADDRESS_3,
    BLS_PRIVATE_KEY4 + "," + BLS_ADDRESS_4,
    BLS_PRIVATE_KEY5 + "," + BLS_ADDRESS_5
  })
  void testVectorsForBlsAddresses(final String publicKey, final String expectedAddress) {
    final Bytes publicKeyBytes = Bytes.fromHexString(publicKey);
    final FilecoinAddress filecoinAddress =
        FilecoinAddress.blsAddress(Network.MAINNET, publicKeyBytes);
    final String actual = filecoinAddress.toString();
    assertThat(actual).isEqualTo(expectedAddress);
  }

  @ParameterizedTest
  @CsvSource(SECP_KEY_1 + "," + SECP_ADDRESS_1)
  void testVectorsForSecpAddresses(final String publicKey, final String expectedAddress) {
    final Bytes publicKeyBytes = Bytes.fromHexString(publicKey);
    final FilecoinAddress filecoinAddress =
        FilecoinAddress.secpAddress(Network.MAINNET, publicKeyBytes);
    assertThat(filecoinAddress.toString()).isEqualTo(expectedAddress);
  }
}
