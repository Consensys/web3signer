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
package tech.pegasys.eth2signer.dsl.signer;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.eth2signer.dsl.tls.TlsClientHelper.createRequestSpecification;
import static tech.pegasys.eth2signer.dsl.utils.WaitUtils.waitFor;
import static tech.pegasys.eth2signer.tests.AcceptanceTestBase.JSON_RPC_PATH;

import tech.pegasys.eth2signer.dsl.lotus.FilecoinJsonRpcEndpoint;
import tech.pegasys.eth2signer.dsl.signer.runner.Eth2SignerRunner;
import tech.pegasys.eth2signer.dsl.tls.ClientTlsConfig;

import java.util.Optional;

import io.restassured.specification.RequestSpecification;
import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Signer extends FilecoinJsonRpcEndpoint {

  private static final Logger LOG = LogManager.getLogger();

  private final Eth2SignerRunner runner;
  private final String hostname;
  private final Vertx vertx;
  private final String urlFormatting;
  private final Optional<ClientTlsConfig> clientTlsConfig;

  public Signer(final SignerConfiguration signerConfig, final ClientTlsConfig clientTlsConfig) {
    super(JSON_RPC_PATH + "/filecoin");
    this.runner = Eth2SignerRunner.createRunner(signerConfig);
    this.hostname = signerConfig.hostname();
    this.urlFormatting =
        signerConfig.getServerTlsOptions().isPresent() ? "https://%s:%s" : "http://%s:%s";
    this.clientTlsConfig = Optional.ofNullable(clientTlsConfig);
    vertx = Vertx.vertx();
  }

  public void start() {
    LOG.info("Starting Eth2Signer");
    runner.start();
    final String httpUrl = getUrl();
    LOG.info("Http requests being submitted to : {} ", httpUrl);
  }

  public void shutdown() {
    LOG.info("Shutting down Eth2Signer");
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
}
