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
package tech.pegasys.web3signer.tests.signing;

import static io.restassured.http.ContentType.ANY;
import static io.restassured.http.ContentType.JSON;
import static io.restassured.http.ContentType.TEXT;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.signers.bls.keystore.model.KdfFunction;
import tech.pegasys.signers.hashicorp.dsl.HashicorpNode;
import tech.pegasys.teku.bls.BLS;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.teku.bls.BLSSignature;
import tech.pegasys.web3signer.core.service.http.ArtifactType;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.Eth2SigningRequestBody;
import tech.pegasys.web3signer.core.signing.KeyType;
import tech.pegasys.web3signer.dsl.HashicorpSigningParams;
import tech.pegasys.web3signer.dsl.utils.Eth2RequestUtils;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;

import java.nio.file.Path;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class BlsSigningAcceptanceTest extends SigningAcceptanceTestBase {

  private static final String PRIVATE_KEY =
      "3ee2224386c82ffea477e2adf28a2929f5c349165a4196158c7f3a2ecca40f35";

  private static final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();
  private static final BLSSecretKey key =
      BLSSecretKey.fromBytes(Bytes32.fromHexString(PRIVATE_KEY));
  private static final BLSKeyPair keyPair = new BLSKeyPair(key);
  private static final BLSPublicKey publicKey = keyPair.getPublicKey();

  @ParameterizedTest
  @EnumSource
  public void signDataWithKeyLoadedFromUnencryptedFile(final ArtifactType artifactType)
      throws JsonProcessingException {
    final String configFilename = publicKey.toString().substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createUnencryptedYamlFileAt(keyConfigFile, PRIVATE_KEY, KeyType.BLS);

    signAndVerifySignature(artifactType, TEXT);
  }

  @ParameterizedTest
  @EnumSource
  public void signDataWithJsonAcceptTypeWithKeyLoadedFromUnencryptedFile(
      final ArtifactType artifactType) throws JsonProcessingException {
    final String configFilename = publicKey.toString().substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createUnencryptedYamlFileAt(keyConfigFile, PRIVATE_KEY, KeyType.BLS);

    signAndVerifySignature(artifactType, JSON);
  }

  @ParameterizedTest
  @EnumSource
  public void signDataWithDefaultAcceptTypeWithKeyLoadedFromUnencryptedFile(
      final ArtifactType artifactType) throws JsonProcessingException {
    final String configFilename = publicKey.toString().substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createUnencryptedYamlFileAt(keyConfigFile, PRIVATE_KEY, KeyType.BLS);
    // this is same as not setting accept type at all - the client defaults to */* aka ANY
    signAndVerifySignature(artifactType, ANY);
  }

  @ParameterizedTest
  @EnumSource(KdfFunction.class)
  public void signDataWithKeyLoadedFromKeyStoreFile(KdfFunction kdfFunction)
      throws JsonProcessingException {
    final String configFilename = publicKey.toString().substring(2);

    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createKeyStoreYamlFileAt(keyConfigFile, keyPair, kdfFunction);

    signAndVerifySignature(ArtifactType.BLOCK);
  }

  @Test
  public void ableToSignUsingHashicorp() throws JsonProcessingException {
    final String configFilename = keyPair.getPublicKey().toString().substring(2);
    final HashicorpNode hashicorpNode = HashicorpNode.createAndStartHashicorp(true);
    try {
      final String secretPath = "acceptanceTestSecretPath";
      final String secretName = "secretName";

      hashicorpNode.addSecretsToVault(singletonMap(secretName, PRIVATE_KEY), secretPath);

      final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
      metadataFileHelpers.createHashicorpYamlFileAt(
          keyConfigFile,
          new HashicorpSigningParams(hashicorpNode, secretPath, secretName, KeyType.BLS));

      signAndVerifySignature(ArtifactType.BLOCK);
    } finally {
      hashicorpNode.shutdown();
    }
  }

  @Test
  @EnabledIfEnvironmentVariables({
    @EnabledIfEnvironmentVariable(named = "AZURE_CLIENT_ID", matches = ".*"),
    @EnabledIfEnvironmentVariable(named = "AZURE_CLIENT_SECRET", matches = ".*"),
    @EnabledIfEnvironmentVariable(named = "AZURE_KEY_VAULT_NAME", matches = ".*"),
    @EnabledIfEnvironmentVariable(named = "AZURE_TENANT_ID", matches = ".*")
  })
  public void ableToSignUsingAzure() throws JsonProcessingException {
    final String clientId = System.getenv("AZURE_CLIENT_ID");
    final String clientSecret = System.getenv("AZURE_CLIENT_SECRET");
    final String tenantId = System.getenv("AZURE_TENANT_ID");
    final String keyVaultName = System.getenv("AZURE_KEY_VAULT_NAME");
    final String secretName = "TEST-KEY";

    final String configFilename = keyPair.getPublicKey().toString().substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createAzureYamlFileAt(
        keyConfigFile, clientId, clientSecret, tenantId, keyVaultName, secretName);

    signAndVerifySignature(ArtifactType.BLOCK);
  }

  @ParameterizedTest
  @EnumSource
  public void failsIfSigningRootDoesNotMatchSigningData(final ArtifactType artifactType)
      throws JsonProcessingException {
    final String configFilename = publicKey.toString().substring(2);

    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createKeyStoreYamlFileAt(keyConfigFile, keyPair, KdfFunction.SCRYPT);

    setupEth2Signer();

    final Eth2SigningRequestBody request = Eth2RequestUtils.createCannedRequest(artifactType);
    final Eth2SigningRequestBody requestWithMismatchedSigningRoot =
        new Eth2SigningRequestBody(
            request.getType(),
            Bytes32.ZERO,
            request.getForkInfo(),
            request.getBlock(),
            request.getBlockRequest(),
            request.getAttestation(),
            request.getAggregationSlot(),
            request.getAggregateAndProof(),
            request.getVoluntaryExit(),
            request.getRandaoReveal(),
            request.getDeposit(),
            request.getSyncCommitteeMessage(),
            request.getSyncAggregatorSelectionData(),
            request.getContributionAndProof());

    final Response response =
        signer.eth2Sign(keyPair.getPublicKey().toString(), requestWithMismatchedSigningRoot);
    assertThat(response.getStatusCode()).isEqualTo(500);
  }

  @ParameterizedTest
  @EnumSource(
      value = ContentType.class,
      names = {"TEXT", "JSON", "ANY"})
  public void ableToSignWithoutSigningRootField(final ContentType acceptableContentType)
      throws JsonProcessingException {
    final String configFilename = publicKey.toString().substring(2);

    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createKeyStoreYamlFileAt(keyConfigFile, keyPair, KdfFunction.SCRYPT);

    setupEth2Signer();

    final Eth2SigningRequestBody request = Eth2RequestUtils.createBlockRequest();

    final Eth2SigningRequestBody requestWithMismatchedSigningRoot =
        new Eth2SigningRequestBody(
            request.getType(),
            null,
            request.getForkInfo(),
            request.getBlock(),
            request.getBlockRequest(),
            request.getAttestation(),
            request.getAggregationSlot(),
            request.getAggregateAndProof(),
            request.getVoluntaryExit(),
            request.getRandaoReveal(),
            request.getDeposit(),
            request.getSyncCommitteeMessage(),
            request.getSyncAggregatorSelectionData(),
            request.getContributionAndProof());

    final Response response =
        signer.eth2Sign(
            keyPair.getPublicKey().toString(),
            requestWithMismatchedSigningRoot,
            acceptableContentType);

    assertThat(response.statusCode()).isEqualTo(200);

    // validate that for ANY and JSON we get application/json content type, and we are able to parse
    // JSON as well.
    if (acceptableContentType == ANY || acceptableContentType == JSON) {
      assertThat(response.contentType()).startsWith("application/json");
      final JsonObject jsonObject = new JsonObject(response.body().print());
      assertThat(jsonObject.containsKey("signature")).isTrue();
    } else {
      assertThat(response.contentType()).startsWith("text/plain");
    }
  }

  private void signAndVerifySignature(final ArtifactType artifactType)
      throws JsonProcessingException {
    signAndVerifySignature(artifactType, TEXT);
  }

  private void signAndVerifySignature(
      final ArtifactType artifactType, final ContentType acceptMediaType)
      throws JsonProcessingException {
    if (artifactType == ArtifactType.BLOCK_V2) {
      setupEth2SignerMinimal();
    } else {
      setupEth2Signer();
    }

    // openapi
    final Eth2SigningRequestBody request = Eth2RequestUtils.createCannedRequest(artifactType);
    final Response response =
        signer.eth2Sign(keyPair.getPublicKey().toString(), request, acceptMediaType);
    final Bytes signature =
        verifyAndGetSignatureResponse(response, expectedContentType(acceptMediaType));
    final BLSSignature expectedSignature =
        BLS.sign(keyPair.getSecretKey(), request.getSigningRoot());
    assertThat(signature).isEqualTo(expectedSignature.toBytesCompressed());
  }

  private ContentType expectedContentType(final ContentType acceptMediaType) {
    return acceptMediaType == ANY || acceptMediaType == JSON ? JSON : TEXT;
  }
}
