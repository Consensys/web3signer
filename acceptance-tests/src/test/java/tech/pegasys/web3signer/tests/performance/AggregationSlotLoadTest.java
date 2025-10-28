/*
 * Copyright 2025 ConsenSys AG.
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
package tech.pegasys.web3signer.tests.performance;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static tech.pegasys.web3signer.dsl.signer.Signer.ETH_2_INTERFACE_OBJECT_MAPPER;

import tech.pegasys.teku.bls.BLS;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSSignature;
import tech.pegasys.teku.infrastructure.async.SafeFuture;
import tech.pegasys.teku.infrastructure.async.ThrottlingTaskQueue;
import tech.pegasys.teku.infrastructure.async.ThrottlingTaskQueueWithPriority;
import tech.pegasys.teku.infrastructure.metrics.StubMetricsSystem;
import tech.pegasys.teku.infrastructure.metrics.TekuMetricCategory;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.SpecMilestone;
import tech.pegasys.teku.spec.TestSpecFactory;
import tech.pegasys.teku.spec.datastructures.state.Fork;
import tech.pegasys.teku.spec.networks.Eth2Network;
import tech.pegasys.teku.spec.signatures.SigningRootUtil;
import tech.pegasys.web3signer.BLSTestUtil;
import tech.pegasys.web3signer.core.service.http.ArtifactType;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.AggregationSlot;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.Eth2SigningRequestBody;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.ForkInfo;
import tech.pegasys.web3signer.dsl.utils.Eth2SigningRequestBodyBuilder;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.web3signer.signing.KeyType;
import tech.pegasys.web3signer.tests.signing.SigningAcceptanceTestBase;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AggregationSlotLoadTest extends SigningAcceptanceTestBase {
  private static final Logger LOG = LogManager.getLogger();
  private static final String GENESIS_VALIDATORS_ROOT =
      "0x04700007fabc8282644aed6d1c7c9e21d38a03a0c4ba193f3afe428824b3a673";

  private static final int NUM_VALIDATORS = 30000;
  private static final int SLOTS_PER_BATCH = 4; // From DEFAULT_SLOT_BATCHING_OPTIONS [1]
  private static final long DELAY_BETWEEN_BATCHES_MS =
      500; // From DEFAULT_SLOT_BATCHING_OPTIONS [1]
  private static final MetadataFileHelpers METADATA_FILE_HELPERS = new MetadataFileHelpers();

  private List<BLSKeyPair> validatorKeyPairs;
  private static final SpecMilestone SPEC_MILESTONE = SpecMilestone.DENEB;
  private static final Spec SPEC = TestSpecFactory.createMinimalDeneb();
  private static final SigningRootUtil SIGNING_ROOT_UTIL = new SigningRootUtil(SPEC);
  private static final HttpClient HTTP_CLIENT =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  @BeforeEach
  void setup() throws Exception {
    LOG.info("Generating {} validator key pairs", NUM_VALIDATORS);
    validatorKeyPairs = new ArrayList<>();

    // Generate and register all validator key pairs
    for (int i = 0; i < NUM_VALIDATORS; i++) {
      final BLSKeyPair keyPair = BLSTestUtil.randomKeyPair(i);
      validatorKeyPairs.add(keyPair);

      // Register key with Web3Signer
      final String configFilename = keyPair.getPublicKey().toString().substring(2);
      final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
      METADATA_FILE_HELPERS.createUnencryptedYamlFileAt(
          keyConfigFile, keyPair.getSecretKey().toBytes().toHexString(), KeyType.BLS);
    }

    // Start Web3Signer with all keys loaded
    setupEth2Signer(Eth2Network.MINIMAL, SPEC_MILESTONE);

    LOG.info("Web3Signer started with {} validators", NUM_VALIDATORS);

    // Verify all keys are loaded
    verifyAllKeysLoaded();
  }

  private void verifyAllKeysLoaded() {
    LOG.info("Verifying {} public keys are loaded", NUM_VALIDATORS);

    final Response response = signer.callApiPublicKeys(KeyType.BLS);
    response.then().statusCode(200).contentType(ContentType.JSON);

    // Extract public keys from response
    final List<String> loadedPublicKeys = response.jsonPath().getList("", String.class);

    LOG.info("Web3Signer reports {} public keys loaded", loadedPublicKeys.size());

    // Verify count matches
    assertThat(loadedPublicKeys)
        .as("All {} validator keys should be loaded", NUM_VALIDATORS)
        .hasSize(NUM_VALIDATORS);

    // Optionally verify specific keys are present
    for (BLSKeyPair keyPair : validatorKeyPairs) {
      assertThat(loadedPublicKeys)
          .as("Public key {} should be loaded", keyPair.getPublicKey())
          .contains(keyPair.getPublicKey().toString());
    }

    LOG.info("Successfully verified all {} public keys are loaded", NUM_VALIDATORS);
  }

  /**
   * Tests aggregation slot signing with the same batching pattern used by Teku's
   * AttestationDutyBatchSchedulingStrategy [2]: 4 slots per batch with 500ms delays. This
   * reproduces the scheduling pattern that correlates with the timeout issue.
   */
  @Test
  void testAggregationSlotBatchedLoad() throws Exception {
    LOG.info("Starting batched load test with {} validators", NUM_VALIDATORS);

    long startTime = System.currentTimeMillis();
    AtomicInteger timeouts = new AtomicInteger(0);
    AtomicInteger failures = new AtomicInteger(0);
    AtomicInteger successCount = new AtomicInteger(0);
    ExecutorService executor = Executors.newFixedThreadPool(100);

    try {
      int validatorsPerSlot = NUM_VALIDATORS / 32;

      for (int batch = 0; batch < 32 / SLOTS_PER_BATCH; batch++) {
        LOG.info("Scheduling batch {} of {}", batch + 1, 32 / SLOTS_PER_BATCH);
        List<CompletableFuture<Void>> batchFutures = new ArrayList<>();

        // Schedule all validators for this batch (4 slots)
        for (int slotInBatch = 0; slotInBatch < SLOTS_PER_BATCH; slotInBatch++) {
          final long slot = batch * SLOTS_PER_BATCH + slotInBatch;

          for (int i = 0; i < validatorsPerSlot; i++) {
            final int validatorIdx = (int) (slot * validatorsPerSlot + i);
            if (validatorIdx >= NUM_VALIDATORS) break;

            batchFutures.add(
                CompletableFuture.runAsync(
                    () -> {
                      try {
                        sendAggregationSlotSigningRequest(validatorIdx, slot);
                        successCount.incrementAndGet();
                      } catch (java.net.http.HttpTimeoutException e) {
                        timeouts.incrementAndGet();
                        LOG.warn("Timeout on validator {} slot {}", validatorIdx, slot);
                      } catch (Exception e) {
                        failures.incrementAndGet();
                        LOG.error(
                            "Request failed for validator {} slot {}: {}",
                            validatorIdx,
                            slot,
                            e.getMessage());
                      }
                    },
                    executor));
          }
        }

        // Wait for all validators in this batch to complete
        CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0]))
            .exceptionally(e -> null)
            .join();

        LOG.info(
            "Batch {} completed. Success: {}, Failures: {}, Timeouts: {}",
            batch + 1,
            successCount.get(),
            failures.get(),
            timeouts.get());

        // Delay between batches (matches DEFAULT_SLOT_BATCHING_OPTIONS)
        Thread.sleep(DELAY_BETWEEN_BATCHES_MS);
      }
    } finally {
      executor.shutdown();
      executor.awaitTermination(5, TimeUnit.MINUTES);
    }

    long duration = System.currentTimeMillis() - startTime;
    LOG.info(
        "Test completed: {} requests, {} successes, {} failures, {} timeouts in {}ms",
        NUM_VALIDATORS,
        successCount.get(),
        failures.get(),
        timeouts.get(),
        duration);

    assertThat(timeouts.get()).as("No aggregation slot signing requests should timeout").isZero();
    assertThat(failures.get()).as("No aggregation slot signing requests should fail").isZero();
  }

  /**
   * Stress test without batching delays - sends all requests concurrently. Use this baseline to
   * compare against the batched test and determine if batching actually helps or if it creates a
   * thundering herd effect.
   */
  @Test
  void testAggregationSlotConcurrentLoad() throws Exception {
    LOG.info(
        "Starting concurrent load test with {} validators (no batching delays)", NUM_VALIDATORS);

    long startTime = System.currentTimeMillis();
    AtomicInteger timeouts = new AtomicInteger(0);
    AtomicInteger failures = new AtomicInteger(0);
    AtomicInteger successCount = new AtomicInteger(0);
    ExecutorService executor = Executors.newFixedThreadPool(100);

    try {
      List<CompletableFuture<Void>> allFutures = new ArrayList<>();

      // Send all 30,000 requests concurrently without batching delays
      for (int i = 0; i < NUM_VALIDATORS; i++) {
        final int validatorIdx = i;
        final long slot = i % 32; // Distribute across 32 slots

        allFutures.add(
            CompletableFuture.runAsync(
                () -> {
                  try {
                    sendAggregationSlotSigningRequest(validatorIdx, slot);
                    successCount.incrementAndGet();
                  } catch (java.net.http.HttpTimeoutException e) {
                    timeouts.incrementAndGet();
                    LOG.warn("Timeout on validator {} slot {}", validatorIdx, slot);
                  } catch (Exception e) {
                    failures.incrementAndGet();
                    LOG.error(
                        "Request failed for validator {} slot {}: {}",
                        validatorIdx,
                        slot,
                        e.getMessage());
                  }
                },
                executor));
      }

      // Wait for all requests to complete
      CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0]))
          .exceptionally(e -> null)
          .join();
    } finally {
      executor.shutdown();
      executor.awaitTermination(5, TimeUnit.MINUTES);
    }

    long duration = System.currentTimeMillis() - startTime;
    LOG.info(
        "Concurrent test completed: {} requests, {} successes, {} failures, {} timeouts in {}ms",
        NUM_VALIDATORS,
        successCount.get(),
        failures.get(),
        timeouts.get(),
        duration);

    assertThat(timeouts.get()).as("No aggregation slot signing requests should timeout").isZero();
    assertThat(failures.get()).as("No aggregation slot signing requests should fail").isZero();
  }

  @Test
  void testAggregationSlotWithLimitedConcurrency() throws Exception {
    LOG.info("Starting load test with limited concurrent request limit");
    StubMetricsSystem metricsSystem = new StubMetricsSystem();
    long startTime = System.currentTimeMillis();
    AtomicInteger timeouts = new AtomicInteger(0);
    AtomicInteger failures = new AtomicInteger(0);
    AtomicInteger successCount = new AtomicInteger(0);

    // Simulate Teku's external signer concurrent request limit
    final int CONCURRENT_REQUEST_LIMIT = 32;
    ThrottlingTaskQueueWithPriority taskQueue =
        ThrottlingTaskQueueWithPriority.create(
            CONCURRENT_REQUEST_LIMIT,
            ThrottlingTaskQueue.DEFAULT_MAXIMUM_QUEUE_SIZE,
            metricsSystem,
            TekuMetricCategory.VALIDATOR,
            "test_external_signer_request_queue_size",
            "test_external_signer_request_queue_rejected");

    try {
      int validatorsPerSlot = NUM_VALIDATORS / 32;
      int remainingValidators = NUM_VALIDATORS % 32;

      LOG.info(
          "Validators per slot: {}, Remaining validators: {}",
          validatorsPerSlot,
          remainingValidators);

      for (int batch = 0; batch < 32 / SLOTS_PER_BATCH; batch++) {
        LOG.info("Scheduling batch {} of {}", batch + 1, 32 / SLOTS_PER_BATCH);
        List<CompletableFuture<Void>> batchFutures = new ArrayList<>();

        for (int slotInBatch = 0; slotInBatch < SLOTS_PER_BATCH; slotInBatch++) {
          final long slot = batch * SLOTS_PER_BATCH + slotInBatch;
          final boolean isLastSlot = slot == 31; // Last slot in epoch

          // Calculate validators for this slot (last slot gets remaining validators)
          final int validatorsForSlot =
              isLastSlot ? validatorsPerSlot + remainingValidators : validatorsPerSlot;

          for (int i = 0; i < validatorsForSlot; i++) {
            final int validatorIdx = (int) (slot * validatorsPerSlot + i);
            if (validatorIdx >= NUM_VALIDATORS) break;

            // Queue task with priority=true (like aggregation slot signing)
            batchFutures.add(
                taskQueue
                    .queueTask(
                        () ->
                            SafeFuture.of(
                                () -> {
                                  try {
                                    sendAggregationSlotSigningRequest(validatorIdx, slot);
                                    successCount.incrementAndGet();
                                    return null;
                                  } catch (java.net.http.HttpTimeoutException e) {
                                    timeouts.incrementAndGet();
                                    LOG.warn("Timeout on validator {} slot {}", validatorIdx, slot);
                                    throw e;
                                  } catch (Exception e) {
                                    failures.incrementAndGet();
                                    LOG.error(
                                        "Request failed for validator {} slot {}: {}",
                                        validatorIdx,
                                        slot,
                                        e.getMessage());
                                    throw e;
                                  }
                                }),
                        true) // prioritize=true
                    .thenApply(__ -> null));
          }
        }

        // Wait for all validators in this batch to complete
        CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0]))
            .exceptionally(e -> null)
            .join();

        LOG.info(
            "Batch {} completed. Success: {}, Failures: {}, Timeouts: {}",
            batch + 1,
            successCount.get(),
            failures.get(),
            timeouts.get());

        // Delay between batches (matches DEFAULT_SLOT_BATCHING_OPTIONS)
        Thread.sleep(DELAY_BETWEEN_BATCHES_MS);
      }
    } finally {
      LOG.info("Task queue final state - Queued tasks: {}", taskQueue.getQueuedTasksCount());
    }

    long duration = System.currentTimeMillis() - startTime;
    LOG.info(
        "Test completed: {} requests, {} successes, {} failures, {} timeouts in {}ms",
        NUM_VALIDATORS,
        successCount.get(),
        failures.get(),
        timeouts.get(),
        duration);

    assertThat(timeouts.get()).as("No aggregation slot signing requests should timeout").isZero();
    assertThat(failures.get()).as("No aggregation slot signing requests should fail").isZero();
  }

  private void sendAggregationSlotSigningRequest(final int validatorIdx, final long slot)
      throws Exception {
    final BLSKeyPair keyPair = validatorKeyPairs.get(validatorIdx);

    // Create valid aggregation slot signing request
    final Eth2SigningRequestBody request = createAggregationSlotRequest(slot);

    // Send to Web3Signer via custom HTTP client
    final Bytes signature =
        sendSigningRequestViaHttpClient(keyPair.getPublicKey().toString(), request);

    final BLSSignature expectedSignature = BLS.sign(keyPair.getSecretKey(), request.signingRoot());

    assertThat(signature).isEqualTo(expectedSignature.toBytesCompressed());
  }

  private Eth2SigningRequestBody createAggregationSlotRequest(final long slot) {
    final ForkInfo forkInfo = getForkInfoForSlot(slot);
    final AggregationSlot aggregationSlot = new AggregationSlot(UInt64.valueOf(slot));

    final Bytes signingRoot =
        SIGNING_ROOT_UTIL.signingRootForSignAggregationSlot(
            aggregationSlot.getSlot(), forkInfo.asInternalForkInfo());

    return Eth2SigningRequestBodyBuilder.anEth2SigningRequestBody()
        .withType(ArtifactType.AGGREGATION_SLOT)
        .withSigningRoot(signingRoot)
        .withForkInfo(forkInfo)
        .withAggregationSlot(aggregationSlot)
        .build();
  }

  private ForkInfo getForkInfoForSlot(final long slot) {
    final UInt64 slotValue = UInt64.valueOf(slot);
    final UInt64 epoch = SPEC.computeEpochAtSlot(slotValue);
    final Fork fork = SPEC.getForkSchedule().getFork(epoch);
    final Bytes32 genesisValidatorsRoot = Bytes32.fromHexString(GENESIS_VALIDATORS_ROOT);
    tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.Fork web3SignerFork =
        new tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.Fork(fork);
    return new ForkInfo(web3SignerFork, genesisValidatorsRoot);
  }

  private Bytes sendSigningRequestViaHttpClient(
      final String publicKey, final Eth2SigningRequestBody request) throws Exception {

    // Serialize request body
    final String jsonBody = ETH_2_INTERFACE_OBJECT_MAPPER.writeValueAsString(request);

    // Build HTTP request
    final String url = String.format("%s/api/v1/eth2/sign/%s", signer.getUrl(), publicKey);

    HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

    // Send request
    HttpResponse<String> response =
        HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 200) {
      throw new Exception("HTTP " + response.statusCode() + ": " + response.body());
    }

    // Parse signature from response
    final SignatureResponse signatureResponse =
        ETH_2_INTERFACE_OBJECT_MAPPER.readValue(response.body(), SignatureResponse.class);

    return Bytes.fromHexString(signatureResponse.signature);
  }

  private record SignatureResponse(String signature) {}
}
