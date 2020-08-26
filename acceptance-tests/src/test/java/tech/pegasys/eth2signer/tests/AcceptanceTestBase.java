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

import tech.pegasys.eth2signer.dsl.signer.Signer;
import tech.pegasys.eth2signer.dsl.signer.SignerConfiguration;

import com.atlassian.oai.validator.restassured.OpenApiValidationFilter;
import org.junit.jupiter.api.AfterEach;

public class AcceptanceTestBase {

  protected Signer signer;
  public static final String JSON_RPC_PATH = "/rpc/v1";
  public static final String FC_RPC_PATH = JSON_RPC_PATH + "/filecoin";

  protected void startSigner(final SignerConfiguration config) {
    signer = new Signer(config, null);
    signer.start();
    signer.awaitStartupCompletion();
  }

  protected OpenApiValidationFilter getOpenApiValidationFilter() {
    final String swaggerUrl = signer.getUrl() + "/swagger-ui/eth2signer.yaml";
    return new OpenApiValidationFilter(swaggerUrl);
  }

  @AfterEach
  protected void cleanup() {
    if (signer != null) {
      signer.shutdown();
      signer = null;
    }
  }
}
