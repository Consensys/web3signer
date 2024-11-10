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
package tech.pegasys.web3signer.signing;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.ECKeyPair;
import org.web3j.utils.Numeric;

class K256ArtifactSignerTest {
  private static final String PRIVATE_KEY_HEX =
      "8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63";
  private static final String OBJECT_ROOT_HEX =
      "419a4f6b748659b3ac4fc3534f3767fffe78127d210af0b2e1c1c8e7b345cf64";

  @Test
  void signCreatesVerifiableSignature() {
    // generate signature using web3j
    ECKeyPair web3jECKeyPair = ECKeyPair.create(Numeric.toBigInt(PRIVATE_KEY_HEX));
    Bytes messageToSign = Bytes.fromHexString(OBJECT_ROOT_HEX);

    // generate using K256ArtifactSigner
    K256ArtifactSigner k256ArtifactSigner = new K256ArtifactSigner(web3jECKeyPair);
    System.out.println("K256ArtifactSigner Signature (using Bouncycastle:");
    ArtifactSignature artifactSignature = k256ArtifactSigner.sign(messageToSign);
    System.out.println("R+S: " + artifactSignature.asHex());

    // Verify the signature against public key
    assertThat(k256ArtifactSigner.verify(messageToSign, artifactSignature)).isTrue();
  }
}
