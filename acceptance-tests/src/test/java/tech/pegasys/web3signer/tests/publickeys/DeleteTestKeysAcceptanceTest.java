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

import tech.pegasys.web3signer.dsl.utils.AwsSecretsManagerUtil;

import java.io.IOException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
public class DeleteTestKeysAcceptanceTest extends KeyIdentifiersAcceptanceTestBase {

  private final String RW_AWS_ACCESS_KEY_ID = System.getenv("RW_AWS_ACCESS_KEY_ID");
  private final String RW_AWS_SECRET_ACCESS_KEY = System.getenv("RW_AWS_SECRET_ACCESS_KEY");

  private final String RO_AWS_ACCESS_KEY_ID = System.getenv("RO_AWS_ACCESS_KEY_ID");
  private final String RO_AWS_SECRET_ACCESS_KEY = System.getenv("RO_AWS_SECRET_ACCESS_KEY");

  private AwsSecretsManagerUtil awsSecretsManagerUtil;

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
  void setup() throws IOException {
    checkEnvironmentVariables();
    awsSecretsManagerUtil =
        new AwsSecretsManagerUtil(AwsKeyPerfAcceptanceTest.AWS_REGION, RW_AWS_ACCESS_KEY_ID, RW_AWS_SECRET_ACCESS_KEY);
  }

  @AfterAll
  void teardown() throws InterruptedException {
    if (awsSecretsManagerUtil != null) {
      awsSecretsManagerUtil.close();
    }
  }

  @Test
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
                //        System.out.print(name);
                if (name.startsWith("signers-aws-integration/")) {
                  //          System.out.print("...deleting");
                  final DeleteSecretRequest secretRequest =
                      DeleteSecretRequest.builder().secretId(name).build();
                  customThreadPool.submit(
                      () -> {
                        secretsManagerClient.deleteSecret(secretRequest);
                        System.err.println("[DELETE KEYS] deleted" + name);
                        System.err.println("[DELETE KEYS] deleted " + count.incrementAndGet());
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
        System.err.print("\n[DELETE KEYS] queuedSubmissionCount=" + queuedSubmissionCount);
        currSubmissionCount = queuedSubmissionCount;
      }
      Thread.sleep(10);
    }
    customThreadPool.shutdown();
    customThreadPool.awaitTermination(30, TimeUnit.SECONDS);
  }
}
