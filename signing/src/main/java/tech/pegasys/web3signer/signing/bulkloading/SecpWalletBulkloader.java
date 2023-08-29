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
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

public class SecpWalletBulkloader {
  private static final Logger LOG = LogManager.getLogger();

  public static MappedResults<ArtifactSigner> loadWalletsUsingPasswordFileOrDir(
      final Path walletsDirectory, final Path passwordsFileOrDirectory) {
    if (!Files.exists(passwordsFileOrDirectory)) {
      LOG.error("Password file or directory doesn't exist.");
      return MappedResults.errorResult();
    }

    final List<Path> walletFiles;
    try {
      walletFiles = JsonFilesUtil.loadJsonExtPaths(walletsDirectory);
    } catch (final IOException e) {
      LOG.error("Error listing v3 wallet paths {}", walletsDirectory);
      return MappedResults.errorResult();
    }

    final PasswordReader passwordReader;
    if (Files.isDirectory(passwordsFileOrDirectory)) {
      passwordReader =
          passwordFile -> Files.readString(passwordsFileOrDirectory.resolve(passwordFile));
    } else if (Files.isRegularFile(passwordsFileOrDirectory)) {
      try {
        final String password = Files.readString(passwordsFileOrDirectory);
        passwordReader = passwordFile -> password;
      } catch (final IOException e) {
        LOG.error("Unable to read password file.", e);
        return MappedResults.errorResult();
      }
    } else {
      LOG.error("Unexpected password file or directory.");
      return MappedResults.errorResult();
    }

    return walletFiles.parallelStream()
        .map(walletFile -> createSecpArtifactSigner(walletFile, passwordReader))
        .reduce(MappedResults.newSetInstance(), MappedResults::merge);
  }

  private static MappedResults<ArtifactSigner> createSecpArtifactSigner(
      final Path wallet, final PasswordReader passwordReader) {
    try {
      final String fileNameWithoutExt =
          FilenameUtils.removeExtension(wallet.getFileName().toString());

      final String password = passwordReader.readPassword(fileNameWithoutExt + ".txt");

      final Credentials credentials = WalletUtils.loadCredentials(password, wallet.toFile());
      final EthSecpArtifactSigner artifactSigner =
          new EthSecpArtifactSigner(new CredentialSigner(credentials));
      return MappedResults.newInstance(Set.of(artifactSigner), 0);
    } catch (final IOException | CipherException | RuntimeException e) {
      LOG.error("v3 Wallet could not be loaded {}", wallet, e);
      return MappedResults.errorResult();
    }
  }
}
