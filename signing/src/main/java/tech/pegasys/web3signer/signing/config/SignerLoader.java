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
package tech.pegasys.web3signer.signing.config;

import tech.pegasys.web3signer.keystorage.common.MappedResults;
import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.config.metadata.SigningMetadata;
import tech.pegasys.web3signer.signing.config.metadata.SigningMetadataException;
import tech.pegasys.web3signer.signing.config.metadata.parser.SignerParser;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The SignerLoader loads metadata files and converts them to ArtifactSigners using a smart caching
 * mechanism. This class maintains a cache of loaded signers and automatically handles file
 * additions and deletions. It leverages Java virtual threads for efficient parallel processing of
 * large file sets.
 *
 * <p>Processing strategies:
 *
 * <ul>
 *   <li>Sequential: For small batches (below sequential threshold, default 100 files)
 *   <li>Parallel with virtual threads: For larger batches, processed in configurable batch sizes
 * </ul>
 *
 * <p>This class implements {@link Closeable} for proper resource management. The {@link #close()}
 * method is idempotent as required by the Closeable contract.
 *
 * <p>This class is thread-safe and uses volatile references with immutable maps for lock-free
 * reads. All write operations are atomic through copy-on-write semantics.
 */
public class SignerLoader implements Closeable {
  private static final Logger LOG = LogManager.getLogger();
  private static final Set<String> CONFIG_FILE_EXTENSIONS = Set.of("yaml", "yml");

  private static final int DEFAULT_SEQUENTIAL_THRESHOLD = 100;
  private static final int DEFAULT_BATCH_SIZE = 500;
  private static final int DEFAULT_TASK_TIMEOUT_SECONDS = 60;

  // Required parameters (set via builder)
  private final Path configsDirectory;

  // Optional parameters with defaults
  private final boolean parallelProcess;
  private final int batchSize;
  private final int taskTimeoutSeconds;
  private final int sequentialThreshold;

  private final ExecutorService virtualThreadExecutor;

  private volatile Map<String, CachedSignerData> cachedArtifactSigners = Collections.emptyMap();
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /** Holds cached signer data along with file metadata for cache invalidation. */
  private record CachedSignerData(
      String filePath, FileTime lastModifiedTime, Set<ArtifactSigner> signers) {}

  /** Private constructor - use {@link Builder} to create instances. */
  private SignerLoader(final Builder builder) {
    this.configsDirectory = builder.configsDirectory.toAbsolutePath().normalize();
    this.parallelProcess = builder.parallelProcess;
    this.batchSize = Math.max(100, builder.batchSize);
    this.taskTimeoutSeconds = Math.max(1, builder.taskTimeoutSeconds);
    this.sequentialThreshold = Math.max(1, builder.sequentialThreshold);
    this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
  }

  /**
   * Creates a new builder for SignerLoader.
   *
   * @return a new Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for creating {@link SignerLoader} instances.
   *
   * <p>Required parameters must be set before calling {@link #build()}:
   *
   * <ul>
   *   <li>{@link #configsDirectory(Path)} - location of metadata files
   *   <li>{@link #parallelProcess(boolean)} - whether to enable parallel processing
   * </ul>
   *
   * <p>Optional parameters have sensible defaults:
   *
   * <ul>
   *   <li>batchSize: 500
   *   <li>taskTimeoutSeconds: 60
   *   <li>sequentialThreshold: 100
   * </ul>
   */
  public static class Builder {
    // Required parameters - no defaults
    private Path configsDirectory;

    // Optional parameters - initialized to defaults
    private boolean parallelProcess = true;
    private int batchSize = DEFAULT_BATCH_SIZE;
    private int taskTimeoutSeconds = DEFAULT_TASK_TIMEOUT_SECONDS;
    private int sequentialThreshold = DEFAULT_SEQUENTIAL_THRESHOLD;

    private Builder() {}

    /**
     * Sets the directory containing signer configuration metadata files. This is a required
     * parameter.
     *
     * @param configsDirectory path to the configuration directory
     * @return this builder for chaining
     * @throws NullPointerException if configsDirectory is null
     */
    public Builder configsDirectory(Path configsDirectory) {
      this.configsDirectory =
          Objects.requireNonNull(configsDirectory, "configsDirectory must not be null");
      return this;
    }

    /**
     * Sets whether to process configuration files in parallel. Optional - defaults to true.
     *
     * @param parallelProcess true to enable parallel processing, false for sequential
     * @return this builder for chaining
     */
    public Builder parallelProcess(boolean parallelProcess) {
      this.parallelProcess = parallelProcess;
      return this;
    }

    /**
     * Sets the number of files to process per batch. Optional - defaults to 500. Minimum value is
     * 100.
     *
     * @param batchSize number of files per batch
     * @return this builder for chaining
     */
    public Builder batchSize(int batchSize) {
      this.batchSize = batchSize;
      return this;
    }

    /**
     * Sets the timeout in seconds for each individual file processing task. Optional - defaults to
     * 60 seconds. Minimum value is 1.
     *
     * @param taskTimeoutSeconds timeout per task in seconds
     * @return this builder for chaining
     */
    public Builder taskTimeoutSeconds(int taskTimeoutSeconds) {
      this.taskTimeoutSeconds = taskTimeoutSeconds;
      return this;
    }

    /**
     * Sets the file count threshold below which sequential processing is used. Optional - defaults
     * to 100. Minimum value is 1.
     *
     * @param sequentialThreshold minimum files for parallel processing
     * @return this builder for chaining
     */
    public Builder sequentialThreshold(int sequentialThreshold) {
      this.sequentialThreshold = sequentialThreshold;
      return this;
    }

    /**
     * Builds a new {@link SignerLoader} instance.
     *
     * @return a new SignerLoader configured with this builder's parameters
     * @throws IllegalStateException if required parameters are not set
     */
    public SignerLoader build() {
      if (configsDirectory == null) {
        throw new IllegalStateException("configsDirectory is required");
      }

      return new SignerLoader(this);
    }
  }

  /**
   * Load ArtifactSigners for metadata files. This method maintains a cache of loaded signers and
   * intelligently handles file changes:
   *
   * <ul>
   *   <li>New files are processed and added to cache
   *   <li>Modified files (based on last modified time) are reprocessed
   *   <li>Deleted files are removed from cache
   *   <li>Unchanged files remain in cache without reprocessing
   * </ul>
   *
   * @param signerParser An implementation of SignerParser to parse the metadata files
   * @return A MappedResults of ArtifactSigners and error count
   * @throws IllegalStateException if this SignerLoader has been closed
   */
  public MappedResults<ArtifactSigner> load(final SignerParser signerParser) {
    if (closed.get()) {
      throw new IllegalStateException("SignerLoader instance has been closed");
    }
    LOG.info("Loading signer configuration metadata files from {}", configsDirectory);
    final Instant loadStartTime = Instant.now();

    // Get all metadata file paths from the config directory
    final Map<String, FileTime> availableFilesWithTime;
    try {
      availableFilesWithTime = getMetadataConfigFilesWithTime();
    } catch (final IOException e) {
      LOG.error("Unable to access the supplied key directory", e);
      return MappedResults.errorResult();
    }

    // Get new cache by removing entries that doesn't exist or has modified time
    final Map<String, CachedSignerData> newCache = getNewCacheMap(availableFilesWithTime);

    // Find files to process (new + modified)
    final Set<String> newFilesToProcess =
        getNewFilesToProcess(availableFilesWithTime.keySet(), newCache.keySet());
    LOG.info(
        "Processing {} metadata files. Cached paths: {}",
        newFilesToProcess.size(),
        newCache.size());

    // load new signers
    final Pair<Map<String, Set<ArtifactSigner>>, Integer> newArtifactsWithErrorCount =
        loadNewSigners(newFilesToProcess, signerParser);

    final Map<String, Set<ArtifactSigner>> loadedSigners = newArtifactsWithErrorCount.getLeft();
    final int loadedSignersErrorCount = newArtifactsWithErrorCount.getRight();

    // Add newly loaded signers to cache with their modification times
    loadedSigners.forEach(
        (pathStr, signers) -> {
          FileTime modTime = availableFilesWithTime.get(pathStr);
          if (modTime == null) {
            LOG.warn("No modification time found for {}, using current time", pathStr);
            modTime = FileTime.from(Instant.now());
          }
          newCache.put(pathStr, new CachedSignerData(pathStr, modTime, signers));
          LOG.trace("Added {} signers from {}", signers.size(), pathStr);
        });

    // Update the volatile reference with the new immutable map
    cachedArtifactSigners = Map.copyOf(newCache);

    // Return all ArtifactSigners from the cache
    final Collection<ArtifactSigner> allArtifactSigners =
        cachedArtifactSigners.values().stream()
            .map(CachedSignerData::signers)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

    LOG.info(
        "Total Artifact Signers loaded via configuration files: {}\nTotal Paths cached: {}, Error count: {}\nTime Taken: {}.",
        allArtifactSigners.size(),
        cachedArtifactSigners.size(),
        loadedSignersErrorCount,
        calculateTimeTaken(loadStartTime).orElse("unknown duration"));

    return MappedResults.newInstance(allArtifactSigners, loadedSignersErrorCount);
  }

  /**
   * Identifies files that need to be processed by computing the set difference between available
   * files and unchanged cached files. The result includes both new files (never cached) and
   * modified files (cached but with different timestamps).
   *
   * @param availableFiles all files currently present in the directory
   * @param unchangedCachedFiles files in cache that haven't been modified
   * @return set of file paths that need to be processed (new + modified files)
   */
  private Set<String> getNewFilesToProcess(
      final Set<String> availableFiles, final Set<String> unchangedCachedFiles) {
    final Set<String> filesToProcess = new HashSet<>(availableFiles);
    filesToProcess.removeAll(unchangedCachedFiles);

    // filesToProcess now contains:
    // 1. New files (were never in cache)
    // 2. Modified files (were in cache but have different timestamp)
    return filesToProcess;
  }

  /**
   * Creates a new cache map containing only files that still exist on the filesystem and haven't
   * been modified since last cached. Files are retained in cache only if:
   *
   * <ol>
   *   <li>They still exist in the filesystem
   *   <li>Their last modified time hasn't changed
   * </ol>
   *
   * Modified or deleted files are excluded from the returned cache and will be reprocessed or
   * removed respectively.
   *
   * @param currentFilesWithTime map of current file paths to their last modified times
   * @return new cache map containing only unchanged files
   */
  private Map<String, CachedSignerData> getNewCacheMap(
      final Map<String, FileTime> currentFilesWithTime) {

    final Map<String, CachedSignerData> currentCacheSnapshot = Map.copyOf(cachedArtifactSigners);
    final Map<String, CachedSignerData> newCache = new HashMap<>();

    // Iterate cached files
    for (Map.Entry<String, CachedSignerData> entry : currentCacheSnapshot.entrySet()) {
      final String filePath = entry.getKey();
      final CachedSignerData cachedData = entry.getValue();

      // only add the files which are there on file system and modified time not changed
      if (currentFilesWithTime.containsKey(filePath)) {
        final FileTime currentModTime = currentFilesWithTime.get(filePath);

        // Keep in cache only if unchanged
        if (currentModTime.compareTo(cachedData.lastModifiedTime()) == 0) {
          newCache.put(filePath, cachedData);
        } else {
          LOG.trace("File modified, will reprocess: {}", filePath);
        }
      } else {
        LOG.trace("File deleted: {}", filePath);
      }
    }

    return newCache; // Contains ONLY unchanged cached files
  }

  /**
   * Loads new signers from the specified files using the appropriate processing strategy based on
   * file count and configuration.
   *
   * @param newFilesToProcess files that need to be loaded
   * @param signerParser parser to convert file content to signers
   * @return pair of loaded signers map and error count
   */
  private Pair<Map<String, Set<ArtifactSigner>>, Integer> loadNewSigners(
      final Set<String> newFilesToProcess, final SignerParser signerParser) {

    final AtomicLong configFilesHandled = new AtomicLong(0);
    final AtomicInteger errorCount = new AtomicInteger(0);
    final int totalFiles = newFilesToProcess.size();

    // Sequential processing for small batches or when parallel is disabled
    if (!parallelProcess || totalFiles < sequentialThreshold) {
      LOG.info("Processing {} files sequentially", totalFiles);
      return processSequentially(
          newFilesToProcess, signerParser, configFilesHandled, errorCount, totalFiles);
    }

    // Parallel processing with batches
    LOG.info("Processing {} files in parallel with batch size {}", totalFiles, batchSize);
    return processInBatches(
        newFilesToProcess, signerParser, configFilesHandled, errorCount, totalFiles);
  }

  /**
   * Processes files in batches using virtual threads with individual task timeouts. Batching
   * controls memory usage while individual timeouts ensure fair processing time per file.
   *
   * @param filesToProcess complete set of files to process
   * @param signerParser parser for converting metadata to signers
   * @param configFilesHandled atomic counter for processed files
   * @param errorCount atomic counter for errors
   * @param totalFiles total number of files for progress reporting
   * @return pair of all processed signers and final error count
   */
  private Pair<Map<String, Set<ArtifactSigner>>, Integer> processInBatches(
      final Set<String> filesToProcess,
      final SignerParser signerParser,
      final AtomicLong configFilesHandled,
      final AtomicInteger errorCount,
      final int totalFiles) {

    final Map<String, Set<ArtifactSigner>> allLoadedSigners = new HashMap<>();
    final List<String> fileList = new ArrayList<>(filesToProcess);

    for (int batchStart = 0; batchStart < fileList.size(); batchStart += batchSize) {
      if (closed.get()) {
        LOG.warn("SignerLoader closed during batch processing");
        break;
      }
      final int batchEnd = Math.min(batchStart + batchSize, fileList.size());
      final List<String> batch = fileList.subList(batchStart, batchEnd);

      if (totalFiles > batchSize) {
        LOG.info("Processing batch {}-{} of {} files", batchStart + 1, batchEnd, totalFiles);
      }

      // Submit this batch
      final List<Future<Map.Entry<String, Set<ArtifactSigner>>>> futures = new ArrayList<>();
      for (String pathStr : batch) {
        futures.add(
            virtualThreadExecutor.submit(
                () ->
                    processFile(
                        pathStr, signerParser, configFilesHandled, errorCount, totalFiles)));
      }

      // Collect results with individual timeouts
      if (!collectBatchResults(futures, batch, allLoadedSigners, errorCount)) {
        // Interrupted - stop processing further batches
        break;
      }
    }

    return Pair.of(allLoadedSigners, errorCount.get());
  }

  /**
   * Collects results from a batch of futures, applying individual timeout to each task.
   *
   * @param futures list of submitted futures
   * @param batch corresponding file paths for logging
   * @param allLoadedSigners map to store successful results
   * @param errorCount counter for errors
   * @return true if processing should continue, false if interrupted
   */
  private boolean collectBatchResults(
      final List<Future<Map.Entry<String, Set<ArtifactSigner>>>> futures,
      final List<String> batch,
      final Map<String, Set<ArtifactSigner>> allLoadedSigners,
      final AtomicInteger errorCount) {

    for (int i = 0; i < futures.size(); i++) {
      final Future<Map.Entry<String, Set<ArtifactSigner>>> future = futures.get(i);
      final String filePath = batch.get(i);

      try {
        final Map.Entry<String, Set<ArtifactSigner>> result =
            future.get(taskTimeoutSeconds, TimeUnit.SECONDS);
        if (result != null) {
          allLoadedSigners.put(result.getKey(), result.getValue());
        }
      } catch (TimeoutException e) {
        LOG.error("Task timed out after {} seconds: {}", taskTimeoutSeconds, filePath);
        future.cancel(true);
        errorCount.incrementAndGet();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        LOG.error("Interrupted while collecting batch results");
        cancelAllFutures(futures);
        return false;
      } catch (ExecutionException e) {
        LOG.warn("Task execution failed: {}", filePath, e);
      } catch (CancellationException e) {
        LOG.info("Task was cancelled: {}", filePath);
      }
    }
    return true;
  }

  private static void cancelAllFutures(
      final List<Future<Map.Entry<String, Set<ArtifactSigner>>>> futures) {
    for (Future<?> f : futures) {
      if (!f.isDone()) {
        f.cancel(true);
      }
    }
  }

  /**
   * Processes files sequentially without parallelization. Used for small batches or when parallel
   * processing is disabled.
   *
   * @param newFilesToProcess files to process
   * @param signerParser parser for converting metadata to signers
   * @param configFilesHandled counter for processed files
   * @param errorCount counter for errors encountered
   * @param totalFiles total number of files for progress reporting
   * @return pair of processed signers and final error count
   */
  private static Pair<Map<String, Set<ArtifactSigner>>, Integer> processSequentially(
      final Set<String> newFilesToProcess,
      final SignerParser signerParser,
      final AtomicLong configFilesHandled,
      final AtomicInteger errorCount,
      final int totalFiles) {

    final Map<String, Set<ArtifactSigner>> loadedArtSigners = new HashMap<>();

    for (String pathStr : newFilesToProcess) {
      Map.Entry<String, Set<ArtifactSigner>> result =
          processFile(pathStr, signerParser, configFilesHandled, errorCount, totalFiles);
      if (result != null) {
        loadedArtSigners.put(result.getKey(), result.getValue());
      }
    }

    return Pair.of(loadedArtSigners, errorCount.get());
  }

  /**
   * Processes a single metadata file and converts it to ArtifactSigners.
   *
   * <p>This method handles the complete lifecycle of processing a signer configuration file:
   *
   * <ol>
   *   <li><b>File Reading (IO-bound):</b> Reads the file content from disk using NIO operations
   *   <li><b>Metadata Parsing (Mixed IO/CPU):</b> Parses the YAML/YML content into SigningMetadata
   *       objects
   *   <li><b>Signer Creation (CPU-bound):</b> Performs decryption and creates ArtifactSigner
   *       instances
   * </ol>
   *
   * <p>The method includes comprehensive error handling and thread interruption checks at strategic
   * points to ensure responsive cancellation when running in virtual threads. Interruption checks
   * are placed:
   *
   * <ul>
   *   <li>Before starting any work (early exit optimization)
   *   <li>After IO operations (NIO channels may be affected by interruption)
   *   <li>Before CPU-intensive decryption operations (which can take up to a minute)
   * </ul>
   *
   * <p><b>Thread Safety:</b> This method is thread-safe and can be called concurrently from
   * multiple virtual threads. The shared counters use atomic operations for thread-safe updates.
   *
   * <p><b>Error Handling:</b> All errors are logged with appropriate detail levels and increment
   * the error counter. The method returns {@code null} for any processing failure rather than
   * propagating exceptions, allowing batch processing to continue with other files.
   *
   * @param pathStr absolute normalized path to the metadata file to process
   * @param signerParser parser implementation for converting metadata to ArtifactSigners
   * @param configFilesHandled atomic counter tracking total files processed (for progress
   *     reporting)
   * @param errorCount atomic counter tracking processing errors across all threads
   * @param totalFiles total number of files being processed in this batch (for progress
   *     calculation)
   * @return a Map.Entry with the file path as key and immutable Set of ArtifactSigners as value, or
   *     {@code null} if processing failed or was interrupted
   * @implNote The returned Set of ArtifactSigners is made immutable using {@code Set.copyOf()} to
   *     ensure thread safety when cached. The decryption step (Step 3) is the most time-consuming
   *     operation and may involve HSM operations, encrypted key material decryption, or remote key
   *     vault access.
   */
  private static Map.Entry<String, Set<ArtifactSigner>> processFile(
      final String pathStr,
      final SignerParser signerParser,
      final AtomicLong configFilesHandled,
      final AtomicInteger errorCount,
      final int totalFiles) {

    // Check interruption at the beginning
    if (Thread.currentThread().isInterrupted()) {
      LOG.debug("File processing interrupted before start: {}", pathStr);
      errorCount.incrementAndGet();
      return null;
    }

    reportProgress(configFilesHandled, totalFiles);

    try {
      // Step 1: File reading (IO-bound)
      final Path filePath = Path.of(pathStr);
      final String content = Files.readString(filePath, StandardCharsets.UTF_8);

      // Check interruption after IO operation
      if (Thread.currentThread().isInterrupted()) {
        LOG.debug("File processing interrupted after reading: {}", pathStr);
        errorCount.incrementAndGet();
        return null;
      }

      // Step 2: Parse metadata (mixed IO/CPU)
      final List<SigningMetadata> signingMetadata;
      try {
        signingMetadata = signerParser.readSigningMetadata(content);
      } catch (final SigningMetadataException e) {
        LOG.error(
            "Error parsing metadata file {} to signing metadata: {}",
            pathStr,
            ExceptionUtils.getRootCauseMessage(e));
        errorCount.incrementAndGet();
        return null;
      }

      // Check interruption before expensive decryption operation
      if (Thread.currentThread().isInterrupted()) {
        LOG.debug("File processing interrupted before decryption: {}", pathStr);
        errorCount.incrementAndGet();
        return null;
      }

      // Step 3: Decryption and conversion (CPU-bound, can take up to a minute)
      final Set<ArtifactSigner> artifactSigners;
      try {
        artifactSigners = Set.copyOf(signerParser.parse(signingMetadata));
      } catch (final SigningMetadataException e) {
        LOG.error(
            "Error converting signing metadata to Artifact Signer for file {}: {}",
            pathStr,
            ExceptionUtils.getRootCauseMessage(e));
        errorCount.incrementAndGet();
        return null;
      }

      LOG.trace("Successfully processed file {} with {} signers", pathStr, artifactSigners.size());
      return new SimpleEntry<>(pathStr, artifactSigners);

    } catch (final IOException e) {
      // Check if IOException was caused by interruption
      if (Thread.currentThread().isInterrupted()) {
        LOG.debug("File processing interrupted during IO: {}", pathStr);
      } else {
        LOG.error("Error reading metadata config file: {}", pathStr, e);
      }
      errorCount.incrementAndGet();
      return null;
    } catch (final Exception e) {
      LOG.error("Unexpected error processing file: {}", pathStr, e);
      errorCount.incrementAndGet();
      return null;
    }
  }

  /**
   * Reports processing progress at dynamic intervals based on total file count. Ensures
   * approximately 100 progress reports regardless of file count, with a minimum interval of 10
   * files.
   *
   * @param processed atomic counter of processed files
   * @param total total number of files being processed
   */
  private static void reportProgress(final AtomicLong processed, final int total) {
    long count = processed.incrementAndGet();
    // Report ~100 times max, or every 10 files minimum
    int interval = Math.max(10, total / 100);
    if (count % interval == 0 || count == total) {
      final int percentage = (int) Math.round((count * 100.0) / total);
      LOG.info("Processed {}/{} files ({}%)", count, total, percentage);
    }
  }

  /**
   * Closes this SignerLoader and releases resources. This method is idempotent - calling it
   * multiple times has no additional effect.
   *
   * @throws IOException if an I/O error occurs during shutdown
   */
  @Override
  public void close() throws IOException {
    if (!closed.compareAndSet(false, true)) {
      return;
    }

    if (virtualThreadExecutor != null) {
      virtualThreadExecutor.shutdown();
      try {
        if (!virtualThreadExecutor.awaitTermination(5, TimeUnit.MINUTES)) {
          LOG.warn("Executor did not terminate in 5 minutes, forcing shutdown");
          virtualThreadExecutor.shutdownNow();
        }
      } catch (InterruptedException e) {
        LOG.error("Interrupted while waiting for executor shutdown", e);
        virtualThreadExecutor.shutdownNow();
        Thread.currentThread().interrupt();
        throw new IOException("Interrupted during close", e);
      }
    }

    // Clear cache to release memory - AFTER executor is shut down
    cachedArtifactSigners = Collections.emptyMap();
  }

  /**
   * Get all metadata config file paths with their last modified times from the directory.
   *
   * @return A map of normalized path strings to their last modified times
   * @throws IOException If there is an error reading the config directory
   */
  private Map<String, FileTime> getMetadataConfigFilesWithTime() throws IOException {
    if (!Files.exists(configsDirectory)) {
      LOG.warn("Config directory does not exist: {}", configsDirectory);
      return Collections.emptyMap();
    }

    if (!Files.isDirectory(configsDirectory)) {
      throw new IOException("Path is not a directory: " + configsDirectory);
    }

    try (final Stream<Path> fileStream = Files.list(configsDirectory)) {
      return fileStream
          .filter(SignerLoader::validFileExtension)
          .collect(
              Collectors.toMap(
                  path -> path.toAbsolutePath().normalize().toString(),
                  path -> {
                    try {
                      return Files.getLastModifiedTime(path);
                    } catch (IOException e) {
                      LOG.warn("Could not get last modified time for {}, using current time", path);
                      return FileTime.from(Instant.now());
                    }
                  }));
    }
  }

  @VisibleForTesting
  static Optional<String> calculateTimeTaken(final Instant start) {
    final Instant now = Instant.now();
    final long timeTaken = Duration.between(start, now).toMillis();
    if (timeTaken < 0) {
      LOG.warn("System Clock returned time in past. Start: {}, Now: {}.", start, now);
      return Optional.empty();
    }
    return Optional.of(DurationFormatUtils.formatDurationHMS(timeTaken));
  }

  /**
   * Validates if a file has an acceptable extension for processing. Filters out hidden files and
   * only accepts .yaml and .yml extensions.
   *
   * @param filename path to validate
   * @return true if file should be processed, false otherwise
   */
  private static boolean validFileExtension(final Path filename) {
    final boolean isHidden = filename.toFile().isHidden();
    final String extension = FilenameUtils.getExtension(filename.toString());
    return !isHidden && CONFIG_FILE_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));
  }
}
