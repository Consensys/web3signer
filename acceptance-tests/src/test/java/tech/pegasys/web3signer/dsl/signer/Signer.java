/*
 * Copyright 2019 ConsenSys AG.
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
package tech.pegasys.web3signer.dsl.signer;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.web3signer.dsl.tls.TlsClientHelper.createRequestSpecification;
import static tech.pegasys.web3signer.dsl.utils.WaitUtils.waitFor;

import tech.pegasys.web3signer.dsl.signer.runner.Web3SignerRunner;
import tech.pegasys.web3signer.dsl.tls.ClientTlsConfig;

import java.util.Optional;

import com.atlassian.oai.validator.restassured.OpenApiValidationFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;

public class Signer extends FilecoinJsonRpcEndpoint {

  private static final Logger LOG = LogManager.getLogger();
  public static final String SIGN_ENDPOINT = "/signer/sign/{identifier}";
  static final String SIGNER_PUBLIC_KEYS_PATH = "/signer/publicKeys";

  private final Web3SignerRunner runner;
  private final String hostname;
  private final Vertx vertx;
  private final String urlFormatting;
  private final Optional<ClientTlsConfig> clientTlsConfig;

  public Signer(final SignerConfiguration signerConfig, final ClientTlsConfig clientTlsConfig) {
    super(JSON_RPC_PATH + "/filecoin");
    this.runner = Web3SignerRunner.createRunner(signerConfig);
    this.hostname = signerConfig.hostname();
    this.urlFormatting =
        signerConfig.getServerTlsOptions().isPresent() ? "https://%s:%s" : "http://%s:%s";
    this.clientTlsConfig = Optional.ofNullable(clientTlsConfig);
    vertx = Vertx.vertx();
  }

  public void start() {
    LOG.info("Starting Web3Signer");
    runner.start();
    final String httpUrl = getUrl();
    LOG.info("Http requests being submitted to : {} ", httpUrl);
  }

  public void shutdown() {
    LOG.info("Shutting down Web3Signer");
    vertx.close();
    runner.shutdown();
  }

  public boolean isRunning() {
    return runner.isRunning();
  }

  public int getUpcheckStatus() {
    return requestSpec().baseUri(getUrl()).when().get("/upcheck").then().extract().statusCode();
  }

  public RequestSpecification requestSpec() {
    return given().spec(createRequestSpecification(clientTlsConfig)).baseUri(getUrl());
  }

  public void awaitStartupCompletion() {
    LOG.info("Waiting for Signer to become responsive...");
    final int secondsToWait = Boolean.getBoolean("debugSubProcess") ? 3600 : 30;
    waitFor(secondsToWait, () -> assertThat(getUpcheckStatus()).isEqualTo(200));
    LOG.info("Signer is now responsive");
  }

  @Override
  public String getUrl() {
    return String.format(urlFormatting, hostname, runner.httpPort());
  }

  public String getMetricsUrl() {
    return String.format(urlFormatting, hostname, runner.metricsPort());
  }

  public Response sign(final String publicKey, final Bytes dataToSign) {
    return given()
        .baseUri(getUrl())
        .filter(getOpenApiValidationFilter())
        .contentType(ContentType.JSON)
        .pathParam("identifier", publicKey)
        .body(new JsonObject().put("data", dataToSign.toHexString()).toString())
        .post(SIGN_ENDPOINT);
  }

  public Response callApiPublicKeys(final String keyType) {
    return given()
        .filter(getOpenApiValidationFilter())
        .baseUri(getUrl())
        .get(SIGNER_PUBLIC_KEYS_PATH + "/" + keyType);
  }

  public OpenApiValidationFilter getOpenApiValidationFilter() {
    final String swaggerUrl = getUrl() + "/swagger-ui/eth2signer.yaml";
    return new OpenApiValidationFilter(swaggerUrl);
  }
}
