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
import tech.pegasys.web3signer.core.util.PublicKeyUtils;
import tech.pegasys.web3signer.signing.secp256k1.EthPublicKeyUtils;
import tech.pegasys.web3signer.signing.secp256k1.SignerIdentifier;
import tech.pegasys.web3signer.signing.secp256k1.util.AddressUtil;

import java.security.interfaces.ECPublicKey;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.web3j.crypto.Keys;

class Eth1AddressSignerIdentifierTest {

  @Test
  void prefixIsRemovedFromAddress() {
    final Eth1AddressSignerIdentifier signerIdentifier = new Eth1AddressSignerIdentifier("0xAb");
    assertThat(signerIdentifier.toStringIdentifier()).isEqualTo("ab");
  }

  @Test
  void validateWorksForSamePrimaryKey() {
    final ECPublicKey publicKey = PublicKeyUtils.createKeyFrom("0xab");
    final SignerIdentifier signerIdentifier = Eth1AddressSignerIdentifier.fromPublicKey(publicKey);
    assertThat(signerIdentifier.validate(publicKey)).isTrue();
  }

  @Test
  void validateFailsForDifferentPrimaryKey() {
    final ECPublicKey publicKey = PublicKeyUtils.createKeyFrom("0xab");
    final SignerIdentifier signerIdentifier = Eth1AddressSignerIdentifier.fromPublicKey(publicKey);
    assertThat(signerIdentifier.validate(PublicKeyUtils.createKeyFrom("0xbb"))).isFalse();
  }

  @Test
  void validateFailsForNullPrimaryKey() {
    final ECPublicKey publicKey = PublicKeyUtils.createKeyFrom("0xab");
    final SignerIdentifier signerIdentifier = Eth1AddressSignerIdentifier.fromPublicKey(publicKey);
    assertThat(signerIdentifier.validate(null)).isFalse();
  }

  @Test
  void correctEth1AddressIsGeneratedFromPublicKey() {
    final ECPublicKey publicKey = PublicKeyUtils.createKeyFrom("0xab");
    final SignerIdentifier signerIdentifier = Eth1AddressSignerIdentifier.fromPublicKey(publicKey);
    final String prefixRemovedAddress =
        AddressUtil.remove0xPrefix(
            Keys.getAddress(EthPublicKeyUtils.toHexString(publicKey)).toLowerCase(Locale.US));
    assertThat(signerIdentifier.toStringIdentifier()).isEqualTo(prefixRemovedAddress);
  }
}
