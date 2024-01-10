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
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.SpecFactory;
import tech.pegasys.teku.spec.datastructures.util.ForkAndSpecMilestone;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.Eth2SigningRequestBody;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.utils.Eth2BlockSigningRequestUtil;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.web3signer.signing.KeyType;

import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.io.Resources;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class Eth2CustomNetworkFileAcceptanceTest extends SigningAcceptanceTestBase {
  private static final Path NETWORK_CONFIG_PATH =
      Path.of(Resources.getResource("eth2/network_config.yaml").getPath());
  private static final String PRIVATE_KEY =
      "3ee2224386c82ffea477e2adf28a2929f5c349165a4196158c7f3a2ecca40f35";

  private static final MetadataFileHelpers METADATA_FILE_HELPERS = new MetadataFileHelpers();
  private static final BLSSecretKey KEY =
      BLSSecretKey.fromBytes(Bytes32.fromHexString(PRIVATE_KEY));
  private static final BLSKeyPair KEY_PAIR = new BLSKeyPair(KEY);
  private static final BLSPublicKey PUBLIC_KEY = KEY_PAIR.getPublicKey();

  @BeforeEach
  void setup() {
    final String configFilename = PUBLIC_KEY.toString().substring(2);
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    METADATA_FILE_HELPERS.createUnencryptedYamlFileAt(keyConfigFile, PRIVATE_KEY, KeyType.BLS);

    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withKeyStoreDirectory(testDirectory).withMode("eth2").withNetwork(NETWORK_CONFIG_PATH);
    startSigner(builder.build());
  }

  @Test
  void signAndVerifyBlockV2SignatureForAllEnabledMilestones() throws Exception {
    final Spec spec = SpecFactory.create(NETWORK_CONFIG_PATH.toString());
    final List<ForkAndSpecMilestone> enabledMilestones = spec.getEnabledMilestones();
    assertThat(enabledMilestones.size()).isEqualTo(5);

    for (final ForkAndSpecMilestone forkAndSpecMilestone : enabledMilestones) {
      final Eth2SigningRequestBody request =
          createBlockV2SigningRequest(spec, forkAndSpecMilestone);
      final Bytes signingRootSignature = sendSignRequestAndReceiveSignature(request);
      final Bytes expectedSigningRootSignature =
          calculateSigningRootSignature(request.signingRoot());

      assertThat(signingRootSignature).isEqualTo(expectedSigningRootSignature);
    }
  }

  private Eth2SigningRequestBody createBlockV2SigningRequest(
      final Spec spec, final ForkAndSpecMilestone forkAndMilestone) {
    final UInt64 forkEpoch = forkAndMilestone.getFork().getEpoch();
    final UInt64 startSlot = spec.computeStartSlotAtEpoch(forkEpoch);

    final Eth2BlockSigningRequestUtil util =
        new Eth2BlockSigningRequestUtil(spec, forkEpoch, startSlot);
    return util.createBlockV2Request();
  }

  private Bytes sendSignRequestAndReceiveSignature(final Eth2SigningRequestBody request)
      throws JsonProcessingException {
    final Response response =
        signer.eth2Sign(KEY_PAIR.getPublicKey().toString(), request, ContentType.JSON);
    return verifyAndGetSignatureResponse(response, ContentType.JSON);
  }

  private Bytes calculateSigningRootSignature(final Bytes signingRoot) {
    return BLS.sign(KEY_PAIR.getSecretKey(), signingRoot).toBytesCompressed();
  }
}
