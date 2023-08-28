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
package tech.pegasys.web3signer.keystorage.azure;

import tech.pegasys.web3signer.keystorage.common.MappedResults;
import tech.pegasys.web3signer.keystorage.common.SecretValueMapperUtil;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.exception.ResourceNotFoundException;
import com.azure.core.http.HttpClient;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.util.HttpClientOptions;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.security.keyvault.keys.KeyClient;
import com.azure.security.keyvault.keys.KeyClientBuilder;
import com.azure.security.keyvault.keys.KeyServiceVersion;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.models.SignatureAlgorithm;
import com.azure.security.keyvault.keys.models.KeyProperties;
import com.azure.security.keyvault.keys.models.KeyVaultKey;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.azure.security.keyvault.secrets.models.SecretProperties;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;

public class AzureKeyVault {

  private static final Logger LOG = LogManager.getLogger();
  private final TokenCredential tokenCredential;
  private final SecretClient secretClient;
  private final KeyClient keyClient;
  private static final List<String> SCOPE = List.of("https://vault.azure.net/.default");
  private final TokenRequestContext tokenRequestContext =
      new TokenRequestContext().setScopes(SCOPE);

  private Optional<AccessToken> maybeToken = Optional.empty();

  public static AzureKeyVault createUsingClientSecretCredentials(
      final String clientId,
      final String clientSecret,
      final String tenantId,
      final String vaultName,
      final ExecutorService executorService,
      final long timeout) {
    final TokenCredential tokenCredential =
        new ClientSecretCredentialBuilder()
            .clientId(clientId)
            .clientSecret(clientSecret)
            .tenantId(tenantId)
            .executorService(executorService)
            .build();
    return new AzureKeyVault(tokenCredential, vaultName, timeout);
  }

  public static AzureKeyVault createUsingManagedIdentity(
      final Optional<String> clientId, final String vaultName, final long timeout) {
    final ManagedIdentityCredentialBuilder managedIdentityCredentialBuilder =
        new ManagedIdentityCredentialBuilder();
    clientId.ifPresent(managedIdentityCredentialBuilder::clientId);
    return new AzureKeyVault(managedIdentityCredentialBuilder.build(), vaultName, timeout);
  }

  private AzureKeyVault(
      final TokenCredential tokenCredential, final String vaultName, final long timeout) {
    this.tokenCredential = tokenCredential;
    final String vaultUrl = constructAzureKeyVaultUrl(vaultName);

    final HttpClient customisedHttpClient =
        HttpClient.createDefault(
            new HttpClientOptions().setResponseTimeout(Duration.ofSeconds(timeout)));

    secretClient =
        new SecretClientBuilder()
            .httpClient(customisedHttpClient)
            .vaultUrl(vaultUrl)
            .credential(tokenCredential)
            .buildClient();

    keyClient =
        new KeyClientBuilder()
            .httpClient(customisedHttpClient)
            .vaultUrl(vaultUrl)
            .credential(tokenCredential)
            .buildClient();
  }

  public Optional<String> fetchSecret(final String secretName) {
    try {
      return Optional.of(secretClient.getSecret(secretName).getValue());
    } catch (final ResourceNotFoundException e) {
      return Optional.empty();
    }
  }

  public CryptographyClient fetchKey(final String keyName, final String keyVersion) {
    final KeyVaultKey key = keyClient.getKey(keyName, keyVersion);
    final String keyId = key.getId();

    return new CryptographyClientBuilder()
        .credential(tokenCredential)
        .keyIdentifier(keyId)
        .buildClient();
  }

  public HttpRequest getRemoteSigningHttpRequest(
      final byte[] data,
      final SignatureAlgorithm signingAlgo,
      final String vaultName,
      final String azureKeyName,
      final String azureKeyVersion) {

    final String apiVersion = KeyServiceVersion.getLatest().getVersion();

    final JsonObject jsonBody = new JsonObject();
    jsonBody.put("alg", signingAlgo);
    jsonBody.put("value", Bytes.of(data).toBase64String());

    final String uriString =
        constructAzureSignApiUri(vaultName, azureKeyName, azureKeyVersion, apiVersion);

    final HttpRequest httpRequest =
        HttpRequest.newBuilder(URI.create(uriString))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + getOrRequestNewToken())
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
            .build();

    return httpRequest;
  }

  private String constructAzureSignApiUri(
      final String keyVaultName,
      final String keyName,
      final String keyVersion,
      final String apiVersion) {
    return String.format(
        "https://%s.vault.azure.net/keys/%s/%s/sign?api-version=%s",
        keyVaultName, keyName, keyVersion, apiVersion);
  }

  public static String constructAzureKeyVaultUrl(final String keyVaultName) {
    return String.format("https://%s.vault.azure.net", keyVaultName);
  }

  /**
   * Fetch multiple secrets from Azure. Apply mapper function to transform the secret values.
   *
   * @param mapper The mapper function to transform secret values to type R.
   * @param tags Map of tags. Only secrets which contains all the tags entries are processed.
   * @return Mapped results containing the converted secrets and error count.
   * @param <R> The result type of mapper function.
   */
  public <R> MappedResults<R> mapSecrets(
      final BiFunction<String, String, R> mapper, final Map<String, String> tags) {
    final Set<R> result = ConcurrentHashMap.newKeySet();
    final AtomicInteger errorCount = new AtomicInteger(0);
    try {
      final PagedIterable<SecretProperties> secretsPagedIterable =
          secretClient.listPropertiesOfSecrets();

      secretsPagedIterable
          .streamByPage()
          .forEach(
              keyPage ->
                  keyPage.getValue().parallelStream()
                      .filter(secretProperties -> secretPropertiesPredicate(tags, secretProperties))
                      .forEach(
                          sp -> {
                            try {
                              final KeyVaultSecret secret = secretClient.getSecret(sp.getName());
                              final MappedResults<R> multiResult =
                                  SecretValueMapperUtil.mapSecretValue(
                                      mapper, sp.getName(), secret.getValue());
                              result.addAll(multiResult.getValues());
                              errorCount.addAndGet(multiResult.getErrorCount());
                            } catch (final Exception e) {
                              LOG.warn(
                                  "Failed to map secret '{}' to requested object type.",
                                  sp.getName());
                              errorCount.incrementAndGet();
                            }
                          }));

    } catch (final Exception e) {
      LOG.error("Unexpected error during Azure map-secrets", e);
      errorCount.incrementAndGet();
    }
    return MappedResults.newInstance(result, errorCount.intValue());
  }

  public <R> MappedResults<R> mapKeyProperties(
      final Function<KeyProperties, R> mapper, final Map<String, String> tags) {
    final Set<R> result = ConcurrentHashMap.newKeySet();
    final AtomicInteger errorCount = new AtomicInteger(0);
    try {
      keyClient
          .listPropertiesOfKeys()
          .streamByPage()
          .forEach(
              keyPage ->
                  keyPage.getValue().parallelStream()
                      .filter(keyProperties -> keyPropertiesPredicate(tags, keyProperties))
                      .forEach(
                          kp -> {
                            try {
                              final R value = mapper.apply(kp);
                              result.add(value);
                            } catch (final Exception e) {
                              LOG.warn(
                                  "Failed to map keyProperties '{}' to requested object type.",
                                  kp.getName());
                              errorCount.incrementAndGet();
                            }
                          }));
    } catch (final Exception e) {
      LOG.error("Unexpected error during Azure mapKeyProperties", e);
      errorCount.incrementAndGet();
    }

    return MappedResults.newInstance(result, errorCount.intValue());
  }

  private static boolean isEmptyTags(final Map<String, String> tags) {
    return tags == null || tags.isEmpty();
  }

  private static boolean secretPropertiesPredicate(
      final Map<String, String> tags, final SecretProperties secretProperties) {
    if (isEmptyTags(tags))
      return true; // we don't want to filter if user-supplied tags map is empty

    return secretProperties.getTags() != null // return false if remote secret doesn't have any tags
        && secretProperties.getTags().entrySet().containsAll(tags.entrySet());
  }

  private static boolean keyPropertiesPredicate(
      final Map<String, String> tags, final KeyProperties keyProperties) {
    if (isEmptyTags(tags))
      return true; // we don't want to filter if user-supplied tags map is empty

    return keyProperties.getTags() != null // return false if remote secret doesn't have any tags
        && keyProperties.getTags().entrySet().containsAll(tags.entrySet());
  }

  private String getOrRequestNewToken() {
    if (maybeToken.isEmpty() || maybeToken.get().isExpired()) {
      maybeToken = Optional.of(tokenCredential.getTokenSync(tokenRequestContext));
    }

    return maybeToken.get().getToken();
  }
}
