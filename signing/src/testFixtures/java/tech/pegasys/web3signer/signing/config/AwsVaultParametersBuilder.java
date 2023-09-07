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

import tech.pegasys.web3signer.common.config.AwsAuthenticationMode;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public final class AwsVaultParametersBuilder {
  private AwsAuthenticationMode authenticationMode = AwsAuthenticationMode.SPECIFIED;
  private String accessKeyId;
  private String secretAccessKey;
  private String region;
  private Collection<String> prefixesFilter = Collections.emptyList();
  private Collection<String> tagNamesFilter = Collections.emptyList();
  private Collection<String> tagValuesFilter = Collections.emptyList();
  private long cacheMaximumSize = 1;

  private Optional<URI> endpointURI = Optional.empty();
  private boolean enabled;

  private AwsVaultParametersBuilder() {}

  public static AwsVaultParametersBuilder anAwsParameters() {
    return new AwsVaultParametersBuilder();
  }

  public AwsVaultParametersBuilder withAuthenticationMode(
      final AwsAuthenticationMode authenticationMode) {
    this.authenticationMode = authenticationMode;
    return this;
  }

  public AwsVaultParametersBuilder withAccessKeyId(final String accessKeyId) {
    this.accessKeyId = accessKeyId;
    return this;
  }

  public AwsVaultParametersBuilder withSecretAccessKey(final String secretAccessKey) {
    this.secretAccessKey = secretAccessKey;
    return this;
  }

  public AwsVaultParametersBuilder withRegion(final String region) {
    this.region = region;
    return this;
  }

  public AwsVaultParametersBuilder withPrefixesFilter(final Collection<String> prefixesFilter) {
    this.prefixesFilter = prefixesFilter;
    return this;
  }

  public AwsVaultParametersBuilder withTagNamesFilter(final Collection<String> tagNameFilters) {
    this.tagNamesFilter = tagNameFilters;
    return this;
  }

  public AwsVaultParametersBuilder withTagValuesFilter(final Collection<String> tagValuesFilter) {
    this.tagValuesFilter = tagValuesFilter;
    return this;
  }

  public AwsVaultParametersBuilder withCacheMaximumSize(final long cacheMaximumSize) {
    this.cacheMaximumSize = cacheMaximumSize;
    return this;
  }

  public AwsVaultParametersBuilder withEndpointOverride(final Optional<URI> endpointOverride) {
    this.endpointURI = endpointOverride;
    return this;
  }

  public AwsVaultParametersBuilder withEnabled(final boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  public AwsVaultParameters build() {
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

    return new TestAwsVaultParameters(
        authenticationMode,
        accessKeyId,
        secretAccessKey,
        region,
        prefixesFilter,
        tagNamesFilter,
        tagValuesFilter,
        cacheMaximumSize,
        endpointURI,
        enabled);
  }

  private static class TestAwsVaultParameters implements AwsVaultParameters {
    private final AwsAuthenticationMode authenticationMode;
    private final String accessKeyId;
    private final String secretAccessKey;
    private final String region;
    private final Collection<String> prefixesFilter;
    private final Collection<String> tagNamesFilter;
    private final Collection<String> tagValuesFilter;
    private final long cacheMaximumSize;
    private final Optional<URI> endpointOverride;
    private final boolean enabled;

    TestAwsVaultParameters(
        final AwsAuthenticationMode authenticationMode,
        final String accessKeyId,
        final String secretAccessKey,
        final String region,
        final Collection<String> prefixesFilter,
        final Collection<String> tagNamesFilter,
        final Collection<String> tagValuesFilter,
        final long cacheMaximumSize,
        final Optional<URI> endpointOverride,
        final boolean enabled) {
      this.authenticationMode = authenticationMode;
      this.accessKeyId = accessKeyId;
      this.secretAccessKey = secretAccessKey;
      this.region = region;
      this.prefixesFilter = prefixesFilter;
      this.tagNamesFilter = tagNamesFilter;
      this.tagValuesFilter = tagValuesFilter;
      this.cacheMaximumSize = cacheMaximumSize;
      this.endpointOverride = endpointOverride;
      this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
      return enabled;
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
      return tagNamesFilter;
    }

    @Override
    public Collection<String> getTagValuesFilter() {
      return tagValuesFilter;
    }

    @Override
    public Optional<URI> getEndpointOverride() {
      return endpointOverride;
    }
  }
}
