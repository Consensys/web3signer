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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.web3signer.signing.util.IdentifierUtils.normaliseIdentifier;

import tech.pegasys.teku.bls.BLS;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSSignature;
import tech.pegasys.web3signer.BLSTestUtil;
import tech.pegasys.web3signer.signing.config.metadata.SignerOrigin;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;

class BlsArtifactSignerTest {
  @Test
  void publicKeyIsReturnedAsIdentifier() {
    final BLSKeyPair keyPair = BLSTestUtil.randomKeyPair(4);
    final BlsArtifactSigner blsArtifactSigner =
        new BlsArtifactSigner(keyPair, SignerOrigin.FILE_RAW);
    final String expectedIdentifier = normaliseIdentifier(keyPair.getPublicKey().toString());
    assertThat(blsArtifactSigner.getIdentifier()).isEqualTo(expectedIdentifier);
  }

  @Test
  void signsData() {
    final Bytes message = Bytes.wrap("Hello, world!".getBytes(UTF_8));
    final BLSKeyPair keyPair = BLSTestUtil.randomKeyPair(4);
    final BLSSignature expectedSignature = BLS.sign(keyPair.getSecretKey(), message);

    final BlsArtifactSigner blsArtifactSigner =
        new BlsArtifactSigner(keyPair, SignerOrigin.FILE_RAW);
    final BlsArtifactSignature signature = blsArtifactSigner.sign(message);

    assertThat(signature.getSignatureData().toString()).isEqualTo(expectedSignature.toString());
  }
}
