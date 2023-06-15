/*
 * Copyright 2023 ConsenSys AG.
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
package tech.pegasys.web3signer.keystorage.aws;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SecretsMaps {
  static final String SECRET_NAME_PREFIX_A = "signers-aws-integration/a/";
  static final String SECRET_NAME_PREFIX_B = "signers-aws-integration/b/";

  private final Map<String, AwsSecret> prefixASecretsMap;
  private final Map<String, AwsSecret> prefixBSecretsMap;
  private final Map<String, AwsSecret> allSecretsMap;

  public SecretsMaps() {
    final Map<String, AwsSecret> secretMapA = new HashMap<>();
    final Map<String, AwsSecret> secretMapB = new HashMap<>();
    final Map<String, AwsSecret> allSecretsMap = new HashMap<>();

    for (int i = 1; i <= 4; i++) {
      final AwsSecret awsSecret = computeSecretValue(i);
      secretMapA.put(computeMapAKey(i), awsSecret);
      secretMapB.put(computeMapBKey(i), awsSecret);
    }
    allSecretsMap.putAll(secretMapA);
    allSecretsMap.putAll(secretMapB);

    this.prefixASecretsMap = Collections.unmodifiableMap(secretMapA);
    this.prefixBSecretsMap = Collections.unmodifiableMap(secretMapB);
    this.allSecretsMap = Collections.unmodifiableMap(allSecretsMap);
  }

  public Map<String, AwsSecret> getPrefixASecretsMap() {
    return prefixASecretsMap;
  }

  public Map<String, AwsSecret> getPrefixBSecretsMap() {
    return prefixBSecretsMap;
  }

  public Map<String, AwsSecret> getAllSecretsMap() {
    return allSecretsMap;
  }

  private static String computeMapAKey(final int i) {
    return String.format("%ssecret%d", SECRET_NAME_PREFIX_A, i);
  }

  private static String computeMapBKey(final int i) {
    return String.format("%ssecret%d", SECRET_NAME_PREFIX_B, i);
  }

  private static AwsSecret computeSecretValue(final int i) {
    final String value = String.format("secret-value%d", i);
    final AwsSecret awsSecret;
    switch (i) {
      case 1:
        awsSecret = new AwsSecret(value, "tagKey1", "tagValA");
        break;
      case 2:
        awsSecret = new AwsSecret(value, "tagKey1", "tagValB");
        break;
      case 3:
        awsSecret = new AwsSecret(value, "tagKey2", "tagValC");
        break;
      default:
        awsSecret = new AwsSecret(value, "tagKey2", "tagValB");
        break;
    }
    return awsSecret;
  }
}
