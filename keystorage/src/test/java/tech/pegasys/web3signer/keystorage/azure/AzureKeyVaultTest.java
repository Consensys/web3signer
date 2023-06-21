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
  private static final String EXPECTED_KEY =
      "3ee2224386c82ffea477e2adf28a2929f5c349165a4196158c7f3a2ecca40f35";

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
        createUsingClientSecretCredentials(CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME);
    final Optional<String> hexKey = azureKeyVault.fetchSecret(SECRET_NAME);
    Assertions.assertThat(hexKey).isNotEmpty().get().isEqualTo(EXPECTED_KEY);
  }

  @Test
  void connectingWithInvalidClientSecretThrowsException() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(CLIENT_ID, "invalid", TENANT_ID, VAULT_NAME);
    Assertions.assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> azureKeyVault.fetchSecret(SECRET_NAME))
        .withMessageContaining("Invalid client secret");
  }

  @Test
  void connectingWithInvalidClientIdThrowsException() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials("invalid", CLIENT_SECRET, TENANT_ID, VAULT_NAME);
    Assertions.assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> azureKeyVault.fetchSecret(SECRET_NAME))
        .withMessageContaining(
            "Application with identifier 'invalid' was not found in the directory");
  }

  @Test
  void nonExistingSecretReturnEmpty() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME);
    assertThat(azureKeyVault.fetchSecret("X-" + SECRET_NAME)).isEmpty();
  }

  @Test
  void secretsCanBeMappedUsingCustomMappingFunction() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME);

    final MappedResults<SimpleEntry<String, String>> result =
        azureKeyVault.mapSecrets(SimpleEntry::new, Collections.emptyMap());
    final Collection<SimpleEntry<String, String>> entries = result.getValues();
    final Optional<SimpleEntry<String, String>> myBlsEntry =
        entries.stream().filter(e -> e.getKey().equals("MyBls")).findAny();
    Assertions.assertThat(myBlsEntry).isPresent();
    Assertions.assertThat(myBlsEntry.get().getValue()).isEqualTo("BlsKey");

    final Optional<SimpleEntry<String, String>> testKeyEntry =
        entries.stream().filter(e -> e.getKey().equals("TEST-KEY")).findAny();
    Assertions.assertThat(testKeyEntry).isPresent();
    Assertions.assertThat(testKeyEntry.get().getValue()).isEqualTo(EXPECTED_KEY);
  }

  @Test
  void mapSecretsUsingTags() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME);

    final MappedResults<SimpleEntry<String, String>> result =
        azureKeyVault.mapSecrets(SimpleEntry::new, Map.of("ENV", "TEST"));
    // The Secrets vault is set up with one secret with this tag. Make sure that it is the only
    // secret that is returned.
    Assertions.assertThat(result.getValues().size()).isOne();
    Optional<SimpleEntry<String, String>> secretEntry =
        result.getValues().stream()
            .filter(entry -> "TEST-KEY-2".equals(entry.getKey()))
            .findFirst();
    Assertions.assertThat(secretEntry).isPresent();
    Assertions.assertThat(secretEntry.get().getValue()).isEqualTo(EXPECTED_KEY);

    // we should not encounter any error count
    Assertions.assertThat(result.getErrorCount()).isZero();
  }

  @Test
  void mapSecretsWhenTagsDoesNotExist() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME);

    final MappedResults<SimpleEntry<String, String>> result =
        azureKeyVault.mapSecrets(SimpleEntry::new, Map.of("INVALID_TAG", "INVALID_TEST"));

    // The secret vault is not expected to have any secrets with above tags.
    Assertions.assertThat(result.getValues()).isEmpty();

    // we should not encounter any error count
    Assertions.assertThat(result.getErrorCount()).isZero();
  }

  @Test
  void azureVaultThrowsAwayObjectsWhichFailMapper() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME);

    final MappedResults<SimpleEntry<String, String>> result =
        azureKeyVault.mapSecrets(
            (name, value) -> {
              if (name.equals("MyBls")) {
                throw new RuntimeException("Arbitrary Failure");
              }
              return new SimpleEntry<>(name, value);
            },
            Collections.emptyMap());
    final Collection<SimpleEntry<String, String>> entries = result.getValues();

    final Optional<SimpleEntry<String, String>> testKeyEntry =
        entries.stream().filter(e -> e.getKey().equals("TEST-KEY")).findAny();
    Assertions.assertThat(testKeyEntry).isPresent();
    Assertions.assertThat(testKeyEntry.get().getValue()).isEqualTo(EXPECTED_KEY);

    final Optional<SimpleEntry<String, String>> myBlsEntry =
        entries.stream().filter(e -> e.getKey().equals("MyBls")).findAny();
    Assertions.assertThat(myBlsEntry).isEmpty();
  }

  @Test
  void azureVaultThrowsAwayObjectsWhichMapToNull() {
    final AzureKeyVault azureKeyVault =
        createUsingClientSecretCredentials(CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME);

    final MappedResults<SimpleEntry<String, String>> result =
        azureKeyVault.mapSecrets(
            (name, value) -> {
              if (name.equals("TEST-KEY")) {
                return null;
              }
              return new SimpleEntry<>(name, value);
            },
            Collections.emptyMap());
    final Collection<SimpleEntry<String, String>> entries = result.getValues();
    final Optional<SimpleEntry<String, String>> testKeyEntry =
        entries.stream().filter(e -> e.getKey().equals("TEST-KEY")).findAny();
    Assertions.assertThat(testKeyEntry).isEmpty();

    final Optional<SimpleEntry<String, String>> myBlsEntry =
        entries.stream().filter(e -> e.getKey().equals("MyBls")).findAny();
    Assertions.assertThat(myBlsEntry).isPresent();
    Assertions.assertThat(myBlsEntry.get().getValue()).isEqualTo("BlsKey");
  }
}
