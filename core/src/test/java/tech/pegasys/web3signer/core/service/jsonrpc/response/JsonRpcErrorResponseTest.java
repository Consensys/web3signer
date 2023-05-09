/*
 * Copyright 2023 ConsenSys AG.
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
package tech.pegasys.web3signer.core.service.jsonrpc.response;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

public class JsonRpcErrorResponseTest {

  @Test
  public void decodeErrorReportFromWeb3jProvider() {
    final JsonObject jsonObject =
        new JsonObject(
            "{\"jsonrpc\":\"2.0\",\"id\":77,\"error\":{\"code\":-32602,\"message\":\"Invalid params\"}}");
    final JsonRpcErrorResponse response = jsonObject.mapTo(JsonRpcErrorResponse.class);

    assertThat(response.getError().getCode()).isEqualTo(-32602);
  }
}
