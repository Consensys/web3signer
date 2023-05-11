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
package tech.pegasys.web3signer.tests;

import static tech.pegasys.web3signer.signing.KeyType.BLS;

import tech.pegasys.web3signer.dsl.signer.Signer;
import tech.pegasys.web3signer.dsl.signer.SignerConfiguration;
import tech.pegasys.web3signer.signing.KeyType;

import org.junit.jupiter.api.AfterEach;
import org.web3j.protocol.core.JsonRpc2_0Web3j;
import org.web3j.protocol.http.HttpService;

public class AcceptanceTestBase {

  protected Signer signer;
  protected JsonRpc2_0Web3j signerJsonRpc;

  public static final String JSON_RPC_PATH = "/rpc/v0";

  protected void startSigner(final SignerConfiguration config) {
    signer = new Signer(config, null);
    signer.start();
    signer.awaitStartupCompletion();
    signerJsonRpc = new JsonRpc2_0Web3j(new HttpService(signer.getUrl()));
  }

  @AfterEach
  protected void cleanup() {
    if (signer != null) {
      signer.shutdown();
      signer = null;
    }
  }

  public static String calculateMode(final KeyType keyType) {
    return (keyType == BLS) ? "eth2" : "eth1";
  }
}
