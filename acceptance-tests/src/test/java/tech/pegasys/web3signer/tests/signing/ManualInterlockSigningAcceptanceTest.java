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

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.teku.bls.BLS;
import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.teku.bls.BLSSignature;
import tech.pegasys.web3signer.core.service.http.ArtifactType;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.Eth2SigningRequestBody;
import tech.pegasys.web3signer.core.signing.KeyType;
import tech.pegasys.web3signer.dsl.utils.Eth2RequestUtils;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;

import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.response.Response;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Requires access to Interlock on USB Armory II")
public class ManualInterlockSigningAcceptanceTest extends SigningAcceptanceTestBase {
  private static final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();

  // following keys are expected at /bls/key1.txt...key11.txt in USB Armory
  private final List<BLSSecretKey> blsSecretKeys =
      List.of(
          BLSSecretKey.fromBytes(
              Bytes32.fromHexString(
                  "000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f")),
          BLSSecretKey.fromBytes(
              Bytes32.fromHexString(
                  "73d51abbd89cb8196f0efb6892f94d68fccc2c35f0b84609e5f12c55dd85aba8")),
          BLSSecretKey.fromBytes(
              Bytes32.fromHexString(
                  "39722cbbf8b91a4b9045c5e6175f1001eac32f7fcd5eccda5c6e62fc4e638508")),
          BLSSecretKey.fromBytes(
              Bytes32.fromHexString(
                  "4c9326bb9805fa8f85882c12eae724cef0c62e118427f5948aefa5c428c43c93")),
          BLSSecretKey.fromBytes(
              Bytes32.fromHexString(
                  "384a62688ee1d9a01c9d58e303f2b3c9bc1885e8131565386f75f7ae6ca8d147")),
          BLSSecretKey.fromBytes(
              Bytes32.fromHexString(
                  "4b6b5c682f2db7e510e0c00ed67ac896c21b847acadd8df29cf63a77470989d2")),
          BLSSecretKey.fromBytes(
              Bytes32.fromHexString(
                  "13086d684f4b1a1632178a8c5be08a2fb01287c4a78313c41373701eb8e66232")),
          BLSSecretKey.fromBytes(
              Bytes32.fromHexString(
                  "25296867ee96fa5b275af1b72f699efcb61586565d4c3c7e41f4b3e692471abd")),
          BLSSecretKey.fromBytes(
              Bytes32.fromHexString(
                  "10e1a313e573d96abe701d8848742cf88166dd2ded38ac22267a05d1d62baf71")),
          BLSSecretKey.fromBytes(
              Bytes32.fromHexString(
                  "0bdeebbad8f9b240192635c42f40f2d02ee524c5a3fe8cda53fb4897b08c66fe")),
          BLSSecretKey.fromBytes(
              Bytes32.fromHexString(
                  "5e8d5667ce78982a07242739ab03dc63c91e830c80a5b6adca777e3f216a405d")));

  @BeforeEach
  void init() {
    final Path knownServersFile = testDirectory.resolve("interlockKnownServer.txt");
    for (int i = 1; i <= 11; i++) {
      final Path configFile = testDirectory.resolve("interlock" + i + ".yaml");
      metadataFileHelpers.createInterlockYamlFileAt(
          configFile, knownServersFile, Path.of("/bls/key" + i + ".txt"), KeyType.BLS);
    }

    // secp key
    final Path configFile = testDirectory.resolve("interlock_secp.yaml");
    metadataFileHelpers.createInterlockYamlFileAt(
        configFile, knownServersFile, Path.of("/secp/key1.txt"), KeyType.SECP256K1);

    setupSigner("eth2", null);
  }

  @Test
  public void blsSigningFromKeysStoredInInterlock() {

    final Eth2SigningRequestBody request = Eth2RequestUtils.createCannedRequest(ArtifactType.BLOCK);

    blsSecretKeys.forEach(
        blsSecretKey -> {
          final Response response;
          try {
            response = signer.eth2Sign(blsSecretKey.toPublicKey().toString(), request);
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }

          final Bytes signature = verifyAndGetSignatureResponse(response);
          final BLSSignature expectedSignature = BLS.sign(blsSecretKey, request.getSigningRoot());

          assertThat(signature).isEqualTo(expectedSignature.toBytesCompressed());
        });
  }
}
