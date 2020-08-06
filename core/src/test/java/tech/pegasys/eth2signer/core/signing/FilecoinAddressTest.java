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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

class FilecoinAddressTest {

  // TODO round trip tests
  // TODO invalid address tests

  @ParameterizedTest
  @CsvFileSource(resources = "bls_testvectors.csv")
  void testVectorsForBlsAddresses(final String address, final String payload) {
    final FilecoinAddress filecoinAddress = FilecoinAddress.fromString(address);
    final String expectedPayload = payload.substring(2);
    assertThat(filecoinAddress.getPayload().toUnprefixedHexString()).isEqualTo(expectedPayload);
  }

  @ParameterizedTest
  @CsvFileSource(resources = "secp_testvectors.csv")
  void testVectorsForSecpAddresses(final String address, final String payload) {
    final FilecoinAddress filecoinAddress = FilecoinAddress.fromString(address);
    final String expectedPayload = payload.substring(2);
    assertThat(filecoinAddress.getPayload().toUnprefixedHexString()).isEqualTo(expectedPayload);
  }

  @ParameterizedTest
  @CsvFileSource(resources = "id_testvectors.csv")
  void testVectorsForIdAddresses(final String address, final String payload) {
    final FilecoinAddress filecoinAddress = FilecoinAddress.fromString(address);
    final String expectedPayload = payload.substring(2);
    assertThat(filecoinAddress.getPayload().toUnprefixedHexString()).isEqualTo(expectedPayload);
  }

  @ParameterizedTest
  @CsvFileSource(resources = "actor_testvectors.csv")
  void testVectorsForActorAddresses(final String address, final String payload) {
    final FilecoinAddress filecoinAddress = FilecoinAddress.fromString(address);
    final String expectedPayload = payload.substring(2);
    assertThat(filecoinAddress.getPayload().toUnprefixedHexString()).isEqualTo(expectedPayload);
  }
}
