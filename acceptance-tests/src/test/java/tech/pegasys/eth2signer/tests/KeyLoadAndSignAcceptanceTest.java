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
package tech.pegasys.eth2signer.tests;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.artemis.bls.BLS;
import tech.pegasys.artemis.bls.BLSKeyPair;
import tech.pegasys.artemis.bls.BLSPublicKey;
import tech.pegasys.artemis.bls.BLSSecretKey;
import tech.pegasys.artemis.bls.BLSSignature;
import tech.pegasys.eth2signer.dsl.HashicorpSigningParams;
import tech.pegasys.eth2signer.dsl.HttpResponse;
import tech.pegasys.eth2signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.eth2signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.signers.bls.keystore.model.KdfFunction;
import tech.pegasys.signers.hashicorp.dsl.DockerClientFactory;
import tech.pegasys.signers.hashicorp.dsl.HashicorpNode;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.Json;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class KeyLoadAndSignAcceptanceTest extends AcceptanceTestBase {

  private static final Bytes SIGNING_ROOT = Bytes.wrap("Hello, world!".getBytes(UTF_8));
  private static final String PRIVATE_KEY =
      "3ee2224386c82ffea477e2adf28a2929f5c349165a4196158c7f3a2ecca40f35";

  private static final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();
  private static final BLSSecretKey key = BLSSecretKey.fromBytes(Bytes.fromHexString(PRIVATE_KEY));
  private static final BLSKeyPair keyPair = new BLSKeyPair(key);
  private static final BLSPublicKey publicKey = keyPair.getPublicKey();
  private static final BLSSignature expectedSignature =
      BLS.sign(keyPair.getSecretKey(), SIGNING_ROOT);

  @TempDir Path testDirectory;

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/signer/block",
        "/signer/attestation",
        "/signer/randao_reveal",
        "/signer/aggregation_slot"
      })
  public void signDataWithKeyLoadedFromUnencryptedFile(final String artifactSigningEndpoint)
      throws Exception {
    final String configFilename = publicKey.toString().substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createUnencryptedYamlFileAt(keyConfigFile, PRIVATE_KEY);

    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withKeyStoreDirectory(testDirectory);
    startSigner(builder.build());

    final HttpResponse response =
        signer.signData(artifactSigningEndpoint, keyPair.getPublicKey(), SIGNING_ROOT);
    assertThat(response.getStatusCode()).isEqualTo(HttpResponseStatus.OK.code());
    assertThat(response.getBody()).isEqualToIgnoringCase(expectedSignature.toString());
  }

  @ParameterizedTest
  @MethodSource("keystoreValues")
  public void signDataWithKeyLoadedFromKeyStoreFile(
      final String artifactSigningEndpoint, KdfFunction kdfFunction) throws Exception {
    final String configFilename = publicKey.toString().substring(2);

    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createKeyStoreYamlFileAt(keyConfigFile, keyPair, kdfFunction);

    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withKeyStoreDirectory(testDirectory);
    startSigner(builder.build());

    final HttpResponse response =
        signer.signData(artifactSigningEndpoint, keyPair.getPublicKey(), SIGNING_ROOT);
    assertThat(response.getStatusCode()).isEqualTo(HttpResponseStatus.OK.code());
    assertThat(response.getBody()).isEqualToIgnoringCase(expectedSignature.toString());
  }

  @Test
  public void receiveA404IfRequestedKeyDoesNotExist() throws Exception {
    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    startSigner(builder.build());

    final HttpResponse response = signer.signData("block", keyPair.getPublicKey(), SIGNING_ROOT);
    assertThat(response.getStatusCode()).isEqualTo(HttpResponseStatus.NOT_FOUND.code());
  }

  @Test
  public void receiveA400IfJsonBodyIsMalformed() throws Exception {
    final String configFilename = keyPair.getPublicKey().toString().substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createUnencryptedYamlFileAt(keyConfigFile, PRIVATE_KEY);

    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withKeyStoreDirectory(testDirectory);
    startSigner(builder.build());

    final String endpoint = "/signer/block/" + keyPair.getPublicKey().toString();
    final HttpResponse response = signer.postRawRequest(endpoint, "invalid Body");
    assertThat(response.getStatusCode()).isEqualTo(400);
  }

  @Test
  public void unusedFieldsInRequestDoesNotAffectSigning() throws Exception {
    final String configFilename = keyPair.getPublicKey().toString().substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createUnencryptedYamlFileAt(keyConfigFile, PRIVATE_KEY);

    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withKeyStoreDirectory(testDirectory);
    startSigner(builder.build());

    final Map<String, String> requestBody = new HashMap<>();
    requestBody.put("publicKey", keyPair.getPublicKey().toString());
    requestBody.put("signingRoot", SIGNING_ROOT.toString());
    requestBody.put("unknownField", "someValue");

    final String httpBody = Json.encode(requestBody);

    final String endpoint = "/signer/block/" + keyPair.getPublicKey().toString();
    final HttpResponse response = signer.postRawRequest(endpoint, httpBody);
    assertThat(response.getStatusCode()).isEqualTo(HttpResponseStatus.OK.code());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/signer/block",
        "/signer/attestation",
        "/signer/randao_reveal",
        "/signer/aggregation_slot"
      })
  public void ableToSignUsingHashicorp(final String artifactSigningEndpoint)
      throws ExecutionException, InterruptedException {
    final String configFilename = keyPair.getPublicKey().toString().substring(2);
    final DockerClientFactory dockerClientFactory = new DockerClientFactory();
    final HashicorpNode hashicorpNode =
        HashicorpNode.createAndStartHashicorp(dockerClientFactory.create(), true);
    try {
      final String secretPath = "acceptanceTestSecretPath";
      final String secretName = "secretName";

      hashicorpNode.addSecretsToVault(singletonMap(secretName, PRIVATE_KEY), secretPath);

      final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
      metadataFileHelpers.createHashicorpYamlFileAt(
          keyConfigFile, new HashicorpSigningParams(hashicorpNode, secretPath, secretName));

      final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
      builder.withKeyStoreDirectory(testDirectory);
      startSigner(builder.build());

      final HttpResponse response =
          signer.signData(artifactSigningEndpoint, keyPair.getPublicKey(), SIGNING_ROOT);
      assertThat(response.getStatusCode()).isEqualTo(HttpResponseStatus.OK.code());
      assertThat(response.getBody()).isEqualToIgnoringCase(expectedSignature.toString());
    } finally {
      hashicorpNode.shutdown();
    }
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> keystoreValues() {
    return Stream.of(
        Arguments.arguments("/signer/block", KdfFunction.SCRYPT),
        Arguments.arguments("/signer/attestation", KdfFunction.SCRYPT),
        Arguments.arguments("/signer/randao_reveal", KdfFunction.SCRYPT),
        Arguments.arguments("/signer/aggregation_slot", KdfFunction.SCRYPT),
        Arguments.arguments("/signer/block", KdfFunction.PBKDF2),
        Arguments.arguments("/signer/attestation", KdfFunction.PBKDF2),
        Arguments.arguments("/signer/randao_reveal", KdfFunction.PBKDF2),
        Arguments.arguments("/signer/aggregation_slot", KdfFunction.PBKDF2));
  }
}
