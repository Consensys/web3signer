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
package tech.pegasys.web3signer.tests.slashing;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.web3signer.core.service.http.ArtifactType;
import tech.pegasys.web3signer.core.service.http.Eth2SigningRequestBody;
import tech.pegasys.web3signer.core.service.http.SigningJsonRpcModule;
import tech.pegasys.web3signer.core.signing.KeyType;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.web3signer.tests.AcceptanceTestBase;

@SuppressWarnings({"rawtypes", "UnusedVariable", "UnusedMethod"})
public class SlashingProtectionThroughputAcceptanceTest extends AcceptanceTestBase {


  public static class SettableSupplier implements Supplier<Long> {

    public Long value;

    public SettableSupplier(Long value) {
      this.value = value;
    }

    @Override
    public Long get() {
      return value;
    }
  }


  private static class MyRequest {

    public final String path;
    public final String body;

    public MyRequest(String path, String body) {
      this.path = path;
      this.body = body;
    }
  }


  private static final Logger LOG = LogManager.getLogger();
  private static final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();
  protected final List<BLSKeyPair> keys =
      IntStream.rangeClosed(1, 200).mapToObj(BLSKeyPair::random).collect(Collectors.toList());

  final ObjectMapper eth2InterfaceObjectMapper =
      new ObjectMapper()
          .registerModule(new SigningJsonRpcModule())
          .setSerializationInclusion(Include.NON_NULL);
  final AtomicLong requestCount = new AtomicLong();

  void setupSigner(final Path testDirectory) {
    final SignerConfigurationBuilder builder =
        new SignerConfigurationBuilder()
            .withMode("eth2")
            //.withSlashingEnabled(false)
            //.withSlashingProtectionDbUsername("postgres")
            //.withSlashingProtectionDbPassword("password")
            .withMetricsEnabled(true)
            .withKeyStoreDirectory(testDirectory);

    keys.forEach(
        kp -> {
          final Path keyConfigFile = testDirectory.resolve(kp.getPublicKey().toString() + ".yaml");
          metadataFileHelpers.createUnencryptedYamlFileAt(
              keyConfigFile, kp.getSecretKey().toBytes().toHexString(), KeyType.BLS);
        });

    startSigner(builder.build());
  }

  @Test
  void doIt(@TempDir Path testDirectory)
      throws MalformedURLException, ExecutionException, InterruptedException {

    final List<MyRequest> requestsToSend = Lists.newArrayList();
    final List<CompletableFuture<Integer>> futures = Lists.newArrayList();

    setupSigner(testDirectory);
    final HttpClientOptions clOptions = new HttpClientOptions();
    clOptions.setDefaultHost("localhost");
    clOptions.setDefaultPort(new URL(signer.getUrl()).getPort());
    clOptions.setReusePort(true);
    clOptions.setReuseAddress(true);
    clOptions.setLogActivity(false);
//    clOptions.setPipelining(true);
//    clOptions.setPipeliningLimit(20);
    clOptions.setMaxPoolSize(10);
    final HttpClient vertxClient = Vertx.vertx().createHttpClient(clOptions);
    //final HttpClient javaClient = HttpClient.newHttpClient();


    keys.stream()
        .forEach(
            kp -> {
              IntStream.rangeClosed(1, 1)
                  .forEach(
                      i -> {
                        final Eth2SigningRequestBody request;
                        if (i % 2 == 0) {
                          request =
                              new Eth2SigningRequestBody(
                                  Bytes.fromHexString("0x01"),
                                  ArtifactType.ATTESTATION,
                                  null,
                                  UInt64.valueOf(i),
                                  UInt64.valueOf(i));
                        } else {
                          request =
                              new Eth2SigningRequestBody(
                                  Bytes.fromHexString("0x01"),
                                  ArtifactType.BLOCK,
                                  UInt64.valueOf(i),
                                  null,
                                  null);
                        }

                        try {
                          final String path =
                              signer.getUrl() + "/api/v1/eth2/sign/" + kp.getPublicKey().toString();

                          final MyRequest rq = new MyRequest(path,
                              eth2InterfaceObjectMapper.writeValueAsString(request));
                          requestsToSend.add(rq);
                        } catch (JsonProcessingException e) {
                          throw new RuntimeException("OOPS!");
                        }
                      });
            });

    //send something to create the connection then sleep - then see how we go
    final CompletableFuture<Integer> justWait = sendVertxRequest(vertxClient, requestsToSend.get(0).path, requestsToSend.get(0).body);
    LOG.info("pre");
    final int val = justWait.get(); //connection is NOW running, so hopefully happy
    LOG.info("post");

    final long startTransmission = System.currentTimeMillis();
    requestsToSend.stream().forEach(rq -> {
      //LOG.info("Sending rq {}", rq.path);
      final CompletableFuture<Integer> txFuture = sendVertxRequest(vertxClient, rq.path, rq.body);
      futures.add(txFuture);
//            try {
//        Thread.sleep(2);
//      } catch (InterruptedException e) {
//        e.printStackTrace();
//      }

//      try {
//        txFuture.get();
//      } catch (InterruptedException e) {
//        e.printStackTrace();
//      } catch (ExecutionException e) {
//        e.printStackTrace();
//      }
//      final CompletableFuture<Integer> txFuture = httpClient
//          .sendAsync(rq, BodyHandlers.ofString())
//          .handle((response, throwable) -> getBlsSignature(response, throwable,
//              requestCount.incrementAndGet(), start));


    });
    final long endTransmission = System.currentTimeMillis();
    LOG.info("***** TIME TO SEND ALL RQs = {}", endTransmission - startTransmission);

    CompletableFuture[] itemsArray = new CompletableFuture[futures.size()];
    itemsArray = futures.toArray(itemsArray);
    CompletableFuture.allOf(itemsArray).join();
    LOG.info("Time to get all responses = {}", System.currentTimeMillis() - endTransmission);
  }

  private int getBlsSignature(
      final HttpResponse<String> response, final Throwable throwable, final long rqCount,
      final long startTime) {

    if (throwable != null) {
      throw new RuntimeException(
          "External signer failed to sign due to " + throwable.getMessage(), throwable);
    }

    LOG.info("################ {} --> {} ({})", rqCount, System.currentTimeMillis() - startTime,
        response.body());
    return response.statusCode();
  }

//  HttpRequest createRequest(final Eth2SigningRequestBody body) {
//    final HttpRequest httpRequest =
//        HttpRequest.newBuilder()
//            .uri(new URI(
//                signer.getUrl() + "/api/v1/eth2/sign/" + kp.getPublicKey()
//                    .toString().substring(2)))
//            .timeout(Duration.ofMillis(1200))
//            .header("Content-Type", "application/json")
//            .POST(BodyPublishers.ofString(
//                eth2InterfaceObjectMapper.writeValueAsString(request)))
//            .build();
//  }

  private CompletableFuture<Integer> sendVertxRequest(final HttpClient client, final String path, final String body) {
    final HttpClientRequest request = client.request(HttpMethod.POST, path);
    request.setTimeout(1000);
    request.headers().add("Content-Type", "application/json");
    request.setChunked(false);

    final SettableSupplier startSupplier = new SettableSupplier(0L);
    final CompletableFuture<Integer> txFuture = new CompletableFuture<>();
    request.handler(response -> handleResponse(response, requestCount.incrementAndGet(), startSupplier, txFuture));
    request.end(body, h -> {
    //  LOG.info("SENT {}", path);
      startSupplier.value = System.nanoTime();
    });

    return txFuture;
  }

  private void handleResponse(final HttpClientResponse response, final long id,
      final Supplier<Long> startTime, final CompletableFuture<Integer> future) {
    final long now = System.nanoTime();
    final double msSinceStart = (double)(now - startTime.get()) / 1000000;

    response.bodyHandler(b -> {
      LOG.info("#########   {} --> {}, {}", id, msSinceStart, b.toString());
      future.complete(response.statusCode());
    });
  }
}
