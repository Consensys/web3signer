/*
 * Copyright 2021 ConsenSys AG.
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

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.teku.bls.BLS;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.teku.bls.BLSSignature;
import tech.pegasys.teku.spec.SpecMilestone;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.Eth2SigningRequestBody;
import tech.pegasys.web3signer.core.signing.KeyType;
import tech.pegasys.web3signer.dsl.utils.Eth2AltairBlockRequestUtil;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;

import java.nio.file.Path;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class Eth2AltairBlockSigningAcceptanceTest extends SigningAcceptanceTestBase {
  private static final String PRIVATE_KEY =
      "3ee2224386c82ffea477e2adf28a2929f5c349165a4196158c7f3a2ecca40f35";

  private static final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();
  private static final BLSSecretKey key =
      BLSSecretKey.fromBytes(Bytes32.fromHexString(PRIVATE_KEY));
  private static final BLSKeyPair keyPair = new BLSKeyPair(key);
  private static final BLSPublicKey publicKey = keyPair.getPublicKey();

  @BeforeEach
  void setup() {
    final String configFilename = publicKey.toString().substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createUnencryptedYamlFileAt(keyConfigFile, PRIVATE_KEY, KeyType.BLS);
  }

  @ParameterizedTest
  @EnumSource
  void signAndVerifyAltairBlockSignature(final SpecMilestone specMilestone) throws Exception {
    final Eth2AltairBlockRequestUtil util = new Eth2AltairBlockRequestUtil(specMilestone, "minimal");

    setupSigner("eth2", null, util.getNetwork());

    final Eth2SigningRequestBody request = util.createRandomBlockV2Request();
    final Response response =
        signer.eth2Sign(keyPair.getPublicKey().toString(), request, ContentType.JSON);
    final Bytes signature = verifyAndGetSignatureResponse(response, ContentType.JSON);
    final BLSSignature expectedSignature =
        BLS.sign(keyPair.getSecretKey(), request.getSigningRoot());
    assertThat(signature).isEqualTo(expectedSignature.toBytesCompressed());
  }

  @Test
  void signAndVerifyPhase0BlockSignature() throws Exception {
    final Eth2AltairBlockRequestUtil util = new Eth2AltairBlockRequestUtil(SpecMilestone.PHASE0, "mainnet");
    setupSigner("eth2", null, util.getNetwork());

    final Eth2SigningRequestBody request = util.createRandomPhase0BlockRequest();
    final Response response =
        signer.eth2Sign(keyPair.getPublicKey().toString(), request, ContentType.JSON);
    final Bytes signature = verifyAndGetSignatureResponse(response, ContentType.JSON);
    final BLSSignature expectedSignature =
        BLS.sign(keyPair.getSecretKey(), request.getSigningRoot());
    assertThat(signature).isEqualTo(expectedSignature.toBytesCompressed());
  }
}
