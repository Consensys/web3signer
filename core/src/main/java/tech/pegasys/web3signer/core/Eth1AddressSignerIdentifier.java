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
package tech.pegasys.web3signer.core;

import static org.web3j.crypto.Keys.getAddress;
import static tech.pegasys.web3signer.signing.secp256k1.EthPublicKeyUtils.toHexString;
import static tech.pegasys.web3signer.signing.secp256k1.util.AddressUtil.remove0xPrefix;

import tech.pegasys.web3signer.signing.secp256k1.SignerIdentifier;

import java.security.interfaces.ECPublicKey;
import java.util.Locale;
import java.util.Objects;

public class Eth1AddressSignerIdentifier implements SignerIdentifier {

  private final String address;

  public Eth1AddressSignerIdentifier(final String address) {
    this.address = remove0xPrefix(address).toLowerCase(Locale.US);
  }

  public static SignerIdentifier fromPublicKey(final ECPublicKey publicKey) {
    return new Eth1AddressSignerIdentifier(getAddress(toHexString(publicKey)));
  }

  public static SignerIdentifier fromPublicKey(final String publicKey) {
    return new Eth1AddressSignerIdentifier(getAddress(publicKey));
  }

  @Override
  public String toStringIdentifier() {
    return address;
  }

  @Override
  public boolean validate(final ECPublicKey publicKey) {
    if (publicKey == null) {
      return false;
    }
    return address.equalsIgnoreCase(remove0xPrefix(getAddress(toHexString(publicKey))));
  }

  @Override
  public String toString() {
    return toStringIdentifier();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final Eth1AddressSignerIdentifier that = (Eth1AddressSignerIdentifier) o;
    return address.equals(that.address);
  }

  @Override
  public int hashCode() {
    return Objects.hash(address);
  }
}
