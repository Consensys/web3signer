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
package tech.pegasys.web3signer.keystore.hashicorp.util;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.writeString;
import static tech.pegasys.web3signer.keystorage.hashicorp.config.toml.TomlConfigLoader.PROP_HASHICORP_KEY_NAME;
import static tech.pegasys.web3signer.keystorage.hashicorp.config.toml.TomlConfigLoader.PROP_HASHICORP_KEY_PATH;
import static tech.pegasys.web3signer.keystorage.hashicorp.config.toml.TomlConfigLoader.PROP_HASHICORP_SERVER_HOST;
import static tech.pegasys.web3signer.keystorage.hashicorp.config.toml.TomlConfigLoader.PROP_HASHICORP_SERVER_PORT;
import static tech.pegasys.web3signer.keystorage.hashicorp.config.toml.TomlConfigLoader.PROP_HASHICORP_TIMEOUT;
import static tech.pegasys.web3signer.keystorage.hashicorp.config.toml.TomlConfigLoader.PROP_HASHICORP_TLS_ENABLE;
import static tech.pegasys.web3signer.keystorage.hashicorp.config.toml.TomlConfigLoader.PROP_HASHICORP_TLS_TS_PASSWORD;
import static tech.pegasys.web3signer.keystorage.hashicorp.config.toml.TomlConfigLoader.PROP_HASHICORP_TLS_TS_PATH;
import static tech.pegasys.web3signer.keystorage.hashicorp.config.toml.TomlConfigLoader.PROP_HASHICORP_TLS_TS_TYPE;
import static tech.pegasys.web3signer.keystorage.hashicorp.config.toml.TomlConfigLoader.PROP_HASHICORP_TOKEN;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Formatter;
import java.util.Locale;
import java.util.Optional;

@SuppressWarnings("InlineFormatString")
public class HashicorpConfigUtil {

  private static final String TOML_STRING_FORMAT = "%s=\"%s\"\n";
  private static final String TOML_NUMBER_FORMAT = "%s=%d\n";
  private static final String TOML_BOOLEAN_FORMAT = "%s=%b\n";

  public static Path createConfigFileWithoutTls(
      final String host,
      final int port,
      final String rootToken,
      final String keyPath,
      final String keyName,
      final int timeout)
      throws IOException {
    return createConfigFile(
        host, port, rootToken, keyPath, keyName, timeout, false, null, null, null);
  }

  public static Path createConfigFile(
      final String host,
      final Integer port,
      final String rootToken,
      final String keyPath,
      final String keyName,
      final Integer timeout,
      final boolean tls,
      final String tlsTrustStoreType,
      final String tlsTrustStorePath,
      final String tlsTrustStorePassword)
      throws IOException {
    final String tomlConfig =
        createTomlConfig(
            host,
            port,
            rootToken,
            keyPath,
            keyName,
            timeout,
            tls,
            tlsTrustStoreType,
            tlsTrustStorePath,
            tlsTrustStorePassword);

    final Path tomlConfigFile = createTempFile("configFile", ".toml");
    tomlConfigFile.toFile().deleteOnExit();
    return writeString(tomlConfigFile, tomlConfig, UTF_8);
  }

  public static String createTomlConfig(
      final String host,
      final Integer port,
      final String rootToken,
      final String keyPath,
      final String keyName,
      final Integer timeout,
      final Boolean tls,
      final String tlsTrustStoreType,
      final String tlsTrustStorePath,
      final String tlsTrustStorePassword) {
    final StringBuilder tomlConfig = new StringBuilder();
    final Formatter formatter = new Formatter(tomlConfig, Locale.US);
    Optional.ofNullable(host)
        .ifPresent(_s -> formatter.format(TOML_STRING_FORMAT, PROP_HASHICORP_SERVER_HOST, host));
    Optional.ofNullable(port)
        .ifPresent(_s -> formatter.format(TOML_NUMBER_FORMAT, PROP_HASHICORP_SERVER_PORT, port));
    Optional.ofNullable(rootToken)
        .ifPresent(_s -> formatter.format(TOML_STRING_FORMAT, PROP_HASHICORP_TOKEN, rootToken));
    Optional.ofNullable(keyPath)
        .ifPresent(_s -> formatter.format(TOML_STRING_FORMAT, PROP_HASHICORP_KEY_PATH, keyPath));
    Optional.ofNullable(keyName)
        .ifPresent(_s -> formatter.format(TOML_STRING_FORMAT, PROP_HASHICORP_KEY_NAME, keyName));
    Optional.ofNullable(timeout)
        .ifPresent(_s -> formatter.format(TOML_NUMBER_FORMAT, PROP_HASHICORP_TIMEOUT, timeout));
    Optional.ofNullable(tls)
        .ifPresent(
            _s ->
                tomlConfig.append(
                    createTlsTomlConfig(
                        tls, tlsTrustStoreType, tlsTrustStorePath, tlsTrustStorePassword)));

    return tomlConfig.toString();
  }

  public static String createTlsTomlConfig(
      final Boolean tls,
      final String tlsTrustStoreType,
      final String tlsTrustStorePath,
      final String tlsTrustStorePassword) {
    final StringBuilder tomlConfig = new StringBuilder();
    final Formatter formatter = new Formatter(tomlConfig, Locale.US);
    Optional.ofNullable(tls)
        .ifPresent(_s -> formatter.format(TOML_BOOLEAN_FORMAT, PROP_HASHICORP_TLS_ENABLE, tls));
    Optional.ofNullable(tlsTrustStoreType)
        .ifPresent(
            _s ->
                formatter.format(
                    TOML_STRING_FORMAT, PROP_HASHICORP_TLS_TS_TYPE, tlsTrustStoreType));
    Optional.ofNullable(tlsTrustStorePath)
        .ifPresent(
            _s ->
                formatter.format(
                    TOML_STRING_FORMAT, PROP_HASHICORP_TLS_TS_PATH, tlsTrustStorePath));
    Optional.ofNullable(tlsTrustStorePassword)
        .ifPresent(
            _s ->
                formatter.format(
                    TOML_STRING_FORMAT, PROP_HASHICORP_TLS_TS_PASSWORD, tlsTrustStorePassword));

    return tomlConfig.toString();
  }
}
