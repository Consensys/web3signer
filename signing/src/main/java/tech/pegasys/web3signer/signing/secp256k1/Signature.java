/*
 * Copyright 2023 ConsenSys AG.
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
package tech.pegasys.web3signer.signing.secp256k1;

import java.math.BigInteger;

public class Signature {
  private final BigInteger v;
  private final BigInteger r;
  private final BigInteger s;

  public Signature(final BigInteger v, final BigInteger r, final BigInteger s) {
    this.v = v;
    this.r = r;
    this.s = s;
  }

  public BigInteger getV() {
    return v;
  }

  public BigInteger getR() {
    return r;
  }

  public BigInteger getS() {
    return s;
  }
}
