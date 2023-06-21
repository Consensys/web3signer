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
package tech.pegasys.web3signer.signing.filecoin;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.teku.bls.BLSSignature;
import tech.pegasys.web3signer.signing.BlsArtifactSignature;
import tech.pegasys.web3signer.signing.SecpArtifactSignature;
import tech.pegasys.web3signer.signing.secp256k1.Signature;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;
import org.web3j.utils.Numeric;

class FilecoinVerifyTest {
  private static final String DATA = "NDI=";
  private static final String BLS_SIGNATURE =
      "p/LeJS8KPzuOco2qpqjhvJxFrA5qj++LpeVoActWkDRQTLhuGsnwAvY1kluQ6PntAqISjvR/RU0kzgkR38F8Hm5NuNPsu9/BEiU+IQheNeD7OoG8THq4OuZMbISg7uR/";
  private static final String SECP_SIGNATURE =
      "4ZDlbvzXtjFnGAnkmnAXImN7KmE+OdW3KbCn+UIHJwY3hEbodjS0VIVBiPUxRWbhJOMao3Sx7GWb4myEsYouJgA=";

  @Test
  void verifiesBlsSignatureWasSignedWithKey() {
    final Bytes message = Bytes.fromBase64String(DATA);

    final BlsArtifactSignature artifactSignature =
        new BlsArtifactSignature(
            BLSSignature.fromBytesCompressed(Bytes.fromBase64String(BLS_SIGNATURE)));
    final FilecoinAddress filecoinAddress =
        FilecoinAddress.fromString(
            "t3sjhgtrk5fdio52k5lzanh7yy4mj4rqbiowd6odddzprrxejgbjbl2irr3gmpbf7epigf45oy7asljj3v3lva");
    assertThat(FilecoinVerify.verify(filecoinAddress, message, artifactSignature)).isTrue();
  }

  @Test
  void verifiesSecpSignatureWasSignedWithKey() {
    final Bytes message = Bytes.fromBase64String(DATA);

    final SecpArtifactSignature artifactSignature =
        createSecpArtifactSignature(Bytes.fromBase64String(SECP_SIGNATURE));
    final FilecoinAddress filecoinAddress =
        FilecoinAddress.fromString("t1656rklyebnscuzs5dee3zwh2xknlqsy7r2ypxei");
    assertThat(FilecoinVerify.verify(filecoinAddress, message, artifactSignature)).isTrue();
  }

  private SecpArtifactSignature createSecpArtifactSignature(final Bytes signature) {
    final Bytes r = signature.slice(0, 32);
    final Bytes s = signature.slice(32, 32);
    final Bytes v = signature.slice(64);
    return new SecpArtifactSignature(
        new Signature(
            Numeric.toBigInt(v.toArrayUnsafe()),
            Numeric.toBigInt(r.toArrayUnsafe()),
            Numeric.toBigInt(s.toArrayUnsafe())));
  }
}
