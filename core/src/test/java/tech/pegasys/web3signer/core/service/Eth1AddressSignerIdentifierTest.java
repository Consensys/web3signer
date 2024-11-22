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
package tech.pegasys.web3signer.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.web3signer.core.Eth1AddressSignerIdentifier;
import tech.pegasys.web3signer.signing.secp256k1.EthPublicKeyUtils;
import tech.pegasys.web3signer.signing.secp256k1.SignerIdentifier;

import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Keys;

class Eth1AddressSignerIdentifierTest {
  private static KeyPair secp256k1KeyPair;
  private static KeyPair secp256k1KeyPair2;

  @BeforeAll
  static void generateKeyPair() {
    secp256k1KeyPair = EthPublicKeyUtils.generateK256KeyPair();
    secp256k1KeyPair2 = EthPublicKeyUtils.generateK256KeyPair();
  }

  @Test
  void prefixIsRemovedFromAddress() {
    // web3j.crypto.Keys.getAddress() returns lower case address without 0x prefix
    final String address =
        Keys.getAddress(
            EthPublicKeyUtils.ecPublicKeyToWeb3JPublicKey(
                (ECPublicKey) secp256k1KeyPair.getPublic()));
    // forcefully convert first two alphabets to uppercase and add prefix
    final String mixCaseAddress = "0X" + convertHexToMixCase(address);

    final Eth1AddressSignerIdentifier signerIdentifier =
        new Eth1AddressSignerIdentifier(mixCaseAddress);
    assertThat(signerIdentifier.toStringIdentifier()).isEqualTo(address);
    assertThat(signerIdentifier.toStringIdentifier()).doesNotStartWithIgnoringCase("0x");
    assertThat(signerIdentifier.toStringIdentifier()).isLowerCase();
  }

  @Test
  void validateWorksForSamePrimaryKey() {
    final ECPublicKey publicKey = (ECPublicKey) secp256k1KeyPair.getPublic();
    final SignerIdentifier signerIdentifier = Eth1AddressSignerIdentifier.fromPublicKey(publicKey);
    assertThat(signerIdentifier.validate(publicKey)).isTrue();
  }

  @Test
  void validateFailsForDifferentPrimaryKey() {
    final ECPublicKey publicKey = (ECPublicKey) secp256k1KeyPair.getPublic();
    final SignerIdentifier signerIdentifier = Eth1AddressSignerIdentifier.fromPublicKey(publicKey);
    assertThat(signerIdentifier.validate((ECPublicKey) secp256k1KeyPair2.getPublic())).isFalse();
  }

  @Test
  void validateFailsForNullPrimaryKey() {
    final ECPublicKey publicKey = (ECPublicKey) secp256k1KeyPair.getPublic();
    final SignerIdentifier signerIdentifier = Eth1AddressSignerIdentifier.fromPublicKey(publicKey);
    assertThat(signerIdentifier.validate(null)).isFalse();
  }

  @Test
  void correctEth1AddressIsGeneratedFromPublicKey() {
    final ECPublicKey publicKey = (ECPublicKey) secp256k1KeyPair.getPublic();
    final SignerIdentifier signerIdentifier = Eth1AddressSignerIdentifier.fromPublicKey(publicKey);

    // web3j.crypto.Keys.getAddress() returns lower case address without 0x prefix
    final String expectedAddress =
        Keys.getAddress(EthPublicKeyUtils.ecPublicKeyToWeb3JPublicKey(publicKey));
    assertThat(signerIdentifier.toStringIdentifier()).isEqualTo(expectedAddress);
    assertThat(signerIdentifier.toStringIdentifier()).doesNotStartWithIgnoringCase("0x");
    assertThat(signerIdentifier.toStringIdentifier()).isLowerCase();
  }

  /**
   * Converts first two alphabets to uppercase that can be used to test the case sensitivity of the
   * address
   *
   * @param input address string in hex, assuming all characters are lowercase.
   * @return address with first two alphabets converted to uppercase
   */
  private static String convertHexToMixCase(final String input) {
    final char[] chars = input.toCharArray();
    int count = 0;

    for (int i = 0; i < chars.length && count < 2; i++) {
      if (Character.isLetter(chars[i]) && Character.isLowerCase(chars[i])) {
        chars[i] = Character.toUpperCase(chars[i]);
        count++;
      }
    }

    return new String(chars);
  }
}
