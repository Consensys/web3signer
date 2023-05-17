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
package tech.pegasys.web3signer.tests.tls;

import static io.restassured.RestAssured.given;
import static java.nio.file.Files.writeString;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static tech.pegasys.web3signer.dsl.tls.TlsClientHelper.createRequestSpecification;

import tech.pegasys.web3signer.core.config.TlsOptions;
import tech.pegasys.web3signer.dsl.signer.Signer;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.tls.BasicTlsOptions;
import tech.pegasys.web3signer.dsl.tls.ClientTlsConfig;
import tech.pegasys.web3signer.dsl.tls.TlsCertificateDefinition;
import tech.pegasys.web3signer.dsl.tls.support.BasicClientAuthConstraints;

import java.nio.file.Path;
import java.util.Optional;
import javax.net.ssl.SSLException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ServerSideTlsCaClientAcceptanceTest {

  private static final TlsCertificateDefinition SERVER_CERT =
      TlsCertificateDefinition.loadFromResource("tls/cert1.pfx", "password");
  private static final TlsCertificateDefinition CLIENT_CERT =
      TlsCertificateDefinition.loadFromResource("tls/cert2.pfx", "password2");

  private Signer signer = null;

  @AfterEach
  void cleanup() {
    if (signer != null) {
      signer.shutdown();
      signer = null;
    }
  }

  private Signer createSigner(
      final TlsCertificateDefinition certInCa, final Path testDir, final boolean useConfigFile)
      throws Exception {

    final Path passwordPath = testDir.resolve("keystore.passwd");
    writeString(passwordPath, SERVER_CERT.getPassword());

    final TlsOptions serverOptions =
        new BasicTlsOptions(
            SERVER_CERT.getPkcs12File(),
            passwordPath.toFile(),
            Optional.of(BasicClientAuthConstraints.caOnly()));

    final SignerConfigurationBuilder configBuilder =
        new SignerConfigurationBuilder()
            .withServerTlsOptions(serverOptions)
            .withOverriddenCA(certInCa)
            .withUseConfigFile(useConfigFile)
            .withMode("eth2");

    final ClientTlsConfig clientTlsConfig = new ClientTlsConfig(SERVER_CERT, CLIENT_CERT);

    return new Signer(configBuilder.build(), clientTlsConfig);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void clientWithCertificateNotInCertificateAuthorityCanConnectAndQueryAccounts(
      final boolean useConfigFile, @TempDir final Path tempDir) throws Exception {
    signer = createSigner(CLIENT_CERT, tempDir, useConfigFile);
    signer.start();
    signer.awaitStartupCompletion();

    signer
        .requestSpec()
        .when()
        .get("/upcheck")
        .then()
        .assertThat()
        .statusCode(200)
        .body(equalToIgnoringCase("OK"));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void clientNotInCaFailedToConnectToWeb3Signer(
      final boolean useConfigFile, @TempDir final Path tempDir) throws Exception {
    signer = createSigner(CLIENT_CERT, tempDir, useConfigFile);
    signer.start();
    signer.awaitStartupCompletion();

    // Create a client which presents the server cert (not in CA) - it should fail to connect.
    final ClientTlsConfig clientTlsConfig = new ClientTlsConfig(SERVER_CERT, SERVER_CERT);

    Runnable request =
        () ->
            given()
                .spec(createRequestSpecification(Optional.of(clientTlsConfig)))
                .baseUri(signer.getUrl())
                .when()
                .get("/upcheck")
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalToIgnoringCase("OK"));

    assertThatThrownBy(request::run).isInstanceOf(SSLException.class);
  }
}
