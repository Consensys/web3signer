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

import tech.pegasys.eth2signer.core.multikey.metadata.SignerParser;
import tech.pegasys.eth2signer.core.signing.ArtifactSigner;
import tech.pegasys.eth2signer.core.signing.ArtifactSignerProvider;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DirectoryBackedArtifactSignerProvider implements ArtifactSignerProvider {

  private static final Logger LOG = LogManager.getLogger();
  private final Path configsDirectory;
  private final String fileExtension;
  private final SignerParser signerParser;

  public DirectoryBackedArtifactSignerProvider(
      final Path rootDirectory, final String fileExtension, final SignerParser signerParser) {
    this.configsDirectory = rootDirectory;
    this.fileExtension = fileExtension;
    this.signerParser = signerParser;
  }

  @Override
  public Optional<ArtifactSigner> getSigner(final String signerIdentifier) {
    final String normalisedIdentifier = normaliseIdentifier(signerIdentifier);
    final Optional<ArtifactSignerWithFileName> signer =
        loadSignerForIdentifier(normalisedIdentifier);
    if (signer.isEmpty()) {
      LOG.error("No valid matching metadata file found for the identifier {}", signerIdentifier);
      return Optional.empty();
    }
    final ArtifactSignerWithFileName signerWithFileName = signer.get();
    if (!signerMatchesIdentifier(signerWithFileName, signerIdentifier)) {
      LOG.error(
          "Signing metadata file {} does not correspond to the specified signer identifier {}",
          signerWithFileName.getPath().getFileName(),
          signerWithFileName.getSigner().getIdentifier());
      return Optional.empty();
    }
    return signer.map(ArtifactSignerWithFileName::getSigner);
  }

  @Override
  public Set<String> availableIdentifiers() {
    return loadAvailableSigners().stream()
        .filter(Objects::nonNull)
        .map(ArtifactSignerWithFileName::getSigner)
        .map(ArtifactSigner::getIdentifier)
        .collect(Collectors.toSet());
  }

  private Optional<ArtifactSignerWithFileName> loadSignerForIdentifier(
      final String signerIdentifier) {
    final Filter<Path> pathFilter = signerIdentifierFilenameFilter(signerIdentifier);
    final Collection<ArtifactSignerWithFileName> matchingSigners = findSigners(pathFilter);
    if (matchingSigners.size() > 1) {
      LOG.error(
          "Found multiple signing metadata file matches for signer identifier " + signerIdentifier);
      return Optional.empty();
    } else if (matchingSigners.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(matchingSigners.iterator().next());
    }
  }

  private Collection<ArtifactSignerWithFileName> loadAvailableSigners() {
    return findSigners(this::matchesFileExtension);
  }

  private Collection<ArtifactSignerWithFileName> findSigners(
      final DirectoryStream.Filter<? super Path> filter) {
    final Collection<ArtifactSignerWithFileName> signers = new HashSet<>();

    try (final DirectoryStream<Path> directoryStream =
        Files.newDirectoryStream(configsDirectory, filter)) {
      for (final Path file : directoryStream) {
        try {
          final ArtifactSignerWithFileName artifactSignerWithFileName =
              new ArtifactSignerWithFileName(file, signerParser.parse(file));
          signers.add(artifactSignerWithFileName);
        } catch (Exception e) {
          LOG.error(
              "Error parsing signing metadata file {}: {}", file.getFileName(), e.getMessage());
        }
      }
      return signers;
    } catch (final IOException | SecurityException e) {
      LOG.warn("Error searching for signing metadata files: {}", e.getMessage());
      return Collections.emptySet();
    }
  }

  private Filter<Path> signerIdentifierFilenameFilter(final String signerIdentifier) {
    return entry -> {
      final String baseName = FilenameUtils.getBaseName(entry.toString());
      return matchesFileExtension(entry)
          && baseName.toLowerCase().endsWith(signerIdentifier.toLowerCase());
    };
  }

  private boolean matchesFileExtension(final Path filename) {
    final String extension = FilenameUtils.getExtension(filename.toString());
    return extension.toLowerCase().endsWith(fileExtension.toLowerCase());
  }

  private boolean signerMatchesIdentifier(
      final ArtifactSignerWithFileName signerWithFileName, final String signerIdentifier) {
    final String identifier = signerWithFileName.getSigner().getIdentifier();
    return normaliseIdentifier(identifier).equalsIgnoreCase(normaliseIdentifier(signerIdentifier));
  }

  private String normaliseIdentifier(final String signerIdentifier) {
    return signerIdentifier.toLowerCase().startsWith("0x")
        ? signerIdentifier.substring(2)
        : signerIdentifier;
  }

  private static class ArtifactSignerWithFileName {
    private final Path path;
    private final ArtifactSigner signer;

    public ArtifactSignerWithFileName(final Path path, final ArtifactSigner signer) {
      this.path = path;
      this.signer = signer;
    }

    public Path getPath() {
      return path;
    }

    public ArtifactSigner getSigner() {
      return signer;
    }
  }
}
