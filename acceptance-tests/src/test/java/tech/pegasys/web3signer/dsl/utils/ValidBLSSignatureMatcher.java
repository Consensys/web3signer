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

import tech.pegasys.teku.bls.BLS;
import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.bls.BLSSignature;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hamcrest.TypeSafeMatcher;

/** A Hamcrest matcher that verifies a BLS scheme signature is valid. */
public class ValidBLSSignatureMatcher extends TypeSafeMatcher<String> {
  private final BLSPublicKey blsPublicKey;
  private final Bytes32 signingRoot;

  public ValidBLSSignatureMatcher(final String blsPubHex, final Bytes32 signingRoot) {
    this.blsPublicKey = BLSPublicKey.fromHexString(blsPubHex);
    this.signingRoot = signingRoot;
  }

  @Override
  protected boolean matchesSafely(final String signature) {
    final BLSSignature blsSignature =
        BLSSignature.fromBytesCompressed(Bytes.fromHexString(signature.replace("\"", "")));
    return BLS.verify(blsPublicKey, signingRoot, blsSignature);
  }

  @Override
  public void describeTo(final org.hamcrest.Description description) {
    description.appendText("a valid BLS signature");
  }
}
