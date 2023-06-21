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

import tech.pegasys.web3signer.signing.secp256k1.Signature;
import tech.pegasys.web3signer.signing.secp256k1.filebased.CredentialSigner;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Sign;
import org.web3j.crypto.Sign.SignatureData;
import org.web3j.utils.Numeric;

class EthSecpArtifactSignerTest {
  private static final String PRIVATE_KEY =
      "8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63";
  public static final String PUBLIC_KEY =
      "09b02f8a5fddd222ade4ea4528faefc399623af3f736be3c44f03e2df22fb792f3931a4d9573d333ca74343305762a753388c3422a86d98b713fc91c1ea04842";

  @Test
  void publicKeyIsReturnedAsIdentifier() {
    final ECKeyPair ecKeyPair =
        new ECKeyPair(Numeric.toBigInt(PRIVATE_KEY), Numeric.toBigInt(PUBLIC_KEY));
    final Credentials credentials = Credentials.create(ecKeyPair);
    final EthSecpArtifactSigner ethSecpArtifactSigner =
        new EthSecpArtifactSigner(new CredentialSigner(credentials));
    assertThat(ethSecpArtifactSigner.getIdentifier()).isEqualTo("0x" + PUBLIC_KEY);
  }

  @Test
  void signsData() {
    final ECKeyPair ecKeyPair =
        new ECKeyPair(Numeric.toBigInt(PRIVATE_KEY), Numeric.toBigInt(PUBLIC_KEY));
    final Credentials credentials = Credentials.create(ecKeyPair);
    final EthSecpArtifactSigner ethSecpArtifactSigner =
        new EthSecpArtifactSigner(new CredentialSigner(credentials));

    final Bytes message = Bytes.wrap("Hello, world!".getBytes(UTF_8));
    final SecpArtifactSignature signature = ethSecpArtifactSigner.sign(message);

    final SignatureData expectedSignature = Sign.signMessage(message.toArrayUnsafe(), ecKeyPair);
    final Signature signatureData = signature.getSignatureData();
    assertThat(signatureData.getR()).isEqualTo(Numeric.toBigInt(expectedSignature.getR()));
    assertThat(signatureData.getS()).isEqualTo(Numeric.toBigInt(expectedSignature.getS()));
    assertThat(signatureData.getV()).isEqualTo(Numeric.toBigInt(expectedSignature.getV()));
  }
}
