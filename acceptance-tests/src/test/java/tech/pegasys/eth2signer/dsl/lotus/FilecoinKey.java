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

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.tuweni.bytes.Bytes;

public class FilecoinKey {
  private final FilecoinKeyType type;
  private final String privateKey;

  private String privateKeyHex;

  @JsonCreator
  public FilecoinKey(
      @JsonProperty("Type") final FilecoinKeyType type,
      @JsonProperty("PrivateKey") final String privateKey) {
    this.type = type;
    this.privateKey = privateKey;
    initPrivateKeyHex();
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

  private void initPrivateKeyHex() {
    final Bytes fcPrivateKey = Bytes.fromBase64String(privateKey);
    switch (type) {
      case BLS:
        // Filecoin BLS private keys are serialised in little endian so must convert to big endian
        this.privateKeyHex = fcPrivateKey.reverse().toHexString();
        break;
      case SECP256K1:
        this.privateKeyHex = fcPrivateKey.toHexString();
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
