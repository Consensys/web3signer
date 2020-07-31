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

import static org.apache.tuweni.bytes.Bytes.concatenate;
import static tech.pegasys.eth2signer.core.signing.FilecoinAddress.Network.MAINNET;
import static tech.pegasys.eth2signer.core.signing.FilecoinAddress.Network.TESTNET;
import static tech.pegasys.eth2signer.core.signing.FilecoinAddress.Protocol.ACTOR;
import static tech.pegasys.eth2signer.core.signing.FilecoinAddress.Protocol.BLS;
import static tech.pegasys.eth2signer.core.signing.FilecoinAddress.Protocol.SECP256K1;

import tech.pegasys.eth2signer.core.util.Blake2b;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.io.Base32;

public class FilecoinAddress {
  private static final String MAINNET_PREFIX = "f";
  private static final String TESTNET_PREFIX = "t";

  public enum Network {
    MAINNET,
    TESTNET
  }

  public enum Protocol {
    ID,
    SECP256K1,
    ACTOR,
    BLS
  }

  private final Network network;
  private final Protocol protocol;
  private final Bytes payload;

  public static FilecoinAddress blsAddress(final Network network, final Bytes publicKey) {
    return new FilecoinAddress(network, BLS, publicKey);
  }

  public static FilecoinAddress secpAddress(final Network network, final Bytes publicKey) {
    return new FilecoinAddress(network, Protocol.SECP256K1, Blake2b.sum160(publicKey));
  }

  public static FilecoinAddress fromString(final String address) {
    final Network network = address.startsWith(MAINNET_PREFIX) ? MAINNET : TESTNET;
    final Protocol protocol = toProtocol(address.substring(1, 1));
    final String payload = address.substring(2, address.length() - 4);
    final String checksum = address.substring(address.length() - 4);
    final Bytes decode = Base32.decode(payload);
    return new FilecoinAddress(network, protocol, decode);
  }

  private FilecoinAddress(final Network network, final Protocol protocol, final Bytes payload) {
    this.network = network;
    this.protocol = protocol;
    this.payload = payload;
  }

  @Override
  public String toString() {
    final Bytes checksum = Blake2b.sum32(concatenate(Bytes.of(getProtocol()), payload));
    return getNetwork() + getProtocol() + base32(concatenate(payload, checksum)).toLowerCase();
  }

  private String getNetwork() {
    return network == MAINNET ? MAINNET_PREFIX : TESTNET_PREFIX;
  }

  private byte getProtocol() {
    switch (protocol) {
      case ID:
        return (byte) 0;
      case SECP256K1:
        return (byte) 1;
      case ACTOR:
        return (byte) 2;
      case BLS:
        return (byte) 3;
      default:
        throw new IllegalStateException("Unknown protocol");
    }
  }

  private static Protocol toProtocol(final String protocol) {
    switch (protocol) {
      case "0":
        return Protocol.ACTOR;
      case "1":
        return SECP256K1;
      case "2":
        return ACTOR;
      case "3":
        return BLS;
      default:
        throw new IllegalStateException("Unknown protocol");
    }
  }

  private String base32(final Bytes bytes) {
    final String base32 = Base32.encode(bytes);
    return base32.replaceAll("=", "");
  }
}
