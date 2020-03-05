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

public class MultiKeyArtifactSignerProvider implements ArtifactSignerProvider {

  private static final Logger LOG = LogManager.getLogger();
  private final Path configsDirectory;
  private SignerParser signerParser;

  public MultiKeyArtifactSignerProvider(final Path rootDirectory, final SignerParser signerParser) {
    this.configsDirectory = rootDirectory;
    this.signerParser = signerParser;
  }

  @Override
  public Optional<ArtifactSigner> getSigner(final String signerIdentifier) {
    final String normalisedIdentifier = normaliseIdentifier(signerIdentifier);
    final Optional<ArtifactSigner> artifactSigner =
        loadSignerForAddress(normalisedIdentifier)
            .filter(
                signer ->
                    normaliseIdentifier(signer.getIdentifier())
                        .equalsIgnoreCase(normalisedIdentifier));
    if (artifactSigner.isEmpty()) {
      LOG.error(
          "Found signing metadata file does not match the signer identifier {}", signerIdentifier);
    }
    return artifactSigner;
  }

  @Override
  public Set<String> availableIdentifiers() {
    return loadAvailableSigners().stream()
        .filter(Objects::nonNull)
        .map(ArtifactSigner::getIdentifier)
        .collect(Collectors.toSet());
  }

  private Optional<ArtifactSigner> loadSignerForAddress(final String signerIdentifier) {
    final Collection<ArtifactSigner> matchingSigners =
        findSigners(
            entry ->
                FilenameUtils.getBaseName(entry.getFileName().toString())
                    .toLowerCase()
                    .endsWith(signerIdentifier.toLowerCase()));
    if (matchingSigners.size() > 1) {
      LOG.error("Found multiple signing metadata file matches for address " + signerIdentifier);
      return Optional.empty();
    } else if (matchingSigners.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(matchingSigners.iterator().next());
    }
  }

  private Collection<ArtifactSigner> loadAvailableSigners() {
    return findSigners((entry) -> true);
  }

  // TODO return tuple with the found fileName for use in error messages?

  private Collection<ArtifactSigner> findSigners(
      final DirectoryStream.Filter<? super Path> filter) {
    final Collection<ArtifactSigner> signers = new HashSet<>();

    try (final DirectoryStream<Path> directoryStream =
        Files.newDirectoryStream(configsDirectory, filter)) {
      for (final Path file : directoryStream) {
        signers.add(signerParser.parse(file));
      }
      return signers;
    } catch (final IOException e) {
      LOG.warn("Error searching for signing metadata files", e);
      return Collections.emptySet();
    }
  }

  private String normaliseIdentifier(final String signerIdentifier) {
    return signerIdentifier.startsWith("0x") ? signerIdentifier.substring(2) : signerIdentifier;
  }
}
