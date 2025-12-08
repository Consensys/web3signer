/*
 * Copyright 2025 ConsenSys AG.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.web3signer.BLSTestUtil;
import tech.pegasys.web3signer.common.Web3SignerMetricCategory;
import tech.pegasys.web3signer.keystorage.aws.AwsSecretsManagerProvider;
import tech.pegasys.web3signer.keystorage.common.MappedResults;
import tech.pegasys.web3signer.keystorage.hashicorp.HashicorpConnectionFactory;
import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.BlsArtifactSigner;
import tech.pegasys.web3signer.signing.config.metadata.BlsArtifactSignerFactory;
import tech.pegasys.web3signer.signing.config.metadata.SigningMetadataException;
import tech.pegasys.web3signer.signing.config.metadata.parser.SignerParser;
import tech.pegasys.web3signer.signing.config.metadata.parser.YamlMapperFactory;
import tech.pegasys.web3signer.signing.config.metadata.parser.YamlSignerParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import de.neuland.assertj.logging.ExpectedLogging;
import de.neuland.assertj.logging.ExpectedLoggingAssertions;
import de.neuland.assertj.logging.LogEvent;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.metrics.LabelledMetric;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Enhanced test suite for SignerLoader focusing on: - Timeout behavior - Batch size processing -
 * Sequential vs parallel processing
 */
@ExtendWith(MockitoExtension.class)
public class SignerLoaderEnhancedTest {
  private static final YAMLMapper YAML_MAPPER = YamlMapperFactory.createYamlMapper();
  @TempDir Path configsDirectory;
  @Mock private MetricsSystem metricsSystem;
  @Mock private HashicorpConnectionFactory hashicorpConnectionFactory;
  @Mock private AwsSecretsManagerProvider awsSecretsManagerProvider;
  @Mock private AzureKeyVaultFactory azureKeyVaultFactory;
  @Mock private LabelledMetric<OperationTimer> privateKeyRetrievalTimer;
  @Mock private OperationTimer operationTimer;

  private SignerParser signerParser;
  private SignerLoader signerLoader;

  @RegisterExtension
  private final ExpectedLogging logging = ExpectedLogging.forSource(SignerLoader.class);

  @BeforeEach
  public void setup() {
    // setup metrics system stubbing
    lenient()
        .when(
            metricsSystem.createLabelledTimer(
                Web3SignerMetricCategory.SIGNING,
                "private_key_retrieval_time",
                "Time taken to retrieve private key",
                "signer"))
        .thenReturn(privateKeyRetrievalTimer);

    lenient().when(privateKeyRetrievalTimer.labels(any())).thenReturn(operationTimer);

    final BlsArtifactSignerFactory blsArtifactSignerFactory =
        new BlsArtifactSignerFactory(
            configsDirectory,
            metricsSystem,
            hashicorpConnectionFactory,
            awsSecretsManagerProvider,
            (args) -> new BlsArtifactSigner(args.getKeyPair(), args.getOrigin(), args.getPath()),
            azureKeyVaultFactory);

    signerParser =
        new YamlSignerParser(
            List.of(blsArtifactSignerFactory), YamlMapperFactory.createYamlMapper());
  }

  @AfterEach
  void cleanup() throws Exception {
    if (signerLoader != null) {
      signerLoader.close();
    }
  }

  // ==================== TIMEOUT BEHAVIOR TESTS ====================
  @Test
  @Timeout(30)
  void taskTimeoutCancelsLongRunningTask() throws Exception {
    // Create a slow parser that takes longer than the timeout
    SignerParser slowParser = createSlowParser(10000); // 10 second delay

    createBLSRawConfigFiles(3);

    // Create loader with 2 second timeout
    signerLoader =
        SignerLoader.builder()
            .configsDirectory(configsDirectory)
            .parallelProcess(true)
            .sequentialThreshold(1) // Force parallel processing for testing timeout
            .taskTimeoutSeconds(2)
            .build();

    MappedResults<ArtifactSigner> result = signerLoader.load(slowParser);

    // All tasks should timeout and be cancelled
    assertThat(result.getErrorCount()).isEqualTo(3);
    assertThat(result.getValues()).isEmpty();

    // Verify we got exactly 3 timeout messages
    long timeoutCount =
        logging.getLogEvents().stream()
            .filter(
                event -> event.getMessage().matches("Task timed out after 2 seconds: .*\\.yaml"))
            .count();
    assertThat(timeoutCount).isEqualTo(3);
  }

  // ==================== BATCH SIZE BEHAVIOR TESTS ====================

  @Test
  void batchSizeControlsProgressLogging() throws Exception {
    int fileCount = 250;

    createBLSRawConfigFiles(fileCount);

    signerLoader =
        SignerLoader.builder()
            .configsDirectory(configsDirectory)
            .parallelProcess(true)
            .batchSize(120)
            .build();
    assertThat(signerLoader.getBatchSize()).isEqualTo(120);

    signerLoader.load(signerParser);

    // With 250 files and batch size 120, should process in 3 batches
    // Verify we see batch progress logs
    ExpectedLoggingAssertions.assertThat(logging)
        .hasInfoMessage("Processing 250 files in parallel with batch size 120")
        .hasInfoMessage("Processing batch 1-120 of 250 files")
        .hasInfoMessage("Processing batch 121-240 of 250 files")
        .hasInfoMessage("Processing batch 241-250 of 250 files");
  }

  @Test
  void largeBatchSizeProcessesInSingleBatch() throws Exception {
    int fileCount = 150;

    createBLSRawConfigFiles(fileCount);

    // Use batch size larger than file count
    signerLoader =
        SignerLoader.builder()
            .configsDirectory(configsDirectory)
            .parallelProcess(true)
            .batchSize(200)
            .build();
    assertThat(signerLoader.getBatchSize()).isEqualTo(200);

    MappedResults<ArtifactSigner> result = signerLoader.load(signerParser);

    assertThat(result.getValues()).hasSize(fileCount);
    assertThat(result.getErrorCount()).isZero();

    // Should see parallel processing but not batch-specific messages
    ExpectedLoggingAssertions.assertThat(logging)
        .hasInfoMessage("Processing 150 files in parallel with batch size 200");

    // Verify no batch subdivision occurred by checking all log messages
    assertThat(
            logging.getLogEvents().stream()
                .map(LogEvent::getMessage)
                .noneMatch(msg -> msg.contains("Processing batch")))
        .isTrue();
  }

  @Test
  void batchSizeLessThan100IsClampedTo100() throws Exception {
    int fileCount = 150;
    createBLSRawConfigFiles(fileCount);

    signerLoader =
        SignerLoader.builder()
            .configsDirectory(configsDirectory)
            .parallelProcess(true)
            .batchSize(50) // Should be clamped to 100
            .build();
    assertThat(signerLoader.getBatchSize()).isEqualTo(100);

    signerLoader.load(signerParser);

    // Verify it used 100, not 50
    ExpectedLoggingAssertions.assertThat(logging)
        .hasInfoMessage("Processing 150 files in parallel with batch size 100");
  }

  @Test
  void exactlyOneBatchWhenFilesEqualBatchSize() throws Exception {
    int fileCount = 100;
    createBLSRawConfigFiles(fileCount);

    signerLoader =
        SignerLoader.builder()
            .configsDirectory(configsDirectory)
            .parallelProcess(true)
            .batchSize(100)
            .build();
    assertThat(signerLoader.getBatchSize()).isEqualTo(100);

    signerLoader.load(signerParser);

    // Should see parallel processing but not batch-specific messages
    ExpectedLoggingAssertions.assertThat(logging)
        .hasInfoMessage("Processing 100 files in parallel with batch size 100");

    // Verify no batch subdivision occurred by checking all log messages
    assertThat(
            logging.getLogEvents().stream()
                .map(LogEvent::getMessage)
                .noneMatch(msg -> msg.contains("Processing batch")))
        .isTrue();
  }

  @Test
  void batchProgressOnlyLoggedWhenTotalExceedsBatchSize() throws Exception {
    int fileCount = 150;
    createBLSRawConfigFiles(fileCount);

    signerLoader =
        SignerLoader.builder()
            .configsDirectory(configsDirectory)
            .parallelProcess(true)
            .batchSize(100)
            .build();
    assertThat(signerLoader.getBatchSize()).isEqualTo(100);

    signerLoader.load(signerParser);

    // With 150 files and batch size 100, should see 2 batches
    ExpectedLoggingAssertions.assertThat(logging)
        .hasInfoMessage("Processing 150 files in parallel with batch size 100")
        .hasInfoMessage("Processing batch 1-100 of 150 files")
        .hasInfoMessage("Processing batch 101-150 of 150 files");
  }

  // ==================== SEQUENTIAL VS PARALLEL PROCESSING TESTS ====================

  @Test
  void sequentialProcessingWhenBelowThreshold() throws Exception {
    // Create files below sequential threshold (default 100)
    createBLSRawConfigFiles(50);

    signerLoader =
        SignerLoader.builder()
            .configsDirectory(configsDirectory)
            .parallelProcess(true)
            .sequentialThreshold(100)
            .build();

    MappedResults<ArtifactSigner> result = signerLoader.load(signerParser);

    assertThat(result.getValues()).hasSize(50);
    assertThat(result.getErrorCount()).isZero();

    // Verify sequential processing was used
    ExpectedLoggingAssertions.assertThat(logging)
        .hasInfoMessage("Processing 50 files sequentially");
  }

  @Test
  void parallelProcessingWhenAboveThreshold() throws Exception {
    // Create files above sequential threshold
    createBLSRawConfigFiles(150);

    signerLoader =
        SignerLoader.builder()
            .configsDirectory(configsDirectory)
            .parallelProcess(true)
            .sequentialThreshold(100)
            .build();

    final MappedResults<ArtifactSigner> result = signerLoader.load(signerParser);

    assertThat(result.getValues()).hasSize(150);
    assertThat(result.getErrorCount()).isZero();

    // Verify parallel processing was used
    ExpectedLoggingAssertions.assertThat(logging)
        .hasInfoMessage("Processing 150 files in parallel with batch size 500");
  }

  @Test
  void sequentialProcessingWhenParallelDisabled() throws Exception {
    // Create many files
    createBLSRawConfigFiles(200);

    signerLoader =
        SignerLoader.builder()
            .configsDirectory(configsDirectory)
            .parallelProcess(false) // Explicitly disable parallel processing
            .build();

    final MappedResults<ArtifactSigner> result = signerLoader.load(signerParser);

    assertThat(result.getValues()).hasSize(200);
    assertThat(result.getErrorCount()).isZero();

    // Verify sequential processing was used despite file count
    ExpectedLoggingAssertions.assertThat(logging)
        .hasInfoMessage("Processing 200 files sequentially");
  }

  @Test
  void customSequentialThresholdIsRespected() throws Exception {
    createBLSRawConfigFiles(75);

    // Set custom threshold to 50
    signerLoader =
        SignerLoader.builder()
            .configsDirectory(configsDirectory)
            .parallelProcess(true)
            .sequentialThreshold(50)
            .build();

    final MappedResults<ArtifactSigner> result = signerLoader.load(signerParser);

    assertThat(result.getValues()).hasSize(75);

    // With 75 files and threshold of 50, should use parallel processing
    ExpectedLoggingAssertions.assertThat(logging)
        .hasInfoMessage("Processing 75 files in parallel with batch size 500");
  }

  @Test
  void minimumSequentialThresholdIsEnforced() {
    // sequentialThreshold less than 1 should be clamped to 1
    signerLoader =
        SignerLoader.builder()
            .configsDirectory(configsDirectory)
            .parallelProcess(true)
            .sequentialThreshold(0)
            .build();

    // The minimum is enforced in constructor: Math.max(1, builder.sequentialThreshold)
    assertThat(signerLoader).isNotNull();
    assertThat(signerLoader.getSequentialThreshold()).isOne();
  }

  @Test
  void sequentialProcessingProducesCorrectResults() throws Exception {
    final List<BLSKeyPair> keyPairs = createBLSRawConfigFiles(10);

    signerLoader =
        SignerLoader.builder().configsDirectory(configsDirectory).parallelProcess(false).build();

    final MappedResults<ArtifactSigner> result = signerLoader.load(signerParser);

    assertThat(result.getValues()).hasSize(10);

    // Verify all expected signers are present
    final List<String> expectedIds =
        keyPairs.stream().map(kp -> kp.getPublicKey().toHexString()).collect(Collectors.toList());

    final List<String> actualIds =
        result.getValues().stream().map(ArtifactSigner::getIdentifier).collect(Collectors.toList());

    assertThat(actualIds).containsExactlyInAnyOrderElementsOf(expectedIds);
  }

  // ==================== HELPER METHODS ====================
  private List<BLSKeyPair> createBLSRawConfigFiles(final int numberOfFiles) throws IOException {
    final List<BLSKeyPair> keyPairs = new ArrayList<>();
    for (int i = 0; i < numberOfFiles; i++) {
      final BLSKeyPair blsKey = BLSTestUtil.randomKeyPair(i);
      keyPairs.add(blsKey);
      createFileInConfigsDirectory(
          configFileName(blsKey), blsKey.getSecretKey().toBytes().toHexString());
    }
    return keyPairs;
  }

  private Path createFileInConfigsDirectory(final String fileName, final String privateKeyHex)
      throws IOException {
    final Path file = configsDirectory.resolve(fileName);

    final Map<String, String> unencryptedKeyMetadataFile = new HashMap<>();
    unencryptedKeyMetadataFile.put("type", "file-raw");
    unencryptedKeyMetadataFile.put("privateKey", privateKeyHex);
    final String yamlContent = YAML_MAPPER.writeValueAsString(unencryptedKeyMetadataFile);

    Files.writeString(file, yamlContent);
    assertThat(file).exists();
    return file;
  }

  private static String configFileName(final BLSKeyPair blsKeyPair) {
    return blsKeyPair.getPublicKey().toHexString() + ".yaml";
  }

  /** Creates a parser that introduces a delay to simulate slow processing */
  private SignerParser createSlowParser(long delayMillis) throws SigningMetadataException {
    SignerParser slowParser = spy(signerParser);

    doAnswer(
            invocation -> {
              Thread.sleep(delayMillis);
              return invocation.callRealMethod();
            })
        .when(slowParser)
        .parse(any());

    return slowParser;
  }

  /** Creates a parser that is slow only for files containing "slow" in the name */
  private SignerParser createSelectivelySlowParser() throws SigningMetadataException {
    SignerParser selectiveParser = spy(signerParser);

    doAnswer(
            invocation -> {
              // Check if we're in a slow file context by looking at the current thread
              // This is a bit hacky but works for testing
              String threadName = Thread.currentThread().getName();
              if (threadName != null && threadName.contains("slow")) {
                Thread.sleep(3000); // 3 seconds - longer than timeout
              }
              return invocation.callRealMethod();
            })
        .when(selectiveParser)
        .readSigningMetadata(anyString());

    return selectiveParser;
  }

  /** Creates a parser that tracks concurrent execution */
  private SignerParser createConcurrencyTrackingParser(
      AtomicInteger maxConcurrent, AtomicInteger currentConcurrent)
      throws SigningMetadataException {

    SignerParser trackingParser = spy(signerParser);

    doAnswer(
            invocation -> {
              int current = currentConcurrent.incrementAndGet();
              int max = maxConcurrent.get();
              while (current > max) {
                if (maxConcurrent.compareAndSet(max, current)) {
                  break;
                }
                max = maxConcurrent.get();
              }

              try {
                Thread.sleep(50); // Small delay to ensure concurrency
                return invocation.callRealMethod();
              } finally {
                currentConcurrent.decrementAndGet();
              }
            })
        .when(trackingParser)
        .parse(any());

    return trackingParser;
  }
}
