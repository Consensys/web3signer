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
package tech.pegasys.web3signer.core.multikey.metadata.yubihsm;

import tech.pegasys.signers.yubihsm.pkcs11.Pkcs11YubiHsmPin;

import java.util.Objects;

public class YubiHsmIdentifier {
  private final short authId;
  private final String password;

  public YubiHsmIdentifier(final short authId, final String password) {
    this.authId = authId;
    this.password = password;
  }

  public Pkcs11YubiHsmPin convertToPkcs11Pin() {
    return new Pkcs11YubiHsmPin(authId, password);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final YubiHsmIdentifier that = (YubiHsmIdentifier) o;
    return authId == that.authId && password.equals(that.password);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authId, password);
  }
}
