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
package tech.pegasys.web3signer.tests.publickeys;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.web3signer.signing.KeyType;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class FilecoinKeyIdentifiersAcceptanceTest extends KeyIdentifiersAcceptanceTestBase {

  @ParameterizedTest
  @EnumSource(value = KeyType.class)
  public void noLoadedKeysReturnsEmptyPublicKeyResponse() {
    initAndStartSigner("filecoin");
    assertThat(signer.walletList()).isEmpty();
  }

  @ParameterizedTest
  @EnumSource(value = KeyType.class)
  public void invalidKeysReturnsEmptyPublicKeyResponse(final KeyType keyType) {
    createKeys(keyType, false, privateKeys(keyType));
    initAndStartSigner("filecoin");
    assertThat(signer.walletList()).isEmpty();
  }

  @ParameterizedTest
  @EnumSource(value = KeyType.class)
  public void onlyValidKeysAreReturnedInPublicKeyResponse(final KeyType keyType) {
    final String[] prvKeys = privateKeys(keyType);
    createKeys(keyType, true, prvKeys[0]);
    createKeys(keyType, false, prvKeys[1]);
    initAndStartSigner("filecoin");

    final String[] filecoinAddresses = filecoinAddresses(keyType);
    assertThat(signer.walletList()).containsExactly(filecoinAddresses[0]);
  }

  @ParameterizedTest
  @EnumSource(value = KeyType.class)
  public void filecoinWalletHasReturnFalseWhenKeysAreNotLoaded(final KeyType keyType) {
    initAndStartSigner("filecoin");

    final String[] filecoinAddresses = filecoinAddresses(keyType);

    assertThat(signer.walletHas(filecoinAddresses[0])).isEqualTo(false);
    assertThat(signer.walletHas(filecoinAddresses[1])).isEqualTo(false);
  }

  @ParameterizedTest
  @EnumSource(value = KeyType.class)
  public void filecoinWalletHasReturnsValidResponse(final KeyType keyType) {
    final String[] prvKeys = privateKeys(keyType);
    createKeys(keyType, true, prvKeys[0]);
    createKeys(keyType, false, prvKeys[1]);

    initAndStartSigner("filecoin");

    final String[] filecoinAddresses = filecoinAddresses(keyType);

    assertThat(signer.walletHas(filecoinAddresses[0])).isEqualTo(true);
    assertThat(signer.walletHas(filecoinAddresses[1])).isEqualTo(false);
  }

  @ParameterizedTest
  @EnumSource(value = KeyType.class)
  public void allLoadedKeysAreReturnedPublicKeyResponseWithEmptyAccept(final KeyType keyType) {
    createKeys(keyType, true, privateKeys(keyType));
    initAndStartSigner("filecoin");

    final String[] filecoinAddresses = filecoinAddresses(keyType);
    assertThat(signer.walletList()).containsOnly(filecoinAddresses);
  }
}
