/*
 * Copyright 2019 ConsenSys AG.
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
package tech.pegasys.web3signer.core.jsonrpcproxy.model.request;

import java.util.Map.Entry;

public class EthNodeRequest {

  private final String body;
  private final Iterable<Entry<String, String>> headers;

  public EthNodeRequest(final Iterable<Entry<String, String>> headers, final String body) {
    this.body = body;
    this.headers = headers;
  }

  public String getBody() {
    return body;
  }

  public Iterable<Entry<String, String>> getHeaders() {
    return headers;
  }
}
