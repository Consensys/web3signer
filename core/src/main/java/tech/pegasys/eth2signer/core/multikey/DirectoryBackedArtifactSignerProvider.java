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
package tech.pegasys.eth2signer.core.multikey;

import static java.util.Collections.emptySet;

import tech.pegasys.eth2signer.core.multikey.metadata.parser.SignerParser;
import tech.pegasys.eth2signer.core.signing.ArtifactSigner;
import tech.pegasys.eth2signer.core.signing.ArtifactSignerProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DirectoryBackedArtifactSignerProvider implements ArtifactSignerProvider {

  private static final Logger LOG = LogManager.getLogger();
  private final Path configsDirectory;
  private final String fileExtension;
  private final SignerParser signerParser;
  private final Map<String, ArtifactSigner> artifactSigners = new ConcurrentHashMap<>();

  public DirectoryBackedArtifactSignerProvider(
      final Path rootDirectory, final String fileExtension, final SignerParser signerParser) {
    this.configsDirectory = rootDirectory;
    this.fileExtension = fileExtension;
    this.signerParser = signerParser;
  }

  @Override
  public Optional<ArtifactSigner> getSigner(final String signerIdentifier) {
    final String normalisedIdentifier = normaliseIdentifier(signerIdentifier);
    final Optional<ArtifactSigner> result =
        Optional.ofNullable(artifactSigners.get(normalisedIdentifier));

    if (result.isEmpty()) {
      LOG.error("No signer was loaded matching identifitier '{}'", signerIdentifier);
    }
    return result;
  }

  @Override
  public Set<String> availableIdentifiers() {
    return artifactSigners
        .keySet()
        .parallelStream()
        .map(id -> "0x" + id)
        .collect(Collectors.toSet());
  }

  public void loadSigners() {
    final Collection<ArtifactSigner> allSigners =
        operateOnFileSubset(this::matchesFileExtension, signerParser::parse);

    allSigners
        .parallelStream()
        .forEach(
            signer -> artifactSigners.put(normaliseIdentifier(signer.getIdentifier()), signer));
    LOG.info("Loaded {} signers", allSigners.size());
  }

  @VisibleForTesting
  protected Map<String, ArtifactSigner> getArtifactSignerCache() {
    return artifactSigners;
  }

  private <T> Collection<T> operateOnFileSubset(
      final Predicate<? super Path> filter, final Function<Path, T> mapper) {

    try (final Stream<Path> fileStream = Files.list(configsDirectory)) {
      return fileStream
          .parallel()
          .filter(filter)
          .map(
              signerConfigFile -> {
                try {
                  return mapper.apply(signerConfigFile);
                } catch (final Exception e) {
                  renderException(e, signerConfigFile.getFileName().toString());
                  return null;
                }
              })
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());
    } catch (final IOException e) {
      LOG.error("Didn't want this one", e);
    }
    return emptySet();
  }

  private boolean matchesFileExtension(final Path filename) {
    final boolean isHidden = filename.toFile().isHidden();
    final String extension = FilenameUtils.getExtension(filename.toString());
    return !isHidden && extension.toLowerCase().endsWith(fileExtension.toLowerCase());
  }

  private String normaliseIdentifier(final String signerIdentifier) {
    return signerIdentifier.toLowerCase().startsWith("0x")
        ? signerIdentifier.substring(2)
        : signerIdentifier;
  }

  private void renderException(final Throwable t, final String filename) {
    LOG.error(
        "Error parsing signing metadata file {}: {}",
        filename,
        ExceptionUtils.getRootCauseMessage(t));
    LOG.debug(ExceptionUtils.getStackTrace(t));
  }
}
