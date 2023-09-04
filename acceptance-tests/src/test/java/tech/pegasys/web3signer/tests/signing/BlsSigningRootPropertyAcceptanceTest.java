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
import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.teku.bls.BLS;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.teku.bls.BLSSignature;
import tech.pegasys.teku.spec.SpecMilestone;
import tech.pegasys.teku.spec.networks.Eth2Network;
import tech.pegasys.web3signer.core.service.http.ArtifactType;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.Eth2SigningRequestBody;
import tech.pegasys.web3signer.dsl.signer.Signer;
import tech.pegasys.web3signer.dsl.utils.Eth2RequestUtils;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.web3signer.signing.KeyType;

import java.nio.file.Path;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class BlsSigningRootPropertyAcceptanceTest extends SigningAcceptanceTestBase {

  private static final String PRIVATE_KEY =
      "3ee2224386c82ffea477e2adf28a2929f5c349165a4196158c7f3a2ecca40f35";
  private static final MetadataFileHelpers METADATA_FILE_HELPERS = new MetadataFileHelpers();
  private static final BLSSecretKey KEY =
      BLSSecretKey.fromBytes(Bytes32.fromHexString(PRIVATE_KEY));
  private static final BLSKeyPair KEY_PAIR = new BLSKeyPair(KEY);
  private static final BLSPublicKey PUBLIC_KEY = KEY_PAIR.getPublicKey();

  @ParameterizedTest(name = "#{index} - Signing request with signing root property as: {0}")
  @ValueSource(strings = {"signing_root", "signingRoot"})
  public void deprecatedSigningRootPropertyWorks(final String signingRootProperty)
      throws JsonProcessingException {
    final String configFilename = PUBLIC_KEY.toString().substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    METADATA_FILE_HELPERS.createUnencryptedYamlFileAt(keyConfigFile, PRIVATE_KEY, KeyType.BLS);

    signAndVerifySignature(signingRootProperty);
  }

  private void signAndVerifySignature(final String signingRootProperty)
      throws JsonProcessingException {
    final ArtifactType artifactType = ArtifactType.RANDAO_REVEAL;
    final ContentType acceptMediaType = JSON;
    setupMinimalWeb3Signer(artifactType);

    // openapi
    final Eth2SigningRequestBody request = Eth2RequestUtils.createCannedRequest(artifactType);
    final String jsonBody = Signer.ETH_2_INTERFACE_OBJECT_MAPPER.writeValueAsString(request);

    // by default, we should have deserialized to signing_root.
    assertThat(jsonBody).containsOnlyOnce("signing_root").doesNotContain("signingRoot");
    // modify before sending
    final String modifiedJsonBody = jsonBody.replace("signing_root", signingRootProperty);
    assertThat(modifiedJsonBody).containsOnlyOnce(signingRootProperty);

    // send modified JSON containing signing_root or signingRoot
    final Response response =
        signer.eth2Sign(KEY_PAIR.getPublicKey().toString(), modifiedJsonBody, acceptMediaType);
    final Bytes signature =
        verifyAndGetSignatureResponse(response, expectedContentType(acceptMediaType));
    final BLSSignature expectedSignature = BLS.sign(KEY_PAIR.getSecretKey(), request.signingRoot());
    assertThat(signature).isEqualTo(expectedSignature.toBytesCompressed());
  }

  private void setupMinimalWeb3Signer(final ArtifactType artifactType) {
    switch (artifactType) {
      case BLOCK_V2, SYNC_COMMITTEE_MESSAGE, SYNC_COMMITTEE_SELECTION_PROOF, SYNC_COMMITTEE_CONTRIBUTION_AND_PROOF -> setupEth2Signer(
          Eth2Network.MINIMAL, SpecMilestone.ALTAIR);
      default -> setupEth2Signer(Eth2Network.MINIMAL, SpecMilestone.PHASE0);
    }
  }

  private ContentType expectedContentType(final ContentType acceptMediaType) {
    return acceptMediaType == ANY || acceptMediaType == JSON ? JSON : TEXT;
  }
}
