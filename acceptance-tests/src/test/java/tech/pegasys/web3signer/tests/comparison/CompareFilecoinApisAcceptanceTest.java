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
package tech.pegasys.web3signer.tests.comparison;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import tech.pegasys.web3signer.core.service.jsonrpc.FilecoinSignature;

import java.util.List;

import org.apache.tuweni.bytes.Bytes;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariables;

@EnabledIfEnvironmentVariables({
  @EnabledIfEnvironmentVariable(named = "LOTUS_PORT", matches = ".*")
})
public class CompareFilecoinApisAcceptanceTest extends CompareApisAcceptanceTestBase {

  @BeforeEach
  void initSigner() {
    super.initAndStartSigner(true);
  }

  @Test
  void compareWalletHasResponses() {
    ADDRESS_MAP.forEach(
        (address, key) -> {
          assertThat(LOTUS_NODE.walletHas(address)).isTrue();
          assertThat(signer.walletHas(address)).isTrue();
        });

    NON_EXISTENT_ADDRESS_MAP.forEach(
        (address, key) -> {
          assertThat(LOTUS_NODE.walletHas(address)).isFalse();
          assertThat(signer.walletHas(address)).isFalse();
        });
  }

  @Test
  void compareWalletListResponses() {
    final List<String> lotusWalletList = LOTUS_NODE.walletList();
    final List<String> signerWalletList = signer.walletList();

    // note: lotus node may have additional miner addresses which aren't loaded in Signer.
    Assertions.assertThat(lotusWalletList).containsAll(signerWalletList);
  }

  @RepeatedTest(25)
  void compareWalletSignAndVerifyResponsesWithRandomDataToSign() {

    ADDRESS_MAP.keySet().parallelStream()
        .forEach(
            address -> {
              final Bytes dataToSign = Bytes.random(32);

              assertThatCode(
                      () -> {
                        final FilecoinSignature lotusFcSig =
                            LOTUS_NODE.walletSign(address, dataToSign);
                        final FilecoinSignature signerFcSig =
                            signer.walletSign(address, dataToSign);

                        assertThat(signerFcSig).isEqualTo(lotusFcSig);

                        // verify signatures
                        final Boolean lotusSigVerify =
                            LOTUS_NODE.walletVerify(address, dataToSign, lotusFcSig);
                        final Boolean signerSigVerify =
                            signer.walletVerify(address, dataToSign, signerFcSig);

                        assertThat(lotusSigVerify).isTrue();
                        assertThat(signerSigVerify).isTrue();
                      })
                  .as("Running with data %s for address %s", dataToSign, address)
                  .doesNotThrowAnyException();
            });
  }
}
