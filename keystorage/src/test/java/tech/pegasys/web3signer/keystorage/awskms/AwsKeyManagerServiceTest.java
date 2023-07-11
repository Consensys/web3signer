/*
 * Copyright 2023 ConsenSys AG.
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
package tech.pegasys.web3signer.keystorage.awskms;

import static org.bouncycastle.util.BigIntegers.asUnsignedByteArray;

import tech.pegasys.web3signer.common.config.AwsAuthenticationMode;
import tech.pegasys.web3signer.common.config.AwsCredentials;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;

class AwsKeyManagerServiceTest {

  @Test
  void testGetECPublicKey() {
    // TODO: Use CI credentials from environment variable
    final AwsCredentials awsCredentials =
        AwsCredentials.builder()
            .withAccessKeyId("test")
            .withSecretAccessKey("test")
            .withSessionToken("test")
            .build();

    final String region = "us-east-1"; // ohio
    final String keyId = "dd39eb13-4208-4a96-b16e-d608c19f7192";
    final URI endpointOverride = URI.create("http://127.0.0.1:4566");
    AwsKeyManagerService awsKeyManagerService =
        new AwsKeyManagerService(
            AwsAuthenticationMode.SPECIFIED,
            awsCredentials,
            region,
            keyId,
            Optional.of(endpointOverride));

    ECPublicKey publicKey = awsKeyManagerService.getECPublicKey();
    System.out.println(toBytes(publicKey).toHexString());

    byte[] sign = awsKeyManagerService.sign("Hello World".getBytes(StandardCharsets.UTF_8));
    System.out.println(Bytes.of(sign));

    sign = awsKeyManagerService.sign("Hello World".getBytes(StandardCharsets.UTF_8));
    System.out.println(Bytes.of(sign));
  }

  private static Bytes toBytes(final ECPublicKey publicKey) {
    final ECPoint ecPoint = publicKey.getW();
    final Bytes xBytes = Bytes32.wrap(asUnsignedByteArray(32, ecPoint.getAffineX()));
    final Bytes yBytes = Bytes32.wrap(asUnsignedByteArray(32, ecPoint.getAffineY()));
    return Bytes.concatenate(xBytes, yBytes);
  }
}
