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
import tech.pegasys.web3signer.signing.secp256k1.util.AddressUtil;

import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.util.Locale;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Keys;

class Eth1AddressSignerIdentifierTest {
  private static KeyPair secp256k1KeyPair;
  private static KeyPair secp256k1KeyPair2;

  @BeforeAll
  static void generateKeyPair() throws Exception {
    final SecureRandom random = new SecureRandom();
    secp256k1KeyPair = EthPublicKeyUtils.createSecp256k1KeyPair(random);
    secp256k1KeyPair2 = EthPublicKeyUtils.createSecp256k1KeyPair(random);
  }

  @Test
  void prefixIsRemovedFromAddress() {
    final String address =
        Keys.getAddress(
            EthPublicKeyUtils.ecPublicKeyToBigInteger((ECPublicKey) secp256k1KeyPair.getPublic()));
    // forcefully convert some random alphabets to uppercase
    final String mixCaseAddress = convertRandomAlphabetsToUpperCase(address);

    final Eth1AddressSignerIdentifier signerIdentifier =
        new Eth1AddressSignerIdentifier(mixCaseAddress);
    assertThat(signerIdentifier.toStringIdentifier()).isEqualTo(address.toLowerCase(Locale.US));
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
    final String prefixRemovedAddress =
        AddressUtil.remove0xPrefix(
            Keys.getAddress(EthPublicKeyUtils.toHexString(publicKey)).toLowerCase(Locale.US));
    assertThat(signerIdentifier.toStringIdentifier()).isEqualTo(prefixRemovedAddress);
  }

  private static String convertRandomAlphabetsToUpperCase(final String input) {
    final char[] chars = input.toCharArray();
    final Random random = new Random();
    int count = 0;

    while (count < 2 || count < 3 && random.nextBoolean()) {
      int index = random.nextInt(chars.length);
      if (Character.isLetter(chars[index]) && Character.isLowerCase(chars[index])) {
        chars[index] = Character.toUpperCase(chars[index]);
        count++;
      }
    }

    return new String(chars);
  }
}
