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

public enum Curve {
  SECP256K1("SECP256K1"),
  BLS("BLS");

  private final String curveName;

  Curve(final String curveName) {
    this.curveName = curveName;
  }

  public static Curve fromString(final String value) {
    if (value.toUpperCase().equals(SECP256K1.curveName)) {
      return SECP256K1;
    } else if (value.toUpperCase().equals(BLS.curveName)) {
      return BLS;
    }
    throw new IllegalArgumentException("Invalid curve value (" + value + ")  provided.");
  }

  public String asString() {
    return curveName;
  }
}
