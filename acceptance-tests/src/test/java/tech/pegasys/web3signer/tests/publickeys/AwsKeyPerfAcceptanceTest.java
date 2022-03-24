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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AwsKeyPerfAcceptanceTest extends KeyIdentifiersAcceptanceTestBase {

  private static final int NO_KEYS_TO_CREATE = 10;
  private final String RW_AWS_ACCESS_KEY_ID = System.getenv("RW_AWS_ACCESS_KEY_ID");
  private final String RW_AWS_SECRET_ACCESS_KEY = System.getenv("RW_AWS_SECRET_ACCESS_KEY");

  private final String RO_AWS_ACCESS_KEY_ID = System.getenv("RO_AWS_ACCESS_KEY_ID");
  private final String RO_AWS_SECRET_ACCESS_KEY = System.getenv("RO_AWS_SECRET_ACCESS_KEY");

  private final String AWS_REGION = "us-east-2";

  private final List<AwsKeyInfo> awsKeyInfos = new ArrayList<>();
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
  void teardown() {
    if (awsSecretsManagerUtil != null) {
      for (AwsKeyInfo awsKeyInfo : awsKeyInfos) {
        awsSecretsManagerUtil.deleteSecret(awsKeyInfo.secretName);
      }
      awsSecretsManagerUtil.close();
    }
  }

  @Test
  public void specifiedAwsKeysReturnAppropriatePublicKey() {
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
    final String timeTaken =
        DurationFormatUtils.formatDurationHMS(Duration.between(start, Instant.now()).toMillis());
    System.out.println("loaded " + awsKeyInfos.size() + " keys in " + timeTaken);
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
