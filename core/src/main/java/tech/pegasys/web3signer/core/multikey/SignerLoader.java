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
package tech.pegasys.web3signer.core.multikey;

import static java.util.Collections.emptyList;

import tech.pegasys.web3signer.core.multikey.metadata.parser.SignerParser;
import tech.pegasys.web3signer.core.signing.ArtifactSigner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SignerLoader {

  private static final Logger LOG = LogManager.getLogger();
  private static final long FILES_PROCESSED_TO_REPORT = 10;

  public static Collection<ArtifactSigner> load(
      final Path configsDirectory, final String fileExtension, final SignerParser signerParser) {

    LOG.info("Loading signer configuration files from {}", configsDirectory);
    final AtomicLong configFilesHandled = new AtomicLong();

    final List<Path> configFilesList = getConfigFilesList(configsDirectory);
    LOG.info("Processing {} files from configuration directory", configFilesList.size());

    // use custom ForkJoin with limited number of threads if default is greater than 10
    Set<ArtifactSigner> artifactSigners = Collections.emptySet();
    ForkJoinPool forkJoinPool = null;
    try {
      forkJoinPool = new ForkJoinPool(numberOfThreads());
      artifactSigners =
          forkJoinPool
              .submit(
                  () ->
                      configFilesList.stream()
                          .parallel()
                          .filter(path -> matchesFileExtension(fileExtension, path))
                          .flatMap(
                              signerConfigFile -> {
                                final long filesProcessed = configFilesHandled.incrementAndGet();
                                if (filesProcessed % FILES_PROCESSED_TO_REPORT == 0) {
                                  LOG.info(
                                      "{} files processed from configuration directory",
                                      filesProcessed);
                                }
                                try {
                                  return signerParser.parse(signerConfigFile).stream();
                                } catch (final Exception e) {
                                  renderException(e, signerConfigFile.getFileName().toString());
                                  return null;
                                }
                              })
                          .filter(Objects::nonNull)
                          .collect(Collectors.toSet()))
              .get();
    } catch (final InterruptedException | ExecutionException e) {
      LOG.error(
          "Unexpected error in processing configuration files in parallel: {}", e.getMessage(), e);
    } finally {
      if (forkJoinPool != null) {
        forkJoinPool.shutdown();
      }
    }

    LOG.info("Total signers loaded from configuration files: {}", artifactSigners.size());

    return artifactSigners;
  }

  private static List<Path> getConfigFilesList(final Path configsDirectory) {
    try (final Stream<Path> fileStream = Files.list(configsDirectory)) {
      return fileStream.collect(Collectors.toList());
    } catch (final IOException e) {
      LOG.error("Unable to access the supplied key directory", e);
    }

    return emptyList();
  }

  private static boolean matchesFileExtension(
      final String validFileExtension, final Path filename) {
    final boolean isHidden = filename.toFile().isHidden();
    final String extension = FilenameUtils.getExtension(filename.toString());
    return !isHidden && extension.toLowerCase().endsWith(validFileExtension.toLowerCase());
  }

  private static void renderException(final Throwable t, final String filename) {
    LOG.error(
        "Error parsing signing metadata file {}: {}",
        filename,
        ExceptionUtils.getRootCauseMessage(t));
    LOG.debug(ExceptionUtils.getStackTrace(t));
  }

  private static int numberOfThreads() {
    int defaultNumberOfThreads = Runtime.getRuntime().availableProcessors() - 1;
    // we only want to use 10 threads at max
    if (defaultNumberOfThreads >= 10) {
      return 10;
    } else if (defaultNumberOfThreads < 1) {
      return 1;
    }
    return defaultNumberOfThreads;
  }
}
