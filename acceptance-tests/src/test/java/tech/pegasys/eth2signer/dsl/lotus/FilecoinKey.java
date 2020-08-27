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
package tech.pegasys.eth2signer.dsl.lotus;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSSecretKey;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.tuweni.bytes.Bytes;
import org.web3j.crypto.ECKeyPair;
import org.web3j.utils.Numeric;

public class FilecoinKey {
  private final FilecoinKeyType type;
  private final String privateKey;

  private String privateKeyHex;
  private String publicKey;

  @JsonCreator
  public FilecoinKey(
      @JsonProperty("Type") final FilecoinKeyType type,
      @JsonProperty("PrivateKey") final String privateKey) {
    this.type = type;
    this.privateKey = privateKey;
    initKeyPairs();
  }

  public FilecoinKeyType getType() {
    return type;
  }

  public String getPrivateKey() {
    return privateKey;
  }

  public String getPrivateKeyHex() {
    return privateKeyHex;
  }

  public String getPublicKey() {
    return publicKey;
  }

  private void initKeyPairs() {
    final Bytes fcPrivateKey = Bytes.fromBase64String(privateKey);

    switch (type) {
      case BLS:
        // Filecoin BLS private keys are serialised in little endian so must convert to big endian
        final BLSKeyPair keyPair = new BLSKeyPair(BLSSecretKey.fromBytes(fcPrivateKey.reverse()));
        this.publicKey = keyPair.getPublicKey().toString();
        this.privateKeyHex = keyPair.getSecretKey().toBytes().toUnprefixedHexString();
        break;
      case SECP256K1:
        final ECKeyPair ecKeyPair =
            ECKeyPair.create(Numeric.toBigInt(fcPrivateKey.toArrayUnsafe()));
        this.publicKey = Numeric.toHexStringWithPrefix(ecKeyPair.getPublicKey());
        this.privateKeyHex = Numeric.toHexStringWithPrefix(ecKeyPair.getPrivateKey());
        break;
      default:
        throw new IllegalStateException("Unexpected type: " + type);
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final FilecoinKey that = (FilecoinKey) o;
    return type == that.type && privateKey.equals(that.privateKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, privateKey);
  }
}
