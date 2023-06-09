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
package tech.pegasys.web3signer.keystorage.hashicorp.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import tech.pegasys.web3signer.keystorage.hashicorp.HashicorpException;
import tech.pegasys.web3signer.keystorage.hashicorp.TrustStoreType;
import tech.pegasys.web3signer.keystorage.hashicorp.config.HashicorpKeyConfig;
import tech.pegasys.web3signer.keystorage.hashicorp.config.TlsOptions;
import tech.pegasys.web3signer.keystorage.hashicorp.config.toml.TomlConfigLoader;
import tech.pegasys.web3signer.keystore.hashicorp.util.HashicorpConfigUtil;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TomlConfigLoaderTest {

  private static final String HOST = "localhost";
  private static final int PORT = 8200;
  private static final String KEY_PATH = "/v1/secret/data/DBEncryptionKey";
  private static final String KEY_NAME = "arbitraryKeyName";
  private static final int TIMEOUT = 5;
  private static final String TOKEN = "token";

  private static final String TRUST_STORE_TYPE = "PKCS12";
  private static final String TRUST_STORE_PATH_STRING = "trust/store/file.path";
  private static final String TRUST_STORE_PASSWORD = "trustStorePassword";

  @Test
  void valuesInATomlFileWithNoTlsAreExtractedAsPerFileContent() throws IOException {
    final Path configFile =
        HashicorpConfigUtil.createConfigFileWithoutTls(
            HOST, PORT, TOKEN, KEY_PATH, KEY_NAME, TIMEOUT);
    final HashicorpKeyConfig config = TomlConfigLoader.fromToml(configFile, null);

    assertThat(config.getConnectionParams().getServerHost()).isEqualTo(HOST);
    assertThat(config.getConnectionParams().getServerPort()).isEqualTo(PORT);
    assertThat(config.getConnectionParams().getTimeoutMilliseconds()).isEqualTo(TIMEOUT);
    assertThat(config.getConnectionParams().getTlsOptions()).isEmpty();

    assertThat(config.getKeyDefinition().getToken()).isEqualTo(TOKEN);
    assertThat(config.getKeyDefinition().getKeyPath()).isEqualTo(KEY_PATH);
    assertThat(config.getKeyDefinition().getKeyName().get()).isEqualTo(KEY_NAME);
  }

  @Test
  void tlsValuesAreExtracted() throws IOException {
    final Path configFile =
        HashicorpConfigUtil.createConfigFile(
            HOST,
            PORT,
            TOKEN,
            KEY_PATH,
            KEY_NAME,
            TIMEOUT,
            true,
            TRUST_STORE_TYPE,
            TRUST_STORE_PATH_STRING,
            TRUST_STORE_PASSWORD);

    final HashicorpKeyConfig config = TomlConfigLoader.fromToml(configFile, null);

    assertThat(config.getConnectionParams().getTlsOptions()).isNotEmpty();
    final TlsOptions tlsOptions = config.getConnectionParams().getTlsOptions().get();
    assertThat(tlsOptions.getTrustStoreType().get())
        .isEqualTo(TrustStoreType.fromString(TRUST_STORE_TYPE).get());
    assertThat(tlsOptions.getTrustStorePath()).isEqualTo(Path.of(TRUST_STORE_PATH_STRING));
    assertThat(tlsOptions.getTrustStorePassword()).isEqualTo(TRUST_STORE_PASSWORD);
  }

  @Test
  void invalidTlsTrustStoreTypeThrowsHashicorpException() throws IOException {
    final Path configFile =
        HashicorpConfigUtil.createConfigFile(
            HOST,
            PORT,
            TOKEN,
            KEY_PATH,
            KEY_NAME,
            TIMEOUT,
            true,
            "InvalidTrustStore",
            TRUST_STORE_PATH_STRING,
            TRUST_STORE_PASSWORD);

    assertThatThrownBy(() -> TomlConfigLoader.fromToml(configFile, null))
        .isInstanceOf(HashicorpException.class);
  }

  @Test
  void missingServerHostThrowsHashicorpException() throws IOException {
    final Path configFile =
        HashicorpConfigUtil.createConfigFile(
            null,
            PORT,
            TOKEN,
            KEY_PATH,
            KEY_NAME,
            TIMEOUT,
            true,
            TRUST_STORE_TYPE,
            TRUST_STORE_PATH_STRING,
            TRUST_STORE_PASSWORD);

    assertThatThrownBy(() -> TomlConfigLoader.fromToml(configFile, null))
        .isInstanceOf(HashicorpException.class);
  }

  @Test
  void missingTokenThrowsHashicorpException() throws IOException {
    final Path configFile =
        HashicorpConfigUtil.createConfigFile(
            HOST,
            PORT,
            null,
            KEY_PATH,
            KEY_NAME,
            TIMEOUT,
            true,
            TRUST_STORE_TYPE,
            TRUST_STORE_PATH_STRING,
            TRUST_STORE_PASSWORD);

    assertThatThrownBy(() -> TomlConfigLoader.fromToml(configFile, null))
        .isInstanceOf(HashicorpException.class);
  }

  @Test
  void missingKeyPathThrowsHashicorpException() throws IOException {
    final Path configFile =
        HashicorpConfigUtil.createConfigFile(
            HOST,
            PORT,
            TOKEN,
            null,
            KEY_NAME,
            TIMEOUT,
            true,
            TRUST_STORE_TYPE,
            TRUST_STORE_PATH_STRING,
            TRUST_STORE_PASSWORD);

    assertThatThrownBy(() -> TomlConfigLoader.fromToml(configFile, null))
        .isInstanceOf(HashicorpException.class);
  }

  @Test
  void ifTlsIsDisabledAllFieldsNoFieldsAreReadIntoConfig() throws IOException {
    final Path configFile =
        HashicorpConfigUtil.createConfigFile(
            HOST,
            PORT,
            TOKEN,
            KEY_PATH,
            KEY_NAME,
            TIMEOUT,
            false,
            TRUST_STORE_TYPE,
            TRUST_STORE_PATH_STRING,
            TRUST_STORE_PASSWORD);

    final HashicorpKeyConfig config = TomlConfigLoader.fromToml(configFile, null);
    assertThat(config.getConnectionParams().getTlsOptions()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"PKCS12", "JKS", "WHITELIST", "PEM"})
  void hashicorpExceptionIsThrownIfTrustStoreIsSetButPathIsEmpty(String trustStoreType)
      throws IOException {
    final Path configFile =
        HashicorpConfigUtil.createConfigFile(
            HOST,
            PORT,
            TOKEN,
            KEY_PATH,
            KEY_NAME,
            TIMEOUT,
            true,
            trustStoreType,
            null,
            TRUST_STORE_PASSWORD);

    assertThatThrownBy(() -> TomlConfigLoader.fromToml(configFile, null))
        .isInstanceOf(HashicorpException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"PKCS12", "JKS"})
  void hashicorpExceptionIsThrownIfTrustStoreRequiresPasswordButNoneIsProvided(
      String trustStoreType) throws IOException {
    final Path configFile =
        HashicorpConfigUtil.createConfigFile(
            HOST,
            PORT,
            TOKEN,
            KEY_PATH,
            KEY_NAME,
            TIMEOUT,
            true,
            trustStoreType,
            TRUST_STORE_PATH_STRING,
            null);

    assertThatThrownBy(() -> TomlConfigLoader.fromToml(configFile, null))
        .isInstanceOf(HashicorpException.class);
  }

  @Test
  void missingParametersStillProducesAValidConfiguration() throws IOException {
    final Path configFile =
        HashicorpConfigUtil.createConfigFile(
            HOST, null, // server port
            TOKEN, KEY_PATH, null, // keyname
            null, // timeout
            false, // enable TLS
            null, null, null);

    final HashicorpKeyConfig config = TomlConfigLoader.fromToml(configFile, null);

    assertThat(config.getConnectionParams().getServerPort()).isEqualTo(8200);
    assertThat(config.getConnectionParams().getTimeoutMilliseconds()).isEqualTo(10_000L);
    assertThat(config.getKeyDefinition().getKeyName()).isEmpty();
    assertThat(config.getConnectionParams().getTlsOptions()).isEmpty();
  }
}
