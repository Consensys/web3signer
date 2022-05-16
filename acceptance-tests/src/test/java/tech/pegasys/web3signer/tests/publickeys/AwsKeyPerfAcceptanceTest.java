/*
 * Copyright 2022 ConsenSys AG.
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
package tech.pegasys.web3signer.tests.publickeys;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.web3signer.BLSTestUtil;
import tech.pegasys.web3signer.dsl.utils.AwsSecretsManagerUtil;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.DeleteSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.ListSecretsResponse;
import software.amazon.awssdk.services.secretsmanager.paginators.ListSecretsIterable;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AwsKeyPerfAcceptanceTest extends KeyIdentifiersAcceptanceTestBase {

  private static final int NO_KEYS_TO_CREATE = 10;
  private final String RW_AWS_ACCESS_KEY_ID = System.getenv("RW_AWS_ACCESS_KEY_ID");
  private final String RW_AWS_SECRET_ACCESS_KEY = System.getenv("RW_AWS_SECRET_ACCESS_KEY");

  private final String RO_AWS_ACCESS_KEY_ID = System.getenv("RO_AWS_ACCESS_KEY_ID");
  private final String RO_AWS_SECRET_ACCESS_KEY = System.getenv("RO_AWS_SECRET_ACCESS_KEY");

//  static final String AWS_REGION = "ap-southeast-2";
  static final String AWS_REGION = "us-east-2";

  private final List<AwsKeyInfo> awsKeyInfos = new ArrayList<>();
  private AwsSecretsManagerUtil awsSecretsManagerUtil;

  private String timeTaken = "";

  private void checkEnvironmentVariables() {
    Assumptions.assumeTrue(
        RW_AWS_ACCESS_KEY_ID != null, "Set RW_AWS_ACCESS_KEY_ID environment variable");
    Assumptions.assumeTrue(
        RW_AWS_SECRET_ACCESS_KEY != null, "Set RW_AWS_SECRET_ACCESS_KEY environment variable");
    Assumptions.assumeTrue(
        RO_AWS_ACCESS_KEY_ID != null, "Set RO_AWS_ACCESS_KEY_ID environment variable");
    Assumptions.assumeTrue(
        RO_AWS_SECRET_ACCESS_KEY != null, "Set RO_AWS_SECRET_ACCESS_KEY environment variable");
  }

  @BeforeAll
  void setup() {
    checkEnvironmentVariables();
    awsSecretsManagerUtil =
        new AwsSecretsManagerUtil(AWS_REGION, RW_AWS_ACCESS_KEY_ID, RW_AWS_SECRET_ACCESS_KEY);

    for (int i = 0; i < NO_KEYS_TO_CREATE; i++) {
      final BLSKeyPair blsKeyPair = BLSTestUtil.randomKeyPair(i);
      final String privateKeyInHex = blsKeyPair.getSecretKey().toBytes().toHexString();
      final String awsSecretName = awsSecretsManagerUtil.createSecret(privateKeyInHex);
      awsKeyInfos.add(new AwsKeyInfo(awsSecretName, blsKeyPair));
    }
  }

  @AfterAll
  void teardown() throws InterruptedException {
    if (awsSecretsManagerUtil != null) {
      ForkJoinPool customThreadPool = new ForkJoinPool(10);
      AtomicInteger count = new AtomicInteger(0);

      for (AwsKeyInfo awsKeyInfo : awsKeyInfos) {
        customThreadPool.submit(
            () -> {
              try {
                System.err.println("[PERF TEST] deleting " + awsKeyInfo.secretName);
                awsSecretsManagerUtil.deleteSecret(awsKeyInfo.secretName);
                System.err.println("[PERF TEST] deleted " + count.incrementAndGet());
              } catch (Exception e) {
                System.err.println(e.getMessage());
                System.err.println(e.getCause().getMessage());
              }
            });
      }

      int currSubmissionCount = customThreadPool.getQueuedSubmissionCount();
      while (!customThreadPool.isQuiescent()) {
        if (customThreadPool.getQueuedSubmissionCount() == currSubmissionCount) {
          System.err.print(".");
        } else {
          final int queuedSubmissionCount = customThreadPool.getQueuedSubmissionCount();
          System.err.print("\n [PERF TEST] queuedSubmissionCount=" + queuedSubmissionCount);
          currSubmissionCount = queuedSubmissionCount;
        }
        Thread.sleep(10);
      }
      customThreadPool.shutdown();
      customThreadPool.awaitTermination(30, TimeUnit.SECONDS);
//      System.err.println("\n[PERF TEST] Second delete pass...");
//      deleteAllTestKeys();
      awsSecretsManagerUtil.close();
      System.err.println("\n[PERF TEST] loaded " + awsKeyInfos.size() + " keys in " + timeTaken);
    }
  }

  public void deleteAllTestKeys() throws InterruptedException {
    AtomicInteger count = new AtomicInteger(0);
    final SecretsManagerClient secretsManagerClient =
        awsSecretsManagerUtil.getSecretsManagerClient();
    final ListSecretsIterable listSecretsResponses = secretsManagerClient.listSecretsPaginator();

    ForkJoinPool customThreadPool = new ForkJoinPool(10);

    for (ListSecretsResponse response : listSecretsResponses) {
      response.secretList().stream()
          .forEach(
              secretListEntry -> {
                final String name = secretListEntry.name();
                //        System.err.print(name);
                if (name.startsWith("signers-aws-integration/")) {
                  //          System.err.print("...deleting");
                  final DeleteSecretRequest secretRequest =
                      DeleteSecretRequest.builder().secretId(name).build();
                  customThreadPool.submit(
                      () -> {
                        secretsManagerClient.deleteSecret(secretRequest);
                        System.err.println("[PERF TEST] deleted " + name);
                        System.err.println("[PERF TEST] deleted " + count.incrementAndGet());
                      });
                }
              });
    }
    int currSubmissionCount = customThreadPool.getQueuedSubmissionCount();
    while (customThreadPool.getQueuedSubmissionCount() > 0) {
      if (customThreadPool.getQueuedSubmissionCount() == currSubmissionCount) {
        System.err.print(".");
      } else {
        final int queuedSubmissionCount = customThreadPool.getQueuedSubmissionCount();
        System.err.print("\n[PERF TEST] queuedSubmissionCount=" + queuedSubmissionCount);
        currSubmissionCount = queuedSubmissionCount;
      }
      Thread.sleep(10);
    }
    customThreadPool.shutdown();
    customThreadPool.awaitTermination(30, TimeUnit.SECONDS);
  }

  @Test
  public void specifiedAwsKeysReturnAppropriatePublicKey() throws InterruptedException {
    for (AwsKeyInfo awsKeyInfo : awsKeyInfos) {
      final String publicKey = awsKeyInfo.blsKeyPair.getPublicKey().toString();
      metadataFileHelpers.createAwsYamlFileAt(
          testDirectory.resolve(publicKey + ".yaml"),
          AWS_REGION,
          RO_AWS_ACCESS_KEY_ID,
          RO_AWS_SECRET_ACCESS_KEY,
          awsKeyInfo.secretName);
    }

    final Instant start = Instant.now();
    initAndStartSigner("eth2");
    timeTaken =
        DurationFormatUtils.formatDurationHMS(Duration.between(start, Instant.now()).toMillis());
    System.err.println("[PERF TEST] loaded " + awsKeyInfos.size() + " keys in " + timeTaken);
  }

  public static class AwsKeyInfo {
    public String secretName;
    public BLSKeyPair blsKeyPair;

    public AwsKeyInfo(final String secretName, final BLSKeyPair blsKeyPair) {
      this.secretName = secretName;
      this.blsKeyPair = blsKeyPair;
    }
  }
}
