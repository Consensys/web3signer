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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.eth2signer.dsl.utils.WaitUtils.waitFor;

import tech.pegasys.eth2signer.core.http.SigningRequestBody;
import tech.pegasys.eth2signer.crypto.PublicKey;
import tech.pegasys.eth2signer.dsl.HttpResponse;
import tech.pegasys.eth2signer.dsl.signer.runner.Eth2SignerRunner;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.Json;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;

public class Signer {

  private static final Logger LOG = LogManager.getLogger();

  private final Eth2SignerRunner runner;
  private final String hostname;
  private final String urlFormatting = "http://%s:%s";
  private final Vertx vertx;
  private HttpClient httpClient;

  public Signer(final SignerConfiguration signerConfig) {
    this.runner = Eth2SignerRunner.createRunner(signerConfig);
    this.hostname = signerConfig.hostname();
    vertx = Vertx.vertx();
  }

  public void start() {
    LOG.info("Starting Eth2Signer");
    runner.start();
    final String httpUrl = getUrl();
    LOG.info("Http requests being submitted to : {} ", httpUrl);

    final HttpClientOptions options = new HttpClientOptions();
    options.setDefaultHost(hostname);
    options.setDefaultPort(runner.httpJsonRpcPort());
    httpClient = vertx.createHttpClient(options);

    awaitStartupCompletion();
  }

  public void shutdown() {
    LOG.info("Shutting down Eth2Signer");
    vertx.close();
    runner.shutdown();
  }

  public boolean isRunning() {
    return runner.isRunning();
  }

  public boolean isListening() {
    final CompletableFuture<String> responseBodyFuture = new CompletableFuture<>();
    final HttpClientRequest request =
        httpClient.get(
            "/upcheck",
            response -> {
              if (response.statusCode() == HttpResponseStatus.OK.code()) {
                response.bodyHandler(body -> responseBodyFuture.complete(body.toString(UTF_8)));
              } else {
                responseBodyFuture.completeExceptionally(new RuntimeException("Illegal response"));
              }
            });
    request.setChunked(false);
    request.end();

    final String body;
    try {
      body = responseBodyFuture.get();
    } catch (final ExecutionException e) {
      throw (RuntimeException) e.getCause();
    } catch (final InterruptedException e) {
      throw new RuntimeException("Thread was interrupted waiting for Eth2Signer response.");
    }
    return "OK".equals(body);
  }

  public HttpResponse signData(final PublicKey publicKey, final Bytes message, final Bytes domain)
      throws ExecutionException, InterruptedException {
    final SigningRequestBody requestBody =
        new SigningRequestBody(publicKey.toString(), message.toHexString(), domain.toHexString());
    final String httpBody = Json.encode(requestBody);

    final CompletableFuture<HttpResponse> responseBodyFuture = new CompletableFuture<>();
    final HttpClientRequest request =
        httpClient.post(
            "/signer/block",
            response -> {
              response.bodyHandler(
                  body ->
                      responseBodyFuture.complete(
                          new HttpResponse(response.statusCode(), body.toString(UTF_8))));
            });

    request.end(httpBody);

    return responseBodyFuture.get();
  }

  public void awaitStartupCompletion() {
    LOG.info("Waiting for Signer to become responsive...");
    final int secondsToWait = Boolean.getBoolean("debugSubProcess") ? 3600 : 30;
    waitFor(secondsToWait, () -> assertThat(isListening()).isTrue());
    LOG.info("Signer is now responsive");
  }

  public String getUrl() {
    return String.format(urlFormatting, hostname, runner.httpJsonRpcPort());
  }
}
