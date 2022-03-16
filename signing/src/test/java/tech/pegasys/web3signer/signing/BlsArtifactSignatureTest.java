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
package tech.pegasys.web3signer.signing;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.teku.bls.BLSSignature;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;

class BlsArtifactSignatureTest {
  private static final String SIGNATURE =
      "0x932603f10c7efd5320596aece2750b6f0fbc982b818f3a571b3a8522eec08d85362deffd27a79210b0d2a64a431afaf60ed4fe882224b70fa4cf9e6af2b08caf1fbb9c9498bf4dee157e261e8ec74755b740a1cb60cd83becf8201d81d2f7cff";

  @Test
  void hexEncodedSignatureIsReturned() {
    final BLSSignature blsSignature =
        BLSSignature.fromBytesCompressed(Bytes.fromHexString(SIGNATURE));
    final BlsArtifactSignature blsArtifactSignature = new BlsArtifactSignature(blsSignature);
    assertThat(blsArtifactSignature.getSignatureData().toString()).isEqualTo(SIGNATURE);
    assertThat(blsSignature.toBytesCompressed().toHexString()).isEqualTo(SIGNATURE);
    assertThat(blsSignature.toString()).isEqualTo(SIGNATURE);
  }
}
