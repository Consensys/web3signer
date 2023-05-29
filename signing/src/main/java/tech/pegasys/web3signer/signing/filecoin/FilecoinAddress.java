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
package tech.pegasys.web3signer.signing.filecoin;

import static tech.pegasys.web3signer.signing.filecoin.FilecoinProtocol.BLS;
import static tech.pegasys.web3signer.signing.filecoin.FilecoinProtocol.ID;
import static tech.pegasys.web3signer.signing.filecoin.FilecoinProtocol.SECP256K1;

import tech.pegasys.web3signer.signing.filecoin.exceptions.InvalidAddressChecksumException;
import tech.pegasys.web3signer.signing.filecoin.exceptions.InvalidAddressLengthException;
import tech.pegasys.web3signer.signing.filecoin.exceptions.InvalidAddressPayloadException;
import tech.pegasys.web3signer.signing.filecoin.exceptions.InvalidFilecoinProtocolException;
import tech.pegasys.web3signer.signing.util.Blake2b;
import tech.pegasys.web3signer.signing.util.ByteUtils;

import java.math.BigInteger;

import com.google.common.io.BaseEncoding;
import org.apache.tuweni.bytes.Bytes;

public class FilecoinAddress {

  private static final int CHECKSUM_BYTE_SIZE = 4;
  private static final BaseEncoding BASE_32_ENCODING =
      BaseEncoding.base32().lowerCase().omitPadding();
  private final FilecoinProtocol protocol;
  private final Bytes payload;

  public static FilecoinAddress blsAddress(final Bytes publicKey) {
    return new FilecoinAddress(BLS, publicKey);
  }

  public static FilecoinAddress secpAddress(final Bytes publicKey) {
    return new FilecoinAddress(SECP256K1, Blake2b.sum160(publicKey));
  }

  public static FilecoinAddress fromString(final String address) {
    return decode(address);
  }

  private FilecoinAddress(final FilecoinProtocol protocol, final Bytes payload) {
    this.protocol = protocol;
    this.payload = payload;
  }

  public Bytes getPayload() {
    return payload;
  }

  public FilecoinProtocol getProtocol() {
    return protocol;
  }

  public String encode(final FilecoinNetwork network) {
    if (protocol == ID) {
      final String payload = ByteUtils.fromUVariant(this.payload).toString();
      return network.getNetworkValue() + protocol.getAddrValue() + payload;
    } else {
      return network.getNetworkValue()
          + protocol.getAddrValue()
          + BASE_32_ENCODING.encode(Bytes.concatenate(payload, checksum(this)).toArrayUnsafe());
    }
  }

  public static FilecoinAddress decode(final String address) {
    if (address == null || address.length() < 3) {
      throw new InvalidAddressLengthException();
    }

    FilecoinNetwork.findByNetworkValue(address.substring(0, 1));
    final FilecoinProtocol protocol = FilecoinProtocol.findByAddrValue(address.substring(1, 2));

    final String rawPayload = address.substring(2);

    if (protocol == ID) {
      final Bytes payload = ByteUtils.putUVariant(new BigInteger(rawPayload));
      return new FilecoinAddress(protocol, payload);
    } else {

      if (!BASE_32_ENCODING.canDecode(rawPayload)) {
        throw new InvalidAddressPayloadException();
      }
    }
    final Bytes value = Bytes.wrap(BASE_32_ENCODING.decode(rawPayload));
    final Bytes payload = value.slice(0, value.size() - CHECKSUM_BYTE_SIZE);
    final Bytes checksum = value.slice(value.size() - CHECKSUM_BYTE_SIZE);
    final FilecoinAddress filecoinAddress = new FilecoinAddress(protocol, payload);
    if (!validateChecksum(filecoinAddress, checksum)) {
      throw new InvalidAddressChecksumException();
    }
    return filecoinAddress;
  }

  private static Bytes checksum(final FilecoinAddress address) {
    try {
      final Integer protocolValue = Integer.valueOf(address.protocol.getAddrValue());
      return Blake2b.sum32(Bytes.concatenate(Bytes.of(protocolValue), address.getPayload()));
    } catch (NumberFormatException nfe) {
      throw new InvalidFilecoinProtocolException();
    }
  }

  public static boolean validateChecksum(
      final FilecoinAddress address, final Bytes expectedChecksum) {
    final Bytes checksum = checksum(address);
    return expectedChecksum.equals(checksum);
  }
}
