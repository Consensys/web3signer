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
package tech.pegasys.web3signer.dsl.utils;

import tech.pegasys.web3signer.K256TestUtil;
import tech.pegasys.web3signer.signing.secp256k1.EthPublicKeyUtils;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.bouncycastle.math.ec.ECPoint;
import org.hamcrest.TypeSafeMatcher;

/** A Hamcrest matcher that verifies a K256 scheme signature (R+S format) is valid. */
public class ValidK256SignatureMatcher extends TypeSafeMatcher<String> {

  private final ECPoint ecPoint;
  private final Bytes32 signingRoot;

  public ValidK256SignatureMatcher(final String ecPubKeyHex, final Bytes32 signingRoot) {
    this.ecPoint = EthPublicKeyUtils.bytesToBCECPoint(Bytes.fromHexString(ecPubKeyHex));
    this.signingRoot = signingRoot;
  }

  @Override
  protected boolean matchesSafely(final String signature) {
    final byte[] sig = Bytes.fromHexString(signature.replace("\"", "")).toArray();
    return K256TestUtil.verifySignature(ecPoint, signingRoot.toArray(), sig);
  }

  @Override
  public void describeTo(final org.hamcrest.Description description) {
    description.appendText("a valid K256 signature");
  }
}
