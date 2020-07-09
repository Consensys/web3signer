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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.artemis.bls.BLS;
import tech.pegasys.artemis.bls.BLSKeyPair;
import tech.pegasys.artemis.bls.BLSSignature;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;

class BlsArtifactSignatureTest {

  @Test
  void hexEncodedSignatureIsReturned() {
    final Bytes message = Bytes.wrap("Hello, world!".getBytes(UTF_8));
    final BLSKeyPair keyPair = BLSKeyPair.random(4);
    final BLSSignature expectedSignature = BLS.sign(keyPair.getSecretKey(), message);

    final BlsArtifactSignature blsArtifactSignature = new BlsArtifactSignature(expectedSignature);
    assertThat(blsArtifactSignature.toHexString())
        .isEqualTo(expectedSignature.getSignature().toString());
  }
}
