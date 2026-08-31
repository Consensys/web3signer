/*
 * Copyright 2020 ConsenSys AG.
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

import tech.pegasys.web3signer.keystorage.azure.AzureKeyVault;
import tech.pegasys.web3signer.keystorage.azure.AzureOverrides;

import java.io.Closeable;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Builds {@link AzureKeyVault} instances, one per distinct (credentials, vault, client-behaviour)
 * combination.
 *
 * <p>Each {@link AzureKeyVault} wraps an Azure AD {@code TokenCredential}, an HTTP client and Azure
 * Key Vault {@code SecretClient}/{@code KeyClient}. Building one is expensive (an AAD
 * client-credentials login plus a new HTTP connection pool) and this factory is invoked once per
 * loaded key/secret, so identical credential sets are cached rather than rebuilt on every call. The
 * bound keeps memory/connection use flat even if callers churn through many distinct credential
 * sets; the least-recently-used entry is simply evicted and, if needed again, rebuilt.
 *
 * <p>Cached {@link AzureKeyVault} instances hold no OS resources that require explicit release (the
 * Azure SDK clients are not {@link Closeable}), so eviction/{@link #close()} only need to drop
 * references, not close anything.
 */
public class AzureKeyVaultFactory implements Closeable {
  private static final int CLIENT_CACHE_SIZE = 10;

  private final AtomicReference<ExecutorService> executorServiceCache = new AtomicReference<>();
  private final LoadingCache<AzureKeyVaultKey, AzureKeyVault> vaultCache =
      CacheBuilder.newBuilder()
          .maximumSize(CLIENT_CACHE_SIZE)
          .build(
              new CacheLoader<>() {
                @Override
                public AzureKeyVault load(final AzureKeyVaultKey key) {
                  return buildAzureKeyVault(key);
                }
              });

  public AzureKeyVault createAzureKeyVault(final AzureKeyVaultParameters azureKeyVaultParameters) {
    return createAzureKeyVault(
        azureKeyVaultParameters.getClientId(),
        azureKeyVaultParameters.getClientSecret(),
        azureKeyVaultParameters.getKeyVaultName(),
        azureKeyVaultParameters.getTenantId(),
        azureKeyVaultParameters.getAuthenticationMode(),
        azureKeyVaultParameters.getTimeout(),
        azureKeyVaultParameters.getAzureOverrides());
  }

  public AzureKeyVault createAzureKeyVault(
      final String clientId,
      final String clientSecret,
      final String keyVaultName,
      final String tenantId,
      final AzureAuthenticationMode mode,
      final long httpClientTimeout,
      final AzureOverrides azureOverrides) {
    final AzureKeyVaultKey key =
        new AzureKeyVaultKey(
            clientId,
            clientSecret,
            keyVaultName,
            tenantId,
            mode,
            httpClientTimeout,
            azureOverrides);
    // getUnchecked is safe here: the loader (buildAzureKeyVault) never throws a checked
    // exception, only unchecked ones from the Azure SDK, which getUnchecked propagates as-is.
    return vaultCache.getUnchecked(key);
  }

  private AzureKeyVault buildAzureKeyVault(final AzureKeyVaultKey key) {
    return switch (key.mode()) {
      case USER_ASSIGNED_MANAGED_IDENTITY ->
          AzureKeyVault.createUsingManagedIdentity(
              Optional.of(key.clientId()),
              key.keyVaultName(),
              key.httpClientTimeout(),
              key.azureOverrides());
      case SYSTEM_ASSIGNED_MANAGED_IDENTITY ->
          AzureKeyVault.createUsingManagedIdentity(
              Optional.empty(), key.keyVaultName(), key.httpClientTimeout(), key.azureOverrides());
      case CLIENT_SECRET ->
          AzureKeyVault.createUsingClientSecretCredentials(
              key.clientId(),
              key.clientSecret(),
              key.tenantId(),
              key.keyVaultName(),
              getOrCreateExecutor(),
              key.httpClientTimeout(),
              key.azureOverrides());
    };
  }

  private ExecutorService getOrCreateExecutor() {
    return executorServiceCache.updateAndGet(
        e ->
            Objects.requireNonNullElseGet(
                e, () -> Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())));
  }

  @Override
  public void close() {
    // Cached AzureKeyVault instances hold no closeable resources (Azure SDK sync clients and
    // TokenCredential are not Closeable); dropping the references is sufficient.
    vaultCache.invalidateAll();

    final ExecutorService executorService = executorServiceCache.get();
    if (executorService != null) {
      executorService.shutdownNow();
      executorServiceCache.set(null);
    }
  }

  @VisibleForTesting
  protected AtomicReference<ExecutorService> getExecutorServiceCache() {
    return executorServiceCache;
  }

  @VisibleForTesting
  protected LoadingCache<AzureKeyVaultKey, AzureKeyVault> getVaultCache() {
    return vaultCache;
  }

  /** Identifies a cached {@link AzureKeyVault} by every input that affects how it is built. */
  @VisibleForTesting
  record AzureKeyVaultKey(
      String clientId,
      String clientSecret,
      String keyVaultName,
      String tenantId,
      AzureAuthenticationMode mode,
      long httpClientTimeout,
      AzureOverrides azureOverrides) {}
}
