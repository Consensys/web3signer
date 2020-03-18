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
import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.artemis.util.mikuli.KeyPair;
import tech.pegasys.artemis.util.mikuli.PublicKey;
import tech.pegasys.artemis.util.mikuli.SecretKey;
import tech.pegasys.eth2signer.dsl.HttpResponse;
import tech.pegasys.eth2signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.eth2signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.signers.bls.keystore.model.KdfFunction;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
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
      "000000000000000000000000000000003ee2224386c82ffea477e2adf28a2929f5c349165a4196158c7f3a2ecca40f35";
  private static final String EXPECTED_SIGNATURE =
      "0x8d4e94e4862aa772500bad94ce9b4abcfd735aa1bb7a8751537cf3ec78eee516262c223a195bae97128047c13b3e250800b8b9a8283598674c3206bf26102d042392559e4425085c548b4f77bcdee66a6a52d7e8832c020a9626733f50634f95";

  private final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();
  private final SecretKey key = SecretKey.fromBytes(Bytes.fromHexString(PRIVATE_KEY));
  private final KeyPair keyPair = new KeyPair(key);

  @TempDir Path testDirectory;

  @ParameterizedTest
  @ValueSource(strings = {"/signer/block", "/signer/attestation", "/signer/randao_reveal"})
  public void signDataWithKeyLoadedFromUnencryptedFile(final String artifactSigningEndpoint)
      throws Exception {
    final String configFilename = keyPair.publicKey().toString().substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createUnencryptedYamlFileAt(keyConfigFile, PRIVATE_KEY);

    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withKeyStoreDirectory(testDirectory);
    startSigner(builder.build());

    final HttpResponse response =
        signer.signData(artifactSigningEndpoint, keyPair.publicKey(), SIGNING_ROOT);
    assertThat(response.getStatusCode()).isEqualTo(HttpResponseStatus.OK.code());
    assertThat(response.getBody()).isEqualToIgnoringCase(EXPECTED_SIGNATURE);
  }

  @ParameterizedTest
  @MethodSource("keystoreValues")
  public void signDataWithKeyLoadedFromKeyStoreFile(
      final String artifactSigningEndpoint, KdfFunction kdfFunction) throws Exception {
    final String configFilename = keyPair.publicKey().toString().substring(2);

    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createKeyStoreYamlFileAt(keyConfigFile, PRIVATE_KEY, kdfFunction);

    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withKeyStoreDirectory(testDirectory);
    startSigner(builder.build());

    final HttpResponse response =
        signer.signData(artifactSigningEndpoint, keyPair.publicKey(), SIGNING_ROOT);
    assertThat(response.getStatusCode()).isEqualTo(HttpResponseStatus.OK.code());
    assertThat(response.getBody()).isEqualToIgnoringCase(EXPECTED_SIGNATURE);
  }

  @Test
  public void receiveA404IfRequestedKeyDoesNotExist() throws Exception {
    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    startSigner(builder.build());

    final HttpResponse response = signer.signData("block", PublicKey.random(), SIGNING_ROOT);
    assertThat(response.getStatusCode()).isEqualTo(HttpResponseStatus.NOT_FOUND.code());
  }

  @Test
  public void receiveA400IfJsonBodyIsMalformed() throws Exception {
    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    startSigner(builder.build());

    final HttpResponse response = signer.postRawRequest("/signer/block", "invalid Body");
    assertThat(response.getStatusCode()).isEqualTo(400);
  }

  @Test
  public void signsDataContainingUnknownFields() throws Exception {
    final String configFilename = keyPair.publicKey().toString().substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createUnencryptedYamlFileAt(keyConfigFile, PRIVATE_KEY);

    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withKeyStoreDirectory(testDirectory);
    startSigner(builder.build());

    final Map<String, String> requestBody = new HashMap<>();
    requestBody.put("publicKey", keyPair.publicKey().toString());
    requestBody.put("signingRoot", SIGNING_ROOT.toString());
    requestBody.put("unknownField", "someValue");

    final String httpBody = Json.encode(requestBody);

    final HttpResponse response = signer.postRawRequest("/signer/block", httpBody);
    assertThat(response.getStatusCode()).isEqualTo(HttpResponseStatus.OK.code());
  }

  @SuppressWarnings("UnusedMethod")
  private static Stream<Arguments> keystoreValues() {
    return Stream.of(
        Arguments.arguments("/signer/block", KdfFunction.SCRYPT),
        Arguments.arguments("/signer/attestation", KdfFunction.SCRYPT),
        Arguments.arguments("/signer/randao_reveal", KdfFunction.SCRYPT),
        Arguments.arguments("/signer/block", KdfFunction.PBKDF2),
        Arguments.arguments("/signer/attestation", KdfFunction.PBKDF2),
        Arguments.arguments("/signer/randao_reveal", KdfFunction.PBKDF2));
  }
}
