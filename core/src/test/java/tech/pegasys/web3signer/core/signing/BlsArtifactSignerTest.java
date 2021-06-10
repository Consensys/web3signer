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
package tech.pegasys.web3signer.core.signing;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.web3signer.core.util.IdentifierUtils.normaliseIdentifier;

import tech.pegasys.teku.bls.BLS;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSSignature;

import java.security.SecureRandom;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;

class BlsArtifactSignerTest {
  private static final SecureRandom secureRandom = new SecureRandom();

  @Test
  void publicKeyIsReturnedAsIdentifier() {
    final BLSKeyPair keyPair = BLSKeyPair.random(secureRandom);
    final BlsArtifactSigner blsArtifactSigner = new BlsArtifactSigner(keyPair);
    final String expectedIdentifier = normaliseIdentifier(keyPair.getPublicKey().toString());
    assertThat(blsArtifactSigner.getIdentifier()).isEqualTo(expectedIdentifier);
  }

  @Test
  void signsData() {
    final Bytes message = Bytes.wrap("Hello, world!".getBytes(UTF_8));
    final BLSKeyPair keyPair = BLSKeyPair.random(secureRandom);
    final BLSSignature expectedSignature = BLS.sign(keyPair.getSecretKey(), message);

    final BlsArtifactSigner blsArtifactSigner = new BlsArtifactSigner(keyPair);
    final BlsArtifactSignature signature = blsArtifactSigner.sign(message);

    assertThat(signature.getSignatureData().toString()).isEqualTo(expectedSignature.toString());
  }
}
