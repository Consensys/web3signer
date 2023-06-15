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

import software.amazon.awssdk.regions.Region;

public class AwsKeyIdentifier {
  private final String accessKeyId;
  private final Region region;

  public AwsKeyIdentifier(final String accessKeyId, final Region region) {
    this.accessKeyId = accessKeyId;
    this.region = region;
  }

  public String getAccessKeyId() {
    return accessKeyId;
  }

  public Region getRegion() {
    return region;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AwsKeyIdentifier that = (AwsKeyIdentifier) o;
    return accessKeyId.equals(that.accessKeyId) && region.equals(that.region);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessKeyId, region);
  }
}
