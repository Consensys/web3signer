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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.web3signer.core.service.http.ArtifactType.AGGREGATION_SLOT;
import static tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers.copyYubiHsmSimulator;

import tech.pegasys.signers.bls.keystore.model.KdfFunction;
import tech.pegasys.signers.hashicorp.dsl.HashicorpNode;
import tech.pegasys.teku.bls.BLS;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.teku.bls.BLSSignature;
import tech.pegasys.web3signer.core.signing.KeyType;
import tech.pegasys.web3signer.dsl.HashicorpSigningParams;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;

import java.io.IOException;
import java.nio.file.Path;

import io.restassured.response.Response;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class BlsSigningAcceptanceTest extends SigningAcceptanceTestBase {

  private static final Bytes DATA = Bytes.wrap("Hello, world!".getBytes(UTF_8));
  private static final String PRIVATE_KEY =
      "3ee2224386c82ffea477e2adf28a2929f5c349165a4196158c7f3a2ecca40f35";

  private static final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();
  private static final BLSSecretKey key =
      BLSSecretKey.fromBytes(Bytes32.fromHexString(PRIVATE_KEY));
  private static final BLSKeyPair keyPair = new BLSKeyPair(key);
  private static final BLSPublicKey publicKey = keyPair.getPublicKey();
  private static final BLSSignature expectedSignature = BLS.sign(keyPair.getSecretKey(), DATA);

  @Test
  public void signDataWithKeyLoadedFromUnencryptedFile() {
    final String configFilename = publicKey.toString().substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createUnencryptedYamlFileAt(keyConfigFile, PRIVATE_KEY, KeyType.BLS);

    signAndVerifySignature();
  }

  @ParameterizedTest
  @EnumSource(KdfFunction.class)
  public void signDataWithKeyLoadedFromKeyStoreFile(KdfFunction kdfFunction) {
    final String configFilename = publicKey.toString().substring(2);

    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createKeyStoreYamlFileAt(keyConfigFile, keyPair, kdfFunction);

    signAndVerifySignature();
  }

  @Test
  public void ableToSignUsingHashicorp() {
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

      signAndVerifySignature();
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
  public void ableToSignUsingAzure() {
    final String clientId = System.getenv("AZURE_CLIENT_ID");
    final String clientSecret = System.getenv("AZURE_CLIENT_SECRET");
    final String tenantId = System.getenv("AZURE_TENANT_ID");
    final String keyVaultName = System.getenv("AZURE_KEY_VAULT_NAME");
    final String secretName = "TEST-KEY";

    final String configFilename = keyPair.getPublicKey().toString().substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createAzureYamlFileAt(
        keyConfigFile, clientId, clientSecret, tenantId, keyVaultName, secretName);

    signAndVerifySignature();
  }

  @Test
  public void ableToSignUsingYubiHsm() throws IOException {
    final Path yubiShellSimulator = copyYubiHsmSimulator(testDirectory);

    final Path configFile = testDirectory.resolve("yubihsm_1.yaml");
    metadataFileHelpers.createYubiHsmYamlFileAt(configFile, yubiShellSimulator, KeyType.BLS);

    signAndVerifySignature();
  }

  private void signAndVerifySignature() {
    setupSigner("eth2");

    // openapi
    final Response response =
        signer.eth2Sign(keyPair.getPublicKey().toString(), DATA, AGGREGATION_SLOT);
    final Bytes signature = verifyAndGetSignatureResponse(response);
    assertThat(signature).isEqualTo(expectedSignature.toBytesCompressed());
  }
}
