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
package tech.pegasys.web3signer.signing.config;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class AwsSecretsManagerParametersBuilder {
  private AwsAuthenticationMode authenticationMode = AwsAuthenticationMode.SPECIFIED;
  private String accessKeyId;
  private String secretAccessKey;
  private String region;
  private List<String> prefixesFilter = Collections.emptyList();
  private List<String> tagsNameFilters = Collections.emptyList();
  private List<String> tagsValueFilters = Collections.emptyList();
  private long cacheMaximumSize = 1;

  private AwsSecretsManagerParametersBuilder() {}

  public static AwsSecretsManagerParametersBuilder anAwsSecretsManagerParameters() {
    return new AwsSecretsManagerParametersBuilder();
  }

  public AwsSecretsManagerParametersBuilder withAuthenticationMode(
      AwsAuthenticationMode authenticationMode) {
    this.authenticationMode = authenticationMode;
    return this;
  }

  public AwsSecretsManagerParametersBuilder withAccessKeyId(String accessKeyId) {
    this.accessKeyId = accessKeyId;
    return this;
  }

  public AwsSecretsManagerParametersBuilder withSecretAccessKey(String secretAccessKey) {
    this.secretAccessKey = secretAccessKey;
    return this;
  }

  public AwsSecretsManagerParametersBuilder withRegion(String region) {
    this.region = region;
    return this;
  }

  public AwsSecretsManagerParametersBuilder withPrefixesFilter(List<String> prefixesFilter) {
    this.prefixesFilter = prefixesFilter;
    return this;
  }

  public AwsSecretsManagerParametersBuilder withTagsNameFilters(List<String> tagsNameFilters) {
    this.tagsNameFilters = tagsNameFilters;
    return this;
  }

  public AwsSecretsManagerParametersBuilder withTagsValueFilters(List<String> tagsValueFilters) {
    this.tagsValueFilters = tagsValueFilters;
    return this;
  }

  public AwsSecretsManagerParametersBuilder withCacheMaximumSize(long cacheMaximumSize) {
    this.cacheMaximumSize = cacheMaximumSize;
    return this;
  }

  public AwsSecretsManagerParameters build() {
    if (authenticationMode == AwsAuthenticationMode.SPECIFIED) {
      if (accessKeyId == null) {
        throw new IllegalArgumentException("accessKeyId is required");
      }

      if (secretAccessKey == null) {
        throw new IllegalArgumentException("secretAccessKey is required");
      }

      if (region == null) {
        throw new IllegalArgumentException("region is required");
      }
    }

    return new TestAwsSecretsManagerParameters(
        authenticationMode,
        accessKeyId,
        secretAccessKey,
        region,
        prefixesFilter,
        tagsNameFilters,
        tagsValueFilters,
        cacheMaximumSize);
  }

  private static class TestAwsSecretsManagerParameters implements AwsSecretsManagerParameters {
    private AwsAuthenticationMode authenticationMode = AwsAuthenticationMode.SPECIFIED;
    private String accessKeyId;
    private String secretAccessKey;
    private String region;
    private List<String> prefixesFilter = Collections.emptyList();
    private List<String> tagsNameFilters = Collections.emptyList();
    private List<String> tagsValueFilters = Collections.emptyList();
    private long cacheMaximumSize = 1;

    TestAwsSecretsManagerParameters(
        final AwsAuthenticationMode authenticationMode,
        final String accessKeyId,
        final String secretAccessKey,
        final String region,
        final List<String> prefixesFilter,
        final List<String> tagsNameFilters,
        final List<String> tagsValueFilters,
        final long cacheMaximumSize) {
      this.authenticationMode = authenticationMode;
      this.accessKeyId = accessKeyId;
      this.secretAccessKey = secretAccessKey;
      this.region = region;
      this.prefixesFilter = prefixesFilter;
      this.tagsNameFilters = tagsNameFilters;
      this.tagsValueFilters = tagsValueFilters;
      this.cacheMaximumSize = cacheMaximumSize;
    }

    @Override
    public boolean isEnabled() {
      return true;
    }

    @Override
    public AwsAuthenticationMode getAuthenticationMode() {
      return authenticationMode;
    }

    @Override
    public String getAccessKeyId() {
      return accessKeyId;
    }

    @Override
    public String getSecretAccessKey() {
      return secretAccessKey;
    }

    @Override
    public String getRegion() {
      return region;
    }

    @Override
    public long getCacheMaximumSize() {
      return cacheMaximumSize;
    }

    @Override
    public Collection<String> getPrefixesFilter() {
      return prefixesFilter;
    }

    @Override
    public Collection<String> getTagNamesFilter() {
      return tagsNameFilters;
    }

    @Override
    public Collection<String> getTagValuesFilter() {
      return tagsValueFilters;
    }
  }
}
