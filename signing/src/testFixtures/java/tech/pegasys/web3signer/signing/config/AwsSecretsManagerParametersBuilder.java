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

public final class AwsSecretsManagerParametersBuilder {
  private AwsAuthenticationMode authenticationMode = AwsAuthenticationMode.SPECIFIED;
  private String accessKeyId;
  private String secretAccessKey;
  private String region;
  private Collection<String> prefixFilters = Collections.emptyList();
  private Collection<String> tagNameFilters = Collections.emptyList();
  private Collection<String> tagValueFilters = Collections.emptyList();
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

  public AwsSecretsManagerParametersBuilder withAccessKeyId(final String accessKeyId) {
    this.accessKeyId = accessKeyId;
    return this;
  }

  public AwsSecretsManagerParametersBuilder withSecretAccessKey(final String secretAccessKey) {
    this.secretAccessKey = secretAccessKey;
    return this;
  }

  public AwsSecretsManagerParametersBuilder withRegion(final String region) {
    this.region = region;
    return this;
  }

  public AwsSecretsManagerParametersBuilder withPrefixFilters(
      final Collection<String> prefixFilters) {
    this.prefixFilters = prefixFilters;
    return this;
  }

  public AwsSecretsManagerParametersBuilder withTagNameFilters(
      final Collection<String> tagNameFilters) {
    this.tagNameFilters = tagNameFilters;
    return this;
  }

  public AwsSecretsManagerParametersBuilder withTagValueFilters(
      final Collection<String> tagValueFilters) {
    this.tagValueFilters = tagValueFilters;
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
        prefixFilters,
        tagNameFilters,
        tagValueFilters,
        cacheMaximumSize);
  }

  private static class TestAwsSecretsManagerParameters implements AwsSecretsManagerParameters {
    private final AwsAuthenticationMode authenticationMode;
    private final String accessKeyId;
    private final String secretAccessKey;
    private final String region;
    private final Collection<String> prefixFilters;
    private final Collection<String> tagNameFilters;
    private final Collection<String> tagValueFilters;
    private long cacheMaximumSize;

    TestAwsSecretsManagerParameters(
        final AwsAuthenticationMode authenticationMode,
        final String accessKeyId,
        final String secretAccessKey,
        final String region,
        final Collection<String> prefixFilters,
        final Collection<String> tagNameFilters,
        final Collection<String> tagValueFilters,
        final long cacheMaximumSize) {
      this.authenticationMode = authenticationMode;
      this.accessKeyId = accessKeyId;
      this.secretAccessKey = secretAccessKey;
      this.region = region;
      this.prefixFilters = prefixFilters;
      this.tagNameFilters = tagNameFilters;
      this.tagValueFilters = tagValueFilters;
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
    public Collection<String> getPrefixFilters() {
      return prefixFilters;
    }

    @Override
    public Collection<String> getTagNameFilters() {
      return tagNameFilters;
    }

    @Override
    public Collection<String> getTagValueFilters() {
      return tagValueFilters;
    }
  }
}
