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
package tech.pegasys.web3signer.signing.bulkloading;

import tech.pegasys.web3signer.keystorage.common.MappedResults;
import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.EthSecpArtifactSigner;
import tech.pegasys.web3signer.signing.K256ArtifactSigner;
import tech.pegasys.web3signer.signing.secp256k1.filebased.CredentialSigner;
import tech.pegasys.web3signer.signing.secp256k1.util.JsonFilesUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.crypto.exception.CipherException;

public class SecpV3KeystoresBulkLoader {
  private static final Logger LOG = LogManager.getLogger();

  /**
   * Bulk-load Ethereum compatible SECP Artifact Signers from encrypted v3 keystores.
   *
   * @param keystoresPath Path to the directory containing the v3 keystores
   * @param pwrdFileOrDirPath Path to the password file or directory containing the passwords for
   *     the v3 keystores
   * @return MappedResults containing the loaded ArtifactSigners
   */
  public static MappedResults<ArtifactSigner> loadV3KeystoresUsingPasswordFileOrDir(
      final Path keystoresPath, final Path pwrdFileOrDirPath) {
    return loadV3KeystoresUsingPasswordFileOrDir(keystoresPath, pwrdFileOrDirPath, true);
  }

  /**
   * Bulk-load generic SECP Artifact Signers from encrypted v3 keystores that can be used by Commit
   * Boost API. It uses compressed public key as identifier and apply SHA256 digest on the message
   * before signing.
   *
   * @param keystoresPath Path to the directory containing the v3 keystores
   * @param pwrdFileOrDirPath Path to the password file or directory containing the passwords for
   *     the v3 keystores
   * @return MappedResults containing the loaded ArtifactSigners
   */
  public static MappedResults<ArtifactSigner> loadECDSAProxyKeystores(
      final Path keystoresPath, final Path pwrdFileOrDirPath) {
    return loadV3KeystoresUsingPasswordFileOrDir(keystoresPath, pwrdFileOrDirPath, false);
  }

  private static MappedResults<ArtifactSigner> loadV3KeystoresUsingPasswordFileOrDir(
      final Path keystoresPath,
      final Path pwrdFileOrDirPath,
      final boolean ethereumSECPCompatible) {
    if (!Files.exists(pwrdFileOrDirPath)) {
      LOG.error("Password file or directory doesn't exist.");
      return MappedResults.errorResult();
    }

    final List<Path> keystoresFiles;
    try {
      keystoresFiles = JsonFilesUtil.loadJsonExtPaths(keystoresPath);
    } catch (final IOException e) {
      LOG.error("Error listing v3 keystores paths {}", keystoresPath);
      return MappedResults.errorResult();
    }

    final PasswordReader passwordReader;
    if (Files.isDirectory(pwrdFileOrDirPath)) {
      passwordReader = passwordFile -> Files.readString(pwrdFileOrDirPath.resolve(passwordFile));
    } else if (Files.isRegularFile(pwrdFileOrDirPath)) {
      try {
        final String password = Files.readString(pwrdFileOrDirPath);
        passwordReader = passwordFile -> password;
      } catch (final IOException e) {
        LOG.error("Unable to read password file.", e);
        return MappedResults.errorResult();
      }
    } else {
      LOG.error(
          "Unexpected path. Expecting it to be a regular file or directory. {}", pwrdFileOrDirPath);
      return MappedResults.errorResult();
    }

    return keystoresFiles.parallelStream()
        .map(
            keystoreFile ->
                createSecpArtifactSigner(keystoreFile, passwordReader, ethereumSECPCompatible))
        .reduce(MappedResults.newSetInstance(), MappedResults::merge);
  }

  private static MappedResults<ArtifactSigner> createSecpArtifactSigner(
      final Path v3KeystorePath,
      final PasswordReader passwordReader,
      final boolean ethereumSECPCompatible) {
    try {
      final String fileNameWithoutExt =
          FilenameUtils.removeExtension(v3KeystorePath.getFileName().toString());

      final String password = passwordReader.readPassword(fileNameWithoutExt + ".txt");

      final Credentials credentials =
          WalletUtils.loadCredentials(password, v3KeystorePath.toFile());
      final ArtifactSigner artifactSigner =
          ethereumSECPCompatible
              ? new EthSecpArtifactSigner(new CredentialSigner(credentials))
              : new K256ArtifactSigner(credentials.getEcKeyPair());

      return MappedResults.newInstance(Set.of(artifactSigner), 0);
    } catch (final IOException | CipherException | RuntimeException e) {
      LOG.error("Error loading v3 keystore {}", v3KeystorePath, e);
      return MappedResults.errorResult();
    }
  }
}
