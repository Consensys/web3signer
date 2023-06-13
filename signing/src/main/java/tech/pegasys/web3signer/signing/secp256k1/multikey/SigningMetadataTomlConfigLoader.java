/*
 * Copyright 2019 ConsenSys AG.
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
package tech.pegasys.web3signer.signing.secp256k1.multikey;

import static java.util.Collections.emptyList;

import tech.pegasys.web3signer.keystorage.hashicorp.config.HashicorpKeyConfig;
import tech.pegasys.web3signer.keystorage.hashicorp.config.toml.TomlConfigLoader;
import tech.pegasys.web3signer.signing.secp256k1.azure.AzureConfig;
import tech.pegasys.web3signer.signing.secp256k1.filebased.FileSignerConfig;
import tech.pegasys.web3signer.signing.secp256k1.multikey.metadata.AzureSigningMetadataFile;
import tech.pegasys.web3signer.signing.secp256k1.multikey.metadata.FileBasedSigningMetadataFile;
import tech.pegasys.web3signer.signing.secp256k1.multikey.metadata.HashicorpSigningMetadataFile;
import tech.pegasys.web3signer.signing.secp256k1.multikey.metadata.RawSigningMetadataFile;
import tech.pegasys.web3signer.signing.secp256k1.multikey.metadata.SigningMetadataFile;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.toml.TomlInvalidTypeException;
import org.apache.tuweni.toml.TomlParseResult;
import org.apache.tuweni.toml.TomlTable;

public class SigningMetadataTomlConfigLoader {

  private static final Logger LOG = LogManager.getLogger();

  private final Path tomlConfigsDirectory;

  public SigningMetadataTomlConfigLoader(final Path rootDirectory) {
    this.tomlConfigsDirectory = rootDirectory;
  }

  public Optional<SigningMetadataFile> loadMetadata(
      final DirectoryStream.Filter<Path> configFileSelector) {
    final List<SigningMetadataFile> matchingMetadata =
        loadAvailableSigningMetadataTomlConfigs(configFileSelector);

    if (matchingMetadata.size() > 1) {
      LOG.error(
          "Found multiple signing metadata TOML file matches for filter "
              + configFileSelector.toString());
      return Optional.empty();
    } else if (matchingMetadata.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(matchingMetadata.get(0));
    }
  }

  List<SigningMetadataFile> loadAvailableSigningMetadataTomlConfigs(
      final DirectoryStream.Filter<Path> configFileSelector) {
    final List<SigningMetadataFile> metadataConfigs = Lists.newArrayList();

    try (final DirectoryStream<Path> directoryStream =
        Files.newDirectoryStream(tomlConfigsDirectory, configFileSelector)) {
      for (final Path file : directoryStream) {
        getMetadataInfo(file).ifPresent(metadataConfigs::add);
      }
      return ImmutableList.copyOf(metadataConfigs);
    } catch (final IOException e) {
      LOG.warn("Error searching for signing metadata TOML files", e);
      return emptyList();
    }
  }

  private Optional<SigningMetadataFile> getMetadataInfo(final Path file) {
    final String filename = file.getFileName().toString();

    try {
      final TomlParseResult result =
          TomlConfigFileParser.loadConfigurationFromFile(file.toAbsolutePath().toString());

      final Optional<TomlTableAdapter> signingTable =
          getSigningTableFrom(file.getFileName().toString(), result);
      if (signingTable.isEmpty()) {
        return Optional.empty();
      }

      final String type = signingTable.get().getString("type");
      if (SignerType.fromString(type).equals(SignerType.FILE_BASED_SIGNER)) {
        return getFileBasedSigningMetadataFromToml(filename, result);
      } else if (SignerType.fromString(type).equals(SignerType.AZURE_SIGNER)) {
        return getAzureBasedSigningMetadataFromToml(file.getFileName().toString(), result);
      } else if (SignerType.fromString(type).equals(SignerType.HASHICORP_SIGNER)) {
        return getHashicorpMetadataFromToml(file, result);
      } else if (SignerType.fromString(type).equals(SignerType.RAW_SIGNER)) {
        return getRawMetadataFromToml(filename, result);
      } else {
        LOG.error("Unknown signing type in metadata: " + type);
        return Optional.empty();
      }
    } catch (final IllegalArgumentException | TomlInvalidTypeException e) {
      final String errorMsg = String.format("%s failed to decode: %s", filename, e.getMessage());
      LOG.error(errorMsg);
      return Optional.empty();
    } catch (final Exception e) {
      LOG.error("Could not load TOML file " + file, e);
      return Optional.empty();
    }
  }

  private Optional<SigningMetadataFile> getRawMetadataFromToml(
      final String filename, final TomlParseResult result) {
    final Optional<TomlTableAdapter> signingTable = getSigningTableFrom(filename, result);
    if (signingTable.isEmpty()) {
      return Optional.empty();
    }
    final TomlTableAdapter table = signingTable.get();
    final String privateKeyHexString = table.getString("priv-key");
    return Optional.of(new RawSigningMetadataFile(filename, privateKeyHexString));
  }

  private Optional<SigningMetadataFile> getFileBasedSigningMetadataFromToml(
      final String filename, final TomlParseResult result) {
    final Optional<TomlTableAdapter> signingTable = getSigningTableFrom(filename, result);
    if (signingTable.isEmpty()) {
      return Optional.empty();
    }
    final TomlTableAdapter table = signingTable.get();

    final String keyFilename = table.getString("key-file");
    final Path keyPath = makeRelativePathAbsolute(keyFilename);
    final String passwordFilename = table.getString("password-file");
    final Path passwordPath = makeRelativePathAbsolute(passwordFilename);
    return Optional.of(
        new FileBasedSigningMetadataFile(filename, new FileSignerConfig(keyPath, passwordPath)));
  }

  private Optional<SigningMetadataFile> getAzureBasedSigningMetadataFromToml(
      final String filename, final TomlParseResult result) {

    final Optional<TomlTableAdapter> signingTable = getSigningTableFrom(filename, result);
    if (signingTable.isEmpty()) {
      return Optional.empty();
    }

    final AzureConfig.AzureConfigBuilder builder;
    final TomlTableAdapter table = signingTable.get();
    builder = new AzureConfig.AzureConfigBuilder();
    builder.withKeyVaultName(table.getString("key-vault-name"));
    builder.withKeyName(table.getString("key-name"));
    builder.withKeyVersion(table.getString("key-version"));
    builder.withClientId(table.getString("client-id"));
    builder.withClientSecret(table.getString("client-secret"));
    builder.withTenantId(table.getString("tenant-id"));
    return Optional.of(new AzureSigningMetadataFile(filename, builder.build()));
  }

  private Optional<SigningMetadataFile> getHashicorpMetadataFromToml(
      final Path inputFile, final TomlParseResult result) {

    final String filename = inputFile.getFileName().toString();

    final Optional<TomlTableAdapter> signingTable = getSigningTableFrom(filename, result);
    if (signingTable.isEmpty()) {
      return Optional.empty();
    }

    final HashicorpKeyConfig config = TomlConfigLoader.fromToml(inputFile, "signing");

    return Optional.of(new HashicorpSigningMetadataFile(filename, config));
  }

  private Optional<TomlTableAdapter> getSigningTableFrom(
      final String filename, final TomlParseResult result) {
    final TomlTable signingTable = result.getTable("signing");
    if (signingTable == null) {
      LOG.error(filename + " is a badly formed metadata file - \"signing\" heading is missing.");
      return Optional.empty();
    }
    return Optional.of(new TomlTableAdapter(signingTable));
  }

  private Path makeRelativePathAbsolute(final String input) {
    final Path parsedInput = Path.of(input);

    if (parsedInput.isAbsolute()) {
      return parsedInput;
    }

    return tomlConfigsDirectory.resolve(input);
  }
}
