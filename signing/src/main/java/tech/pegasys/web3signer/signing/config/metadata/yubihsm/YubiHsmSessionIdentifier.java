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
package tech.pegasys.web3signer.signing.config.metadata.yubihsm;

import tech.pegasys.web3signer.signing.config.metadata.YubiHsmSigningMetadata;

import java.util.Objects;

public class YubiHsmSessionIdentifier {
  private final String modulePath;
  private final String connectorUrl;
  private final short authId;

  public YubiHsmSessionIdentifier(
      final String modulePath, final String connectorUrl, final short authId) {
    this.modulePath = modulePath;
    this.connectorUrl = connectorUrl;
    this.authId = authId;
  }

  public static YubiHsmSessionIdentifier buildFrom(final YubiHsmSigningMetadata metadata) {
    return new YubiHsmSessionIdentifier(
        metadata.getPkcs11ModulePath(), metadata.getConnectorUrl(), metadata.getAuthId());
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final YubiHsmSessionIdentifier that = (YubiHsmSessionIdentifier) o;
    return authId == that.authId
        && modulePath.equals(that.modulePath)
        && connectorUrl.equals(that.connectorUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(modulePath, connectorUrl, authId);
  }
}
