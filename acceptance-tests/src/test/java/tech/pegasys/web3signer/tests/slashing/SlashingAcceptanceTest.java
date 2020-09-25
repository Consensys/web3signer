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
package tech.pegasys.web3signer.tests.slashing;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;
import java.nio.file.Path;
import java.security.SecureRandom;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.web3signer.core.service.http.ArtifactType;
import tech.pegasys.web3signer.core.service.http.Eth2SigningRequestBody;
import tech.pegasys.web3signer.core.service.http.SigningJsonRpcModule;
import tech.pegasys.web3signer.core.signing.KeyType;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.web3signer.tests.AcceptanceTestBase;

public class SlashingAcceptanceTest extends AcceptanceTestBase {

  private static final String SLASHING_DB_URL = System.getenv("SLASHING_DB_URL");
  private static final String SLASHING_DB_USERNAME = System.getenv("SLASHING_DB_USERNAME");
  private static final String SLASHING_DB_PASSWORD = System.getenv("SLASHING_DB_PASSWORD");

  private static final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();

  private static final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new SigningJsonRpcModule());


  @BeforeAll
  static void setup() {
    Assumptions.assumeTrue(SLASHING_DB_URL != null, "Set SLASHING_DB_URL environment variable");
    Assumptions.assumeTrue(
        SLASHING_DB_USERNAME != null, "Set SLASHING_DB_USERNAME environment variable");
    Assumptions.assumeTrue(
        SLASHING_DB_PASSWORD != null, "Set SLASHING_DB_PASSWORD environment variable");
  }

  @Test
  void cannotSignSameAttestationTwiceWhenSlashingIsEnabled(@TempDir Path testDirectory)
      throws JsonProcessingException {
    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withMode("eth2");
    builder.withSlashingProtectionDbUrl(SLASHING_DB_URL);
    builder.withSlashingProtectionDbUsername(SLASHING_DB_USERNAME);
    builder.withSlashingProtectionDbPassword(SLASHING_DB_PASSWORD);
    builder.withKeyStoreDirectory(testDirectory);

    final BLSKeyPair keyPair = BLSKeyPair.random(new SecureRandom());
    final Path keyConfigFile = testDirectory.resolve("keyfile.yaml");
    metadataFileHelpers.createUnencryptedYamlFileAt(
        keyConfigFile, keyPair.getSecretKey().toBytes().toHexString(), KeyType.BLS);

    startSigner(builder.build());

    final Bytes signingRoot = Bytes.fromHexString("0x01");
    final Eth2SigningRequestBody request =
        new Eth2SigningRequestBody(
            signingRoot,
            ArtifactType.ATTESTATION,
            UInt64.valueOf(1L),
            UInt64.valueOf(5L),
            UInt64.valueOf(6L));

    final String requestBody = objectMapper.writeValueAsString(request);

    final Response initialResponse =
        signer.sign(keyPair.getPublicKey().toBytesCompressed().toHexString(), requestBody);
    assertThat(initialResponse.getStatusCode()).isEqualTo(200);
    final Response secondResponse =
        signer.sign(keyPair.getPublicKey().toBytesCompressed().toHexString(), requestBody);
    assertThat(secondResponse.getStatusCode()).isEqualTo(400);
  }
}
