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
package tech.pegasys.web3signer.core;

import tech.pegasys.web3signer.signing.secp256k1.Signer;
import tech.pegasys.web3signer.signing.secp256k1.SignerProvider;

import java.security.interfaces.ECPublicKey;
import java.util.Optional;
import java.util.Set;

/** Wrapper on SignerProvider that uses address (Eth1AddressSignerIdentifier) to load signer */
public class Eth1AddressSignerProvider {
  private final SignerProvider signerProvider;

  public Eth1AddressSignerProvider(final SignerProvider signerProvider) {
    this.signerProvider = signerProvider;
  }

  /* Gets a signer from its address, address is expected to be hex value, with or without 0x */
  public Optional<Signer> getSigner(final String address) {
    return signerProvider.getSigner(new Eth1AddressSignerIdentifier(address));
  }

  public Set<ECPublicKey> availablePublicKeys() {
    return signerProvider.availablePublicKeys(Eth1AddressSignerIdentifier::fromPublicKey);
  }
}
