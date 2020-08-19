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

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.eth2signer.core.signing.filecoin.FilecoinNetwork;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSSecretKey;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FcBlsArtifactSignerTest {
  private static final String FC_BLS_PRIVATE_KEY = "z38sVnSEnswoEHFC9e4g/aPk96c1NvXt425UKv/tKz0=";

  private FcBlsArtifactSigner fcBlsArtifactSigner;

  @BeforeEach
  public void setup() {
    final Bytes fcPrivateKey = Bytes.fromBase64String(FC_BLS_PRIVATE_KEY);
    // Filecoin private keys are serialised in little endian so must convert to big endian
    final Bytes privateKey = fcPrivateKey.reverse();
    final BLSKeyPair keyPair = new BLSKeyPair(BLSSecretKey.fromBytes(privateKey));
    fcBlsArtifactSigner = new FcBlsArtifactSigner(keyPair, FilecoinNetwork.TESTNET);
  }

  @Test
  void identifierReturnsBlsAddress() {
    final String identifier = fcBlsArtifactSigner.getIdentifier();
    final String expectedBlsAddress =
        "t3sjhgtrk5fdio52k5lzanh7yy4mj4rqbiowd6odddzprrxejgbjbl2irr3gmpbf7epigf45oy7asljj3v3lva";
    assertThat(identifier).isEqualTo(expectedBlsAddress);
  }
}
