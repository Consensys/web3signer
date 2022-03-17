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

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.web3signer.signing.filecoin.FilecoinNetwork;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FcBlsArtifactSignerTest {
  private static final String FC_BLS_PRIVATE_KEY = "z38sVnSEnswoEHFC9e4g/aPk96c1NvXt425UKv/tKz0=";

  private FcBlsArtifactSigner fcBlsArtifactSigner;

  @BeforeEach
  public void setup() {
    final Bytes fcPrivateKey = Bytes.fromBase64String(FC_BLS_PRIVATE_KEY);
    // Filecoin private keys are serialised in little endian so must convert to big endian
    final Bytes32 privateKey = Bytes32.wrap(fcPrivateKey.reverse());
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

  @ParameterizedTest
  @CsvSource({
    "'',qqsXe9+kDAGOh8uKmI6+oGN90ydJ0jRELvxvf2VwAd+g3WRKT/bi65RlhiGTrzzCFP4gY0BiAMe3+ghInVFLs1CXBcYGol+RiPoFUew7UZ6ON1zq5Tjhs5F2vWFj1qt8",
    "NDI=,p/LeJS8KPzuOco2qpqjhvJxFrA5qj++LpeVoActWkDRQTLhuGsnwAvY1kluQ6PntAqISjvR/RU0kzgkR38F8Hm5NuNPsu9/BEiU+IQheNeD7OoG8THq4OuZMbISg7uR/"
  })
  void signsData(final String message, final String expectedSignature) {
    final BlsArtifactSignature signature =
        fcBlsArtifactSigner.sign(Bytes.fromBase64String(message));
    assertThat(signature).isInstanceOf(BlsArtifactSignature.class);
    assertThat(signature.getSignatureData().toBytesCompressed().toBase64String())
        .isEqualTo(expectedSignature);
  }
}
