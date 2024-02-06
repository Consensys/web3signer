/*
 * Copyright 2024 ConsenSys AG.
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
package tech.pegasys.web3signer.core.service.http.handlers.keymanager.imports;

import java.util.Objects;

import org.apache.tuweni.bytes.Bytes;

/** Keep the index of the keystore import json and password */
public class ImportKeystoreData implements Comparable<ImportKeystoreData> {
  private final int index;
  private final String pubKey;
  private final Bytes pubKeyBytes;
  private final String keystoreJson;
  private final String password;
  private ImportKeystoreResult importKeystoreResult;

  public ImportKeystoreData(
      final int index,
      String pubKey,
      final String keystoreJson,
      final String password,
      final ImportKeystoreResult importKeystoreResult) {
    this.index = index;
    this.pubKey = pubKey;
    this.pubKeyBytes = Bytes.fromHexString(pubKey);
    this.keystoreJson = keystoreJson;
    this.password = password;
    this.importKeystoreResult = importKeystoreResult;
  }

  public int getIndex() {
    return index;
  }

  public String getPubKey() {
    return pubKey;
  }

  public Bytes getPubKeyBytes() {
    return pubKeyBytes;
  }

  public String getKeystoreJson() {
    return keystoreJson;
  }

  public String getPassword() {
    return password;
  }

  public void setImportKeystoreResult(ImportKeystoreResult importKeystoreResult) {
    this.importKeystoreResult = importKeystoreResult;
  }

  public ImportKeystoreResult getImportKeystoreResult() {
    return importKeystoreResult;
  }

  @Override
  public int compareTo(final ImportKeystoreData other) {
    return Integer.compare(this.index, other.index);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ImportKeystoreData that = (ImportKeystoreData) o;
    return index == that.index;
  }

  @Override
  public int hashCode() {
    return Objects.hash(index);
  }
}
