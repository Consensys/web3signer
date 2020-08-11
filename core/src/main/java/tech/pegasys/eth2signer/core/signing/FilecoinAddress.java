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
import static tech.pegasys.eth2signer.core.signing.FilecoinAddress.Protocol.ID;
import static tech.pegasys.eth2signer.core.signing.FilecoinAddress.Protocol.SECP256K1;
import static tech.pegasys.eth2signer.core.util.ByteUtils.fromUVariant;
import static tech.pegasys.eth2signer.core.util.ByteUtils.putUVariant;

import tech.pegasys.eth2signer.core.util.Blake2b;

import java.math.BigInteger;
import java.util.List;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.io.Base32;

public class FilecoinAddress {
  private static final String MAINNET_PREFIX = "f";
  private static final String TESTNET_PREFIX = "t";
  private static final int CHECKSUM_SIZE = 4;

  private static final org.apache.commons.codec.binary.Base32 base32 =
      new org.apache.commons.codec.binary.Base32();

  // TODO move these enums out of the class?
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

  private final Protocol protocol;
  private final Bytes payload;

  public static FilecoinAddress fromString(final String address) {
    return decode(address);
  }

  public FilecoinAddress(final Protocol protocol, final Bytes payload) {
    this.protocol = protocol;
    this.payload = payload;
  }

  public Bytes getPayload() {
    return payload;
  }

  public Protocol getProtocol() {
    return protocol;
  }

  public String encode(final Network network) {
    if (protocol == ID) {
      final String payload = fromUVariant(this.payload).toString();
      return networkToString(network) + protocolToByte() + payload;
    } else {
      return networkToString(network)
          + protocolToByte()
          + base32(concatenate(payload, checksum(this))).toLowerCase();
    }
  }

  public static FilecoinAddress decode(final String address) {
    validateNetwork(address.substring(0, 1));
    final Protocol protocol = stringToProtocol(address.substring(1, 2));
    final String rawPayload = address.substring(2);

    if (protocol == ID) {
      final Bytes payload = putUVariant(new BigInteger(rawPayload));
      return new FilecoinAddress(protocol, payload);
    } else {
      if (!base32.isInAlphabet(rawPayload)) {
        throw new IllegalStateException("Invalid payload must be base32 encoded");
      }
      final Bytes value = Bytes.wrap(Base32.decode(rawPayload));
      final Bytes payload = value.slice(0, value.size() - CHECKSUM_SIZE);
      final Bytes checksum = value.slice(value.size() - 4);
      final FilecoinAddress filecoinAddress = new FilecoinAddress(protocol, payload);
      if (!validateChecksum(filecoinAddress, checksum)) {
        throw new IllegalStateException("Filecoin address checksum doesn't match");
      }
      return filecoinAddress;
    }
  }

  public static Bytes checksum(final FilecoinAddress address) {
    return Blake2b.sum32(concatenate(Bytes.of(address.protocolToByte()), address.getPayload()));
  }

  public static boolean validateChecksum(
      final FilecoinAddress address, final Bytes expectedChecksum) {
    final Bytes checksum = checksum(address);
    return expectedChecksum.equals(checksum);
  }

  private String networkToString(final Network network) {
    switch (network) {
      case MAINNET:
        return MAINNET_PREFIX;
      case TESTNET:
        return TESTNET_PREFIX;
      default:
        throw new IllegalStateException("Unknown Filecoin network");
    }
  }

  private static void validateNetwork(final String network) {
    if (!List.of(MAINNET_PREFIX, TESTNET_PREFIX).contains(network)) {
      throw new IllegalStateException("Unknown Filecoin network");
    }
  }

  private byte protocolToByte() {
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
        throw new IllegalStateException("Unknown Filecoin protocol");
    }
  }

  private static Protocol stringToProtocol(final String protocol) {
    switch (protocol) {
      case "0":
        return ID;
      case "1":
        return SECP256K1;
      case "2":
        return ACTOR;
      case "3":
        return BLS;
      default:
        throw new IllegalStateException("Unknown Filecoin protocol");
    }
  }

  private String base32(final Bytes bytes) {
    final String base32 = Base32.encode(bytes);
    return base32.replaceAll("=", "");
  }
}
