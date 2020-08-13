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
package tech.pegasys.eth2signer.core.multikey;

import static java.util.Collections.emptySet;

import tech.pegasys.eth2signer.core.multikey.metadata.parser.SignerParser;
import tech.pegasys.eth2signer.core.signing.ArtifactSigner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DirectoryLoader {

  private static final Logger LOG = LogManager.getLogger();
  private final Path configsDirectory;
  private final String fileExtension;
  private final SignerParser signerParser;

  public DirectoryLoader(
      final Path configsDirectory, final String fileExtension, final SignerParser signerParser) {
    this.configsDirectory = configsDirectory;
    this.fileExtension = fileExtension;
    this.signerParser = signerParser;
  }

  public static Collection<ArtifactSigner> loadFromDisk(
      final Path rootDirectory, final String fileExtension, final SignerParser signerParser) {
    final DirectoryLoader loader = new DirectoryLoader(rootDirectory, fileExtension, signerParser);
    final Collection<ArtifactSigner> signers = loader.operateOnFileSubset();
    LOG.info("Loaded {} signers", signers.size());
    return signers;
  }

  public Collection<ArtifactSigner> operateOnFileSubset() {

    try (final Stream<Path> fileStream = Files.list(configsDirectory)) {
      return fileStream
          .parallel()
          .filter(this::matchesFileExtension)
          .map(
              signerConfigFile -> {
                try {
                  return signerParser.parse(signerConfigFile);
                } catch (final Exception e) {
                  renderException(e, signerConfigFile.getFileName().toString());
                  return null;
                }
              })
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());
    } catch (final IOException e) {
      LOG.error("Unable to access the supplied key directory", e);
    }
    return emptySet();
  }

  private boolean matchesFileExtension(final Path filename) {
    final boolean isHidden = filename.toFile().isHidden();
    final String extension = FilenameUtils.getExtension(filename.toString());
    return !isHidden && extension.toLowerCase().endsWith(fileExtension.toLowerCase());
  }

  private void renderException(final Throwable t, final String filename) {
    LOG.error(
        "Error parsing signing metadata file {}: {}",
        filename,
        ExceptionUtils.getRootCauseMessage(t));
    LOG.debug(ExceptionUtils.getStackTrace(t));
  }
}
