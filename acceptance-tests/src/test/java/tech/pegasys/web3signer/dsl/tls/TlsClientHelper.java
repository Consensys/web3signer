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
package tech.pegasys.web3signer.dsl.tls;

import java.util.Optional;

import io.restassured.RestAssured;
import io.restassured.authentication.AuthenticationScheme;
import io.restassured.authentication.CertificateAuthSettings;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

public class TlsClientHelper {

  public static RequestSpecification createRequestSpecification(
      final Optional<ClientTlsConfig> clientTlsConfiguration) {
    if (clientTlsConfiguration.isPresent()) {
      final ClientTlsConfig clientTlsConfig = clientTlsConfiguration.get();

      // non-existent keystore needs to be represented by an empty string
      final String keyStoreFilePath =
          clientTlsConfig.getClientCertificateToPresent() == null
              ? ""
              : clientTlsConfig.getClientCertificateToPresent().getPkcs12File().getAbsolutePath();
      final String keyStorePassword =
          clientTlsConfig.getClientCertificateToPresent() == null
              ? ""
              : clientTlsConfig.getClientCertificateToPresent().getPassword();

      final AuthenticationScheme pkcs12 =
          RestAssured.certificate(
              clientTlsConfig.getExpectedTlsServerCert().getPkcs12File().getAbsolutePath(),
              clientTlsConfig.getExpectedTlsServerCert().getPassword(),
              keyStoreFilePath,
              keyStorePassword,
              CertificateAuthSettings.certAuthSettings().keyStoreType("pkcs12"));
      return new RequestSpecBuilder().setAuth(pkcs12).build();
    }
    return new RequestSpecBuilder().build();
  }
}
