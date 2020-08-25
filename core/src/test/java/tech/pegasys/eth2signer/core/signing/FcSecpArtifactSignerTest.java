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

import tech.pegasys.eth2signer.core.signing.filecoin.FilecoinNetwork;
import tech.pegasys.eth2signer.core.util.ByteUtils;
import tech.pegasys.signers.secp256k1.api.Signature;
import tech.pegasys.signers.secp256k1.filebased.CredentialSigner;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.web3j.crypto.Credentials;

class FcSecpArtifactSignerTest {
  private static final String FC_SECP_PRIVATE_KEY = "WrwTNNmNFDIVDfMQ+dL9UXgKK4p0iYkfXUq5535vsWk=";

  private FcSecpArtifactSigner fcSecpArtifactSigner;

  @BeforeEach
  public void setup() {
    final Bytes fcPrivateKey = Bytes.fromBase64String(FC_SECP_PRIVATE_KEY);
    final Credentials credentials = Credentials.create(fcPrivateKey.toHexString());
    fcSecpArtifactSigner =
        new FcSecpArtifactSigner(new CredentialSigner(credentials, false), FilecoinNetwork.TESTNET);
  }

  @Test
  void identifierReturnsSecpAddress() {
    final String identifier = fcSecpArtifactSigner.getIdentifier();
    final String expectedAddress = "t1656rklyebnscuzs5dee3zwh2xknlqsy7r2ypxei";
    assertThat(identifier).isEqualTo(expectedAddress);
  }

  @ParameterizedTest
  @CsvSource({
    "'',E+f4UauYmVPEL7+PUr/GL819Euww2Qy6/WJ2AQd5x8okioQiu8UbhRwUNjtLxw6ukdTBdLKwA3KkkkghqwSgxAA=",
    "NDI=,4ZDlbvzXtjFnGAnkmnAXImN7KmE+OdW3KbCn+UIHJwY3hEbodjS0VIVBiPUxRWbhJOMao3Sx7GWb4myEsYouJgA="
  })
  void signsData(final String message, final String expectedSignature) {
    final SecpArtifactSignature signature =
        fcSecpArtifactSigner.sign(Bytes.fromBase64String(message));
    assertThat(signature).isInstanceOf(SecpArtifactSignature.class);

    final Signature signatureData = signature.getSignatureData();
    final Bytes signatureValue =
        Bytes.concatenate(
            Bytes32.leftPad(Bytes.wrap(ByteUtils.bigIntegerToBytes(signatureData.getR()))),
            Bytes32.leftPad(Bytes.wrap(ByteUtils.bigIntegerToBytes(signatureData.getS()))),
            Bytes.wrap(ByteUtils.bigIntegerToBytes(signatureData.getV())));
    assertThat(signatureValue.toBase64String()).isEqualTo(expectedSignature);
  }

  @Test
  void verifiesSignatureWasSignedWithKey() {
    final Bytes message = Bytes.wrap("Hello, world!".getBytes(UTF_8));

    final SecpArtifactSignature artifactSignature = fcSecpArtifactSigner.sign(message);
    assertThat(fcSecpArtifactSigner.verify(message, artifactSignature)).isTrue();

    final Credentials otherCredentials =
        Credentials.create("8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63");
    final FcSecpArtifactSigner otherFcSecpArtifactSigner =
        new FcSecpArtifactSigner(
            new CredentialSigner(otherCredentials, false), FilecoinNetwork.TESTNET);
    final SecpArtifactSignature otherArtifactSignature = otherFcSecpArtifactSigner.sign(message);
    assertThat(fcSecpArtifactSigner.verify(message, otherArtifactSignature)).isFalse();
  }
}
