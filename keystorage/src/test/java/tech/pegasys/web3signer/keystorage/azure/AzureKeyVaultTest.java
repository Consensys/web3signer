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
import static tech.pegasys.web3signer.keystorage.azure.AzureKeyVault.createUsingClientSecretCredentials;

import tech.pegasys.web3signer.keystorage.common.MappedResults;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.azure.security.keyvault.keys.models.KeyProperties;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AzureKeyVaultTest {
  private static final String CLIENT_ID = System.getenv("AZURE_CLIENT_ID");
  private static final String CLIENT_SECRET = System.getenv("AZURE_CLIENT_SECRET");
  private static final String TENANT_ID = System.getenv("AZURE_TENANT_ID");
  private static final String VAULT_NAME = System.getenv("AZURE_KEY_VAULT_NAME");
  private static final String SECRET_NAME = "TEST-KEY";
  private static final String SECRET_NAME2 = "TEST-KEY-2";
  public static final String KEY_NAME = "TestKey2";
  public static final String KEY_NAME2 = "TestKeyWithTag";
  private static final String EXPECTED_KEY =
      "3ee2224386c82ffea477e2adf28a2929f5c349165a4196158c7f3a2ecca40f35";
  private static final String EXPECTED_KEY2 =
      "0x5aba5b89c1d8b731dba1ba29128a4070df0dbfd7e0a67edb40ae7f860cd3ca1c";
  private final ExecutorService azureExecutor = Executors.newCachedThreadPool();
  private final long AZURE_DEFAULT_TIMEOUT = 60;

  @BeforeAll
  public static void setup() {
    Assumptions.assumeTrue(CLIENT_ID != null, "Set AZURE_CLIENT_ID environment variable");
    Assumptions.assumeTrue(CLIENT_SECRET != null, "Set AZURE_CLIENT_SECRET environment variable");
    Assumptions.assumeTrue(TENANT_ID != null, "Set AZURE_TENANT_ID environment variable");
    Assumptions.assumeTrue(VAULT_NAME != null, "Set AZURE_KEY_VAULT_NAME environment variable");
  }

  @Test
  void fetchExistingSecretKeyFromAzureVault() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(
            CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME, azureExecutor, AZURE_DEFAULT_TIMEOUT);
    final Optional<String> hexKey = azureKeyVault.fetchSecret(SECRET_NAME);
    Assertions.assertThat(hexKey).isNotEmpty().get().isEqualTo(EXPECTED_KEY);
  }

  @Test
  void connectingWithInvalidClientSecretThrowsException() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(
            CLIENT_ID, "invalid", TENANT_ID, VAULT_NAME, azureExecutor, AZURE_DEFAULT_TIMEOUT);
    Assertions.assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> azureKeyVault.fetchSecret(SECRET_NAME))
        .withMessageContaining("Invalid client secret");
  }

  @Test
  void connectingWithInvalidClientIdThrowsException() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(
            "invalid", CLIENT_SECRET, TENANT_ID, VAULT_NAME, azureExecutor, AZURE_DEFAULT_TIMEOUT);
    Assertions.assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> azureKeyVault.fetchSecret(SECRET_NAME))
        .withMessageContaining(
            "Application with identifier 'invalid' was not found in the directory");
  }

  @Test
  void nonExistingSecretReturnEmpty() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(
            CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME, azureExecutor, AZURE_DEFAULT_TIMEOUT);
    assertThat(azureKeyVault.fetchSecret("X-" + SECRET_NAME)).isEmpty();
  }

  @Test
  void secretsCanBeMappedUsingCustomMappingFunction() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(
            CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME, azureExecutor, AZURE_DEFAULT_TIMEOUT);

    final MappedResults<SimpleEntry<String, String>> result =
        azureKeyVault.mapSecrets(SimpleEntry::new, Collections.emptyMap());
    final Collection<SimpleEntry<String, String>> entries = result.getValues();

    final Optional<SimpleEntry<String, String>> testKeyEntry =
        entries.stream().filter(e -> e.getKey().equals(SECRET_NAME)).findAny();
    Assertions.assertThat(testKeyEntry).isPresent();
    Assertions.assertThat(testKeyEntry.get().getValue()).isEqualTo(EXPECTED_KEY);
    Assertions.assertThat(result.getErrorCount()).isZero();
  }

  @Test
  void keyPropertiesCanBeMappedUsingCustomMappingFunction() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(
            CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME, azureExecutor, AZURE_DEFAULT_TIMEOUT);

    final MappedResults<String> result =
        azureKeyVault.mapKeyProperties(KeyProperties::getName, Collections.emptyMap());
    final Collection<String> entries = result.getValues();
    final Optional<String> testKeyEntry =
        entries.stream().filter(e -> e.equals(KEY_NAME)).findAny();
    Assertions.assertThat(testKeyEntry).hasValue(KEY_NAME);
    Assertions.assertThat(result.getErrorCount()).isZero();
  }

  @Test
  void mapSecretsUsingTags() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(
            CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME, azureExecutor, AZURE_DEFAULT_TIMEOUT);

    final MappedResults<SimpleEntry<String, String>> result =
        azureKeyVault.mapSecrets(SimpleEntry::new, Map.of("ENV", "TEST"));
    // The Secrets vault is set up with one secret with this tag. Make sure that it is the only
    // secret that is returned.
    Assertions.assertThat(result.getValues().size()).isOne();
    Optional<SimpleEntry<String, String>> secretEntry =
        result.getValues().stream()
            .filter(entry -> SECRET_NAME2.equals(entry.getKey()))
            .findFirst();
    Assertions.assertThat(secretEntry).isPresent();
    Assertions.assertThat(secretEntry.get().getValue()).isEqualTo(EXPECTED_KEY2);

    // we should not encounter any error count
    Assertions.assertThat(result.getErrorCount()).isZero();
  }

  @Test
  void mapKeyPropertiesUsingTags() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(
            CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME, azureExecutor, AZURE_DEFAULT_TIMEOUT);

    final MappedResults<String> result =
        azureKeyVault.mapKeyProperties(KeyProperties::getName, Map.of("ENV", "TEST"));
    final Collection<String> entries = result.getValues();
    final Optional<String> testKeyEntry =
        entries.stream().filter(e -> e.equals(KEY_NAME2)).findAny();
    Assertions.assertThat(testKeyEntry).hasValue(KEY_NAME2);
    Assertions.assertThat(result.getErrorCount()).isZero();
  }

  @Test
  void mapSecretsWhenTagsDoesNotExist() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(
            CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME, azureExecutor, AZURE_DEFAULT_TIMEOUT);

    final MappedResults<SimpleEntry<String, String>> result =
        azureKeyVault.mapSecrets(SimpleEntry::new, Map.of("INVALID_TAG", "INVALID_TEST"));

    // The secret vault is not expected to have any secrets with above tags.
    Assertions.assertThat(result.getValues()).isEmpty();

    // we should not encounter any error count
    Assertions.assertThat(result.getErrorCount()).isZero();
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
    Assertions.assertThat(result.getValues()).isEmpty();

    // we should not encounter any error count
    Assertions.assertThat(result.getErrorCount()).isZero();
  }

  @Test
  void mapSecretsThrowsAwayObjectsWhichFailMapper() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(
            CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME, azureExecutor, AZURE_DEFAULT_TIMEOUT);

    final MappedResults<SimpleEntry<String, String>> result =
        azureKeyVault.mapSecrets(
            (name, value) -> {
              if (name.equals(SECRET_NAME2)) {
                throw new RuntimeException("Arbitrary Failure");
              }
              return new SimpleEntry<>(name, value);
            },
            Collections.emptyMap());
    final Collection<SimpleEntry<String, String>> entries = result.getValues();
    Assertions.assertThat(result.getErrorCount()).isOne();

    final Optional<SimpleEntry<String, String>> testKeyEntry =
        entries.stream().filter(e -> e.getKey().equals(SECRET_NAME)).findAny();
    Assertions.assertThat(testKeyEntry).isPresent();
    Assertions.assertThat(testKeyEntry.get().getValue()).isEqualTo(EXPECTED_KEY);

    final Optional<SimpleEntry<String, String>> testKey2 =
        entries.stream().filter(e -> e.getKey().equals(SECRET_NAME2)).findAny();
    Assertions.assertThat(testKey2).isEmpty();
  }

  @Test
  void mapKeyPropertiesThrowsAwayObjectsWhichFailMapper() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(
            CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME, azureExecutor, AZURE_DEFAULT_TIMEOUT);

    final MappedResults<String> result =
        azureKeyVault.mapKeyProperties(
            keyProperties -> {
              if (keyProperties.getName().equals(KEY_NAME2)) {
                throw new RuntimeException("Arbitrary Failure");
              }
              return keyProperties.getName();
            },
            Collections.emptyMap());

    final Collection<String> entries = result.getValues();
    Assertions.assertThat(result.getErrorCount()).isOne();

    final Optional<String> testKey2Entry =
        entries.stream().filter(e -> e.equals(KEY_NAME)).findAny();
    Assertions.assertThat(testKey2Entry).isPresent();
    Assertions.assertThat(testKey2Entry).hasValue(KEY_NAME);

    final Optional<String> testKeyWithTag =
        entries.stream().filter(e -> e.equals(KEY_NAME2)).findAny();
    Assertions.assertThat(testKeyWithTag).isEmpty();
  }

  @Test
  void mapSecretsThrowsAwayObjectsWhichMapToNull() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(
            CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME, azureExecutor, AZURE_DEFAULT_TIMEOUT);

    final MappedResults<SimpleEntry<String, String>> result =
        azureKeyVault.mapSecrets(
            (name, value) -> {
              if (name.equals(SECRET_NAME)) {
                return null;
              }
              return new SimpleEntry<>(name, value);
            },
            Collections.emptyMap());
    final Collection<SimpleEntry<String, String>> entries = result.getValues();
    Assertions.assertThat(result.getErrorCount()).isOne();
    final Optional<SimpleEntry<String, String>> testKeyEntry =
        entries.stream().filter(e -> e.getKey().equals(SECRET_NAME)).findAny();
    Assertions.assertThat(testKeyEntry).isEmpty();

    final Optional<SimpleEntry<String, String>> testKey2Entry =
        entries.stream().filter(e -> e.getKey().equals(SECRET_NAME2)).findAny();
    Assertions.assertThat(testKey2Entry).isPresent();
    Assertions.assertThat(testKey2Entry.get().getValue()).isEqualTo(EXPECTED_KEY2);
  }

  @Test
  void mapKeyPropertiesThrowsAwayObjectsWhichMapToNull() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(
            CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME, azureExecutor, AZURE_DEFAULT_TIMEOUT);

    final MappedResults<String> result =
        azureKeyVault.mapKeyProperties(
            keyProperties -> {
              if (keyProperties.getName().equals(KEY_NAME2)) {
                return null;
              }
              return keyProperties.getName();
            },
            Collections.emptyMap());

    final Collection<String> entries = result.getValues();
    Assertions.assertThat(result.getErrorCount()).isOne();

    final Optional<String> testKey2Entry =
        entries.stream().filter(e -> e.equals(KEY_NAME)).findAny();
    Assertions.assertThat(testKey2Entry).isPresent();
    Assertions.assertThat(testKey2Entry).hasValue(KEY_NAME);

    final Optional<String> testKeyWithTag =
        entries.stream().filter(e -> e.equals(KEY_NAME2)).findAny();
    Assertions.assertThat(testKeyWithTag).isEmpty();
  }
}
