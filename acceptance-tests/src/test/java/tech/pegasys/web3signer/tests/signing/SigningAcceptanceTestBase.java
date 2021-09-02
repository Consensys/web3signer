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
package tech.pegasys.web3signer.tests.signing;

import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.tests.AcceptanceTestBase;

import java.nio.file.Path;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.io.TempDir;

public class SigningAcceptanceTestBase extends AcceptanceTestBase {
  protected @TempDir Path testDirectory;
  private static final Long MINIMAL_ALTAIR_FORK = 0L;

  protected void setupEth1Signer() {
    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withKeyStoreDirectory(testDirectory).withMode("eth1");
    startSigner(builder.build());
  }

  protected void setupFilecoinSigner() {
    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withKeyStoreDirectory(testDirectory).withMode("filecoin");
    startSigner(builder.build());
  }

  protected void setupEth2Signer() {
    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder
        .withKeyStoreDirectory(testDirectory)
        .withMode("eth2")
        .withAltairForkEpoch(MINIMAL_ALTAIR_FORK);
    startSigner(builder.build());
  }

  protected void setupEth2SignerMinimal() {
    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder
        .withKeyStoreDirectory(testDirectory)
        .withMode("eth2")
        .withNetwork("minimal")
        .withAltairForkEpoch(MINIMAL_ALTAIR_FORK);
    startSigner(builder.build());
  }

  protected void setupEth2SignerMinimalWithoutAltairFork() {
    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withKeyStoreDirectory(testDirectory).withMode("eth2").withNetwork("minimal");
    startSigner(builder.build());
  }

  protected Bytes verifyAndGetSignatureResponse(final Response response) {
    return verifyAndGetSignatureResponse(response, ContentType.TEXT);
  }

  protected Bytes verifyAndGetSignatureResponse(
      final Response response, final ContentType expectedContentType) {
    response.then().contentType(expectedContentType).statusCode(200);
    final String signature =
        expectedContentType == ContentType.JSON
            ? response.body().jsonPath().getString("signature")
            : response.body().print();
    return Bytes.fromHexString(signature);
  }
}
