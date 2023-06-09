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
package tech.pegasys.web3signer.keystorage.hashicorp.config.toml;

import tech.pegasys.web3signer.keystorage.hashicorp.HashicorpException;
import tech.pegasys.web3signer.keystorage.hashicorp.TrustStoreType;
import tech.pegasys.web3signer.keystorage.hashicorp.config.ConnectionParameters;
import tech.pegasys.web3signer.keystorage.hashicorp.config.HashicorpKeyConfig;
import tech.pegasys.web3signer.keystorage.hashicorp.config.KeyDefinition;
import tech.pegasys.web3signer.keystorage.hashicorp.config.TlsOptions;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.tuweni.toml.TomlParseResult;
import org.apache.tuweni.toml.TomlTable;

public class TomlConfigLoader {

  public static final String PROP_HASHICORP_SERVER_PORT = "serverPort";
  public static final String PROP_HASHICORP_TIMEOUT = "timeout";
  public static final String PROP_HASHICORP_SERVER_HOST = "serverHost";
  public static final String PROP_HASHICORP_KEY_PATH = "keyPath";
  public static final String PROP_HASHICORP_KEY_NAME = "keyName";
  public static final String PROP_HASHICORP_TOKEN = "token";

  public static final String PROP_HASHICORP_TLS_ENABLE = "tlsEnable";
  public static final String PROP_HASHICORP_TLS_TS_TYPE = "tlsTrustStoreType";
  public static final String PROP_HASHICORP_TLS_TS_PATH = "tlsTrustStorePath";
  public static final String PROP_HASHICORP_TLS_TS_PASSWORD = "tlsTrustStorePassword";

  /**
   * Expected TOML Format in file <code>
   * [Hashicorp Vault Options]
   * serverHost = <String>
   * serverPort = <int>
   * timeout = <int> (milliseconds)
   * tlsEnabled = <Boolean> (true if required TLS to be used, false otherwise, default to true
   * tlsTrustStoreType = <String> (enum of "JKS", "PKCS", "PEM)
   * tlsTrustStorePath = <String> (path to the trust store file)
   * tlsTrustStorePassword = <String> (the password required to access the truststore)
   *
   * keyPath = <String> (the http endpoint path)
   * keyName = <String> (name of key being loaded)
   * token = <String> (security token added into request header)
   * </code>
   */
  private final Path fileToParse;

  public TomlConfigLoader(final Path fileToParse) {
    this.fileToParse = fileToParse;
  }

  public HashicorpKeyConfig parse(final String tableName) {
    final TomlParser tomlParser = new TomlParser();
    final TomlParseResult tomlResult = tomlParser.getTomlParseResult(fileToParse);
    TomlTable tableToParse = tomlResult;

    if (tableName != null) {
      tableToParse = tomlResult.getTable(tableName);
    }

    if (tableToParse == null) {
      final String error = String.format("Toml table %s is missing", tableName);
      throw new HashicorpException(constructErrorMessage(error));
    }

    final KeyDefinition keyDefinition = loadKeyDefinition(tableToParse);
    final ConnectionParameters connectionsParams = loadConnectionParams(tableToParse);

    return new HashicorpKeyConfig(connectionsParams, keyDefinition);
  }

  public static HashicorpKeyConfig fromToml(final Path input, final String tableName) {
    final TomlConfigLoader loader = new TomlConfigLoader(input);
    return loader.parse(tableName);
  }

  private KeyDefinition loadKeyDefinition(final TomlTable tomlInput) {
    final String keyPath = tomlInput.getString(PROP_HASHICORP_KEY_PATH);
    final String keyName = tomlInput.getString(PROP_HASHICORP_KEY_NAME);
    final String token = tomlInput.getString(PROP_HASHICORP_TOKEN);

    // Enforce required parameters
    if (keyPath == null) {
      throwMissingElementException(PROP_HASHICORP_KEY_PATH);
    }

    if (token == null) {
      throwMissingElementException(PROP_HASHICORP_TOKEN);
    }

    return new KeyDefinition(keyPath, Optional.ofNullable(keyName), token);
  }

  private ConnectionParameters loadConnectionParams(final TomlTable tomlInput) {
    final Optional<TlsOptions> tlsOptions = loadTlsOptions(tomlInput);
    final String serverHost = tomlInput.getString(PROP_HASHICORP_SERVER_HOST);
    final Long serverPort = tomlInput.getLong(PROP_HASHICORP_SERVER_PORT);
    final Long timeoutMs = tomlInput.getLong(PROP_HASHICORP_TIMEOUT);

    // Enforce required parameters
    if (serverHost == null) {
      throwMissingElementException(PROP_HASHICORP_SERVER_HOST);
    }

    return new ConnectionParameters(
        serverHost,
        Optional.ofNullable(serverPort == null ? null : serverPort.intValue()),
        tlsOptions,
        Optional.ofNullable(timeoutMs));
  }

  private Optional<TlsOptions> loadTlsOptions(final TomlTable tomlInput) {
    final boolean tlsEnabled = tomlInput.getBoolean(PROP_HASHICORP_TLS_ENABLE, () -> true);
    final String trustStoreString = tomlInput.getString(PROP_HASHICORP_TLS_TS_TYPE);
    final String trustStorePath = tomlInput.getString(PROP_HASHICORP_TLS_TS_PATH);
    final String trustStorePassword = tomlInput.getString(PROP_HASHICORP_TLS_TS_PASSWORD);

    if (!tlsEnabled) {
      return Optional.empty();
    }

    final TrustStoreType trustStoreType = decodeTrustStoreType(trustStoreString);

    if (trustStoreType != null) {
      if (trustStorePath == null) {
        final String error =
            String.format(
                "%s must be specified if custom trust store (%s) is specified",
                PROP_HASHICORP_TLS_TS_PATH, PROP_HASHICORP_TLS_TS_TYPE);
        throw new HashicorpException(error);
      }

      if (trustStoreType.isPasswordRequired() && (trustStorePassword == null)) {
        final String error =
            String.format(
                "%s must be specified if custom trust store (%s) is specified",
                PROP_HASHICORP_TLS_TS_PASSWORD, trustStoreType.name());
        throw new HashicorpException(constructErrorMessage(error));
      }
    }

    return Optional.of(
        new TlsOptions(
            Optional.ofNullable(trustStoreType),
            trustStorePath == null ? null : Path.of(trustStorePath),
            trustStorePassword));
  }

  private TrustStoreType decodeTrustStoreType(final String trustStoreString) {
    if (trustStoreString == null) {
      return null;
    }

    final Optional<TrustStoreType> trustStoreType = TrustStoreType.fromString(trustStoreString);
    if (trustStoreType.isEmpty()) {
      final String error =
          String.format(
              "%s contains an illegal value (%s) must be (%s)",
              PROP_HASHICORP_TLS_TS_TYPE,
              trustStoreString,
              Arrays.stream(TrustStoreType.values())
                  .map(Enum::name)
                  .collect(Collectors.joining(",")));
      throw new HashicorpException(constructErrorMessage(error));
    }

    return trustStoreType.get();
  }

  private void throwMissingElementException(final String missingParamKey) {
    final String error = String.format("missing key '%s'", missingParamKey);
    throw new HashicorpException(constructErrorMessage(error));
  }

  private String constructErrorMessage(final String error) {
    return String.format("Invalid Hashicorp Vault configuration file (%s): %s", fileToParse, error);
  }
}
