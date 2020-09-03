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
package tech.pegasys.web3signer.dsl.lotus;

import static tech.pegasys.web3signer.dsl.lotus.FilecoinKeyType.BLS;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.tuweni.bytes.Bytes;

public class FilecoinKey {
  private final FilecoinKeyType type;
  private final Bytes privateKey;
  // the hex version is to be stored in Signers configuration file
  private final String privateKeyHex;

  @JsonCreator
  public FilecoinKey(
      @JsonProperty("Type") final FilecoinKeyType type,
      @JsonProperty("PrivateKey") final Bytes privateKey) {
    this.type = type;
    this.privateKey = privateKey;
    // FC BLS Key in Little endian, reverse it.
    this.privateKeyHex =
        type == BLS ? privateKey.reverse().toHexString() : privateKey.toHexString();
  }

  public FilecoinKeyType getType() {
    return type;
  }

  public Bytes getPrivateKey() {
    return privateKey;
  }

  public String getPrivateKeyHex() {
    return privateKeyHex;
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
