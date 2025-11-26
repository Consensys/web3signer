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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static tech.pegasys.web3signer.keystorage.azure.AzureKeyVault.createUsingClientSecretCredentials;

import tech.pegasys.web3signer.keystorage.common.MappedResults;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.azure.security.keyvault.keys.models.KeyProperties;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AzureKeyVaultTest {
  private static final String CLIENT_ID = System.getenv("AZURE_CLIENT_ID");
  private static final String CLIENT_SECRET = System.getenv("AZURE_CLIENT_SECRET");
  private static final String TENANT_ID = System.getenv("AZURE_TENANT_ID");
  private static final String VAULT_NAME = System.getenv("AZURE_KEY_VAULT_NAME");
  private final ExecutorService azureExecutor = Executors.newCachedThreadPool();
  private final long AZURE_DEFAULT_TIMEOUT = 60;

  @BeforeAll
  public static void setup() {
    assumeTrue(!StringUtils.isEmpty(CLIENT_ID), "Set AZURE_CLIENT_ID environment variable");
    assumeTrue(!StringUtils.isEmpty(CLIENT_SECRET), "Set AZURE_CLIENT_SECRET environment variable");
    assumeTrue(!StringUtils.isEmpty(TENANT_ID), "Set AZURE_TENANT_ID environment variable");
    assumeTrue(!StringUtils.isEmpty(VAULT_NAME), "Set AZURE_KEY_VAULT_NAME environment variable");
  }

  @Test
  void fetchExistingSecretKeyFromAzureVault() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(
            CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME, azureExecutor, AZURE_DEFAULT_TIMEOUT);
    // obtain the list of available secret names, then fetch first secret by name
    final List<AzureKeyVault.AzureSecret> availableSecrets = azureKeyVault.getAzureSecrets();
    assertThat(availableSecrets).isNotEmpty();
    final var randomSecret = availableSecrets.stream().findAny().orElseThrow();
    final Optional<String> data = azureKeyVault.fetchSecret(randomSecret.name());
    assertThat(data).isPresent();
  }

  @Test
  void connectingWithInvalidClientSecretThrowsException() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(
            CLIENT_ID, "invalid", TENANT_ID, VAULT_NAME, azureExecutor, AZURE_DEFAULT_TIMEOUT);
    Assertions.assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> azureKeyVault.fetchSecret("not-relevant"))
        .withMessageContaining("Invalid client secret");
  }

  @Test
  void connectingWithInvalidClientIdThrowsException() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(
            "invalid", CLIENT_SECRET, TENANT_ID, VAULT_NAME, azureExecutor, AZURE_DEFAULT_TIMEOUT);
    Assertions.assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> azureKeyVault.fetchSecret("not-relevant"))
        .withMessageContaining(
            "Application with identifier 'invalid' was not found in the directory");
  }

  @Test
  void nonExistingSecretReturnEmpty() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(
            CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME, azureExecutor, AZURE_DEFAULT_TIMEOUT);
    assertThat(azureKeyVault.fetchSecret("not-relevant")).isEmpty();
  }

  @Test
  void secretsCanBeMappedUsingCustomMappingFunction() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(
            CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME, azureExecutor, AZURE_DEFAULT_TIMEOUT);

    // obtain list of secret names directly. Then validate mapping function works as expected.
    final List<AzureKeyVault.AzureSecret> availableSecrets = azureKeyVault.getAzureSecrets();
    assertThat(availableSecrets).isNotEmpty();
    var valuesCount = availableSecrets.stream().mapToInt(s -> s.values().size()).sum();

    // mapSecrets can convert multiple secrets under single key.
    final MappedResults<SimpleEntry<String, String>> result =
        azureKeyVault.mapSecrets(SimpleEntry::new, Collections.emptyMap());
    final Collection<SimpleEntry<String, String>> entries = result.getValues();

    // the number of entries should match the available secret values count
    assertThat(entries).hasSize(valuesCount);
    assertThat(result.getErrorCount()).isZero();
  }

  @Test
  void keyPropertiesCanBeMappedUsingCustomMappingFunction() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(
            CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME, azureExecutor, AZURE_DEFAULT_TIMEOUT);

    // obtain list of Key names. Then validate mapping function works as expected.
    final List<String> availableKeyNames =
        azureKeyVault.getAzureKeys().stream().map(AzureKeyVault.AzureKey::name).toList();
    assertThat(availableKeyNames).isNotEmpty();

    final MappedResults<String> result =
        azureKeyVault.mapKeyProperties(KeyProperties::getName, Collections.emptyMap());
    final Collection<String> entries = result.getValues();
    assertThat(entries).containsExactlyInAnyOrderElementsOf(availableKeyNames);
    assertThat(result.getErrorCount()).isZero();
  }

  @Test
  void mapSecretsUsingTags() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(
            CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME, azureExecutor, AZURE_DEFAULT_TIMEOUT);

    final MappedResults<SimpleEntry<String, String>> result =
        azureKeyVault.mapSecrets(SimpleEntry::new, Map.of("ENV", "TEST"));
    // The Azure key vault is set up with at least one Secret with above tag.
    assertThat(result.getValues()).isNotEmpty();
    // we should not encounter any error count
    assertThat(result.getErrorCount()).isZero();
  }

  @Test
  void mapKeyPropertiesUsingTags() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(
            CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME, azureExecutor, AZURE_DEFAULT_TIMEOUT);

    final MappedResults<String> result =
        azureKeyVault.mapKeyProperties(KeyProperties::getName, Map.of("ENV", "TEST"));
    // The Azure key vault is set up with at least one Key with above tag.
    final Collection<String> entries = result.getValues();
    assertThat(entries).isNotEmpty();
    // we should not encounter any error count
    assertThat(result.getErrorCount()).isZero();
  }

  @Test
  void mapSecretsWhenTagsDoesNotExist() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(
            CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME, azureExecutor, AZURE_DEFAULT_TIMEOUT);

    final MappedResults<SimpleEntry<String, String>> result =
        azureKeyVault.mapSecrets(SimpleEntry::new, Map.of("INVALID_TAG", "INVALID_TEST"));

    // The secret vault is not expected to have any secrets with above tags.
    assertThat(result.getValues()).isEmpty();

    // we should not encounter any error count
    assertThat(result.getErrorCount()).isZero();
  }

  @Test
  void mapKeyPropertiesWhenTagsDoesNotExist() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(
            CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME, azureExecutor, AZURE_DEFAULT_TIMEOUT);

    final MappedResults<String> result =
        azureKeyVault.mapKeyProperties(
            KeyProperties::getName, Map.of("INVALID_TAG", "INVALID_TEST"));

    // The key vault is not expected to have any secrets with above tags.
    assertThat(result.getValues()).isEmpty();

    // we should not encounter any error count
    assertThat(result.getErrorCount()).isZero();
  }

  @Test
  void mapSecretsThrowsAwayObjectsWhichFailMapper() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(
            CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME, azureExecutor, AZURE_DEFAULT_TIMEOUT);

    // map one remote Secret/Value conversion to raise exception - this is to simulate failure in
    // mapping function. The assertion assumes that there are more than one Secrets in the remote
    // vault
    final AtomicInteger counter = new AtomicInteger(0);
    final MappedResults<SimpleEntry<String, String>> result =
        azureKeyVault.mapSecrets(
            (name, value) -> {
              if (counter.incrementAndGet() == 1) {
                throw new RuntimeException("Arbitrary Failure");
              }
              return new SimpleEntry<>(name, value);
            },
            Collections.emptyMap());
    assertThat(result.getErrorCount()).isOne();
    assertThat(result.getValues()).isNotEmpty();
  }

  @Test
  void mapKeyPropertiesThrowsAwayObjectsWhichFailMapper() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(
            CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME, azureExecutor, AZURE_DEFAULT_TIMEOUT);

    // map one remote Key conversion to raise exception - this is to simulate failure in mapping
    // function. The assertion assumes that there are more than one Key in the remote vault
    final AtomicInteger counter = new AtomicInteger(0);
    final MappedResults<String> result =
        azureKeyVault.mapKeyProperties(
            keyProperties -> {
              if (counter.incrementAndGet() == 1) {
                throw new RuntimeException("Arbitrary Failure");
              }
              return keyProperties.getName();
            },
            Collections.emptyMap());

    assertThat(result.getErrorCount()).isOne();
    assertThat(result.getValues()).isNotEmpty();
  }

  @Test
  void mapSecretsThrowsAwayObjectsWhichMapToNull() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(
            CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME, azureExecutor, AZURE_DEFAULT_TIMEOUT);

    // map all remote Secrets values to null - this is to simulate failure in mapping function
    final MappedResults<SimpleEntry<String, String>> result =
        azureKeyVault.mapSecrets((name, value) -> null, Collections.emptyMap());
    final Collection<SimpleEntry<String, String>> entries = result.getValues();
    assertThat(result.getErrorCount()).isNotZero();
    assertThat(result.getValues()).isEmpty();
  }

  @Test
  void mapKeyPropertiesThrowsAwayObjectsWhichMapToNull() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(
            CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME, azureExecutor, AZURE_DEFAULT_TIMEOUT);

    // map all remote Keys to null - this is to simulate failure in mapping function
    final MappedResults<String> result =
        azureKeyVault.mapKeyProperties(keyProperties -> null, Collections.emptyMap());

    assertThat(result.getErrorCount()).isNotZero();
    assertThat(result.getValues()).isEmpty();
  }
}
