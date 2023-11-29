/*
 * Copyright 2022 ConsenSys AG.
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
package tech.pegasys.web3signer.signing.config.metadata.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static tech.pegasys.web3signer.signing.config.metadata.parser.YamlSignerParserTest.getFileKeystoreConfigMetadata;
import static tech.pegasys.web3signer.signing.config.metadata.parser.YamlSignerParserTest.getFileRawConfigYaml;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.web3signer.BLSTestUtil;
import tech.pegasys.web3signer.KeystoreUtil;
import tech.pegasys.web3signer.common.Web3SignerMetricCategory;
import tech.pegasys.web3signer.keystorage.aws.AwsSecretsManagerProvider;
import tech.pegasys.web3signer.keystorage.hashicorp.HashicorpConnectionFactory;
import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.BlsArtifactSigner;
import tech.pegasys.web3signer.signing.KeyType;
import tech.pegasys.web3signer.signing.config.AzureKeyVaultFactory;
import tech.pegasys.web3signer.signing.config.metadata.BlsArtifactSignerFactory;
import tech.pegasys.web3signer.signing.config.metadata.SigningMetadataException;
import tech.pegasys.web3signer.signing.config.metadata.interlock.InterlockKeyProvider;
import tech.pegasys.web3signer.signing.config.metadata.yubihsm.YubiHsmOpaqueDataProvider;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.metrics.LabelledMetric;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class YamlSignerParserMultiReadTest {

  private final BLSKeyPair blsKeyPair1 = BLSTestUtil.randomKeyPair(1);
  private final BLSKeyPair blsKeyPair2 = BLSTestUtil.randomKeyPair(2);
  @Mock private MetricsSystem metricsSystem;
  @Mock private HashicorpConnectionFactory hashicorpConnectionFactory;
  @Mock private InterlockKeyProvider interlockKeyProvider;
  @Mock private YubiHsmOpaqueDataProvider yubiHsmOpaqueDataProvider;
  @Mock private AwsSecretsManagerProvider awsSecretsManagerProvider;
  @Mock private AzureKeyVaultFactory azureKeyVaultFactory;
  @Mock private LabelledMetric<OperationTimer> privateKeyRetrievalTimer;
  @Mock private OperationTimer operationTimer;

  @TempDir Path configDir;

  private YamlSignerParser signerParser;

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
            configDir,
            metricsSystem,
            hashicorpConnectionFactory,
            interlockKeyProvider,
            yubiHsmOpaqueDataProvider,
            awsSecretsManagerProvider,
            (args) -> new BlsArtifactSigner(args.getKeyPair(), args.getOrigin(), args.getPath()),
            azureKeyVaultFactory);

    signerParser =
        new YamlSignerParser(
            List.of(blsArtifactSignerFactory), YamlMapperFactory.createYamlMapper());
  }

  @Test
  void readMultiDoc() {
    final String prvKey1 = blsKeyPair1.getSecretKey().toBytes().toHexString();
    final String prvKey2 = blsKeyPair2.getSecretKey().toBytes().toHexString();

    final String multiYaml =
        String.format(
            "---\n"
                + "privateKey: \"%s\"\n"
                + "type: \"file-raw\"\n"
                + "---\n"
                + "privateKey: \"%s\"\n"
                + "type: \"file-raw\"",
            prvKey1, prvKey2);

    final List<ArtifactSigner> signingMetadataList =
        signerParser.parse(signerParser.readSigningMetadata(multiYaml));

    assertThat(signingMetadataList).hasSize(2);
  }

  @Test
  void readMultiDocWithDifferentTypes() throws Exception {
    // encrypted keystore type
    final Map.Entry<Path, Path> keystoreFiles =
        KeystoreUtil.createKeystore(blsKeyPair1, configDir, configDir, "password");
    final String fileKeystoreMetadataYaml =
        getFileKeystoreConfigMetadata(keystoreFiles.getKey(), keystoreFiles.getValue());

    // raw file type
    final String prvKey2 = blsKeyPair2.getSecretKey().toBytes().toHexString();
    final String rawKeystoreMetadataYaml = getFileRawConfigYaml(prvKey2, KeyType.BLS);

    // combine two different types in yaml multi-doc format
    final String multiDocYaml =
        String.format("%s%n%s", fileKeystoreMetadataYaml, rawKeystoreMetadataYaml);

    // parse and assert results
    final List<ArtifactSigner> result =
        signerParser.parse(signerParser.readSigningMetadata(multiDocYaml));
    final Set<String> publicKeyIdentifiers =
        result.stream().map(ArtifactSigner::getIdentifier).collect(Collectors.toSet());

    assertThat(publicKeyIdentifiers).hasSize(2);
    assertThat(publicKeyIdentifiers)
        .containsExactlyInAnyOrder(
            blsKeyPair1.getPublicKey().toHexString(), blsKeyPair2.getPublicKey().toHexString());
  }

  @Test
  void invalidMultiDocThrowsException() {
    final String prvKey1 = blsKeyPair1.getSecretKey().toBytes().toHexString();
    final String prvKey2 = blsKeyPair2.getSecretKey().toBytes().toHexString();

    // missing type:
    final String multiYaml =
        String.format(
            "---\n"
                + "privateKey: \"%s\"\n"
                + "type: \"file-raw\"\n"
                + "---\n"
                + "privateKey: \"%s\"",
            prvKey1, prvKey2);

    assertThatExceptionOfType(SigningMetadataException.class)
        .isThrownBy(() -> signerParser.parse(signerParser.readSigningMetadata(multiYaml)))
        .withMessage("Invalid signing metadata file format");
  }
}
