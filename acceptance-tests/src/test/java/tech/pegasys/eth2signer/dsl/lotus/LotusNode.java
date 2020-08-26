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
package tech.pegasys.eth2signer.dsl.lotus;

import java.io.IOException;

import com.github.arteam.simplejsonrpc.client.JsonRpcClient;
import com.google.common.net.MediaType;
import org.apache.commons.codec.Charsets;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * Lotus is expected to be running outside of acceptance test. It can be executed using following
 * docker command <code>
 *     docker run --rm --name lotuslocalnet -e TEXLOTUSDEVNET_SPEED=1500 \ -e
 * TEXLOTUSDEVNET_BIGSECTORS=false -p 1234:7777 \ -v /tmp/import:/tmp/import textile/lotus-devnet
 * </code>
 */
public class LotusNode {
  private static final String FC_URL_FORMAT = "http://%s:%d/rpc/v0";
  private final String fcUrl;
  private final JsonRpcClient jsonRpcClient;

  public LotusNode(final String host, final int port) {
    fcUrl = String.format(FC_URL_FORMAT, host, port);
    jsonRpcClient = initClient();
  }

  public LotusNode(final int port) {
    this("127.0.0.1", port);
  }

  public void loadDefaultAddresses() {
    AddressesUtil.getDefaultFilecoinAddressMap()
        .forEach(
            (address, key) -> {
              if (!hasAddress(address)) {
                importPrivateKey(key);
              }
            });
  }

  private void importPrivateKey(final FilecoinKey key) {
    jsonRpcClient
        .createRequest()
        .method("Filecoin.WalletImport")
        .params(key)
        .id(202)
        .returnAs(String.class)
        .execute();
  }

  public Boolean hasAddress(final String address) {
    return jsonRpcClient
        .createRequest()
        .method("Filecoin.WalletHas")
        .params(address)
        .id(101)
        .returnAs(Boolean.class)
        .execute();
  }

  public String executeRawJsonRpcRequest(final String request) throws IOException {
    final HttpPost post = new HttpPost(fcUrl);
    post.setEntity(new StringEntity(request, Charsets.UTF_8));
    post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString());
    try (final CloseableHttpClient httpClient = HttpClients.createDefault();
        final CloseableHttpResponse httpResponse = httpClient.execute(post)) {
      return EntityUtils.toString(httpResponse.getEntity(), Charsets.UTF_8);
    }
  }

  private JsonRpcClient initClient() {
    return new JsonRpcClient(this::executeRawJsonRpcRequest);
  }
}
