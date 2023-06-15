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

import java.util.Objects;

public class AwsSecret {
  private final String secretValue;
  private final String tagKey;
  private final String tagValue;

  public AwsSecret(final String secretValue, final String tagKey, final String tagValue) {
    this.secretValue = secretValue;
    this.tagKey = tagKey;
    this.tagValue = tagValue;
  }

  public String getSecretValue() {
    return secretValue;
  }

  public String getTagKey() {
    return tagKey;
  }

  public String getTagValue() {
    return tagValue;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AwsSecret that = (AwsSecret) o;
    return secretValue.equals(that.secretValue)
        && tagKey.equals(that.tagKey)
        && tagValue.equals(that.tagValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(secretValue, tagKey, tagValue);
  }
}
