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
package tech.pegasys.web3signer.dsl.lotus;

import tech.pegasys.web3signer.core.service.jsonrpc.FilecoinJsonRpcModule;
import tech.pegasys.web3signer.core.service.jsonrpc.FilecoinSignature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.arteam.simplejsonrpc.client.JsonRpcClient;
import com.google.common.net.MediaType;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.tuweni.bytes.Bytes;

public abstract class FilecoinJsonRpcEndpoint {

  public static final String BLS_SIGTYPE = "bls";
  public static final String SECP_SIGTYPE = "secp256k1";

  // This is required to be set if operating against a full Lotus node (as opposed to dev-lotus).
  private static final Optional<String> AUTH_TOKEN =
      Optional.ofNullable(System.getenv("WEB3SIGNER_BEARER_TOKEN"));

  private static final ObjectMapper OBJECT_MAPPER =
      JsonMapper.builder().addModule(new FilecoinJsonRpcModule()).build();

  private final JsonRpcClient jsonRpcClient;
  private final String rpcPath;

  protected FilecoinJsonRpcEndpoint(final String rpcPath) {
    jsonRpcClient = new JsonRpcClient(this::executeRawJsonRpcRequest, OBJECT_MAPPER);
    this.rpcPath = rpcPath;
  }

  public String walletNew(final String sigType) {
    return jsonRpcClient
        .createRequest()
        .method("Filecoin.WalletNew")
        .params(sigType)
        .id(101)
        .returnAs(String.class)
        .execute();
  }

  public FilecoinKey walletExport(final String address) {
    return jsonRpcClient
        .createRequest()
        .method("Filecoin.WalletExport")
        .params(address)
        .id(101)
        .returnAs(FilecoinKey.class)
        .execute();
  }

  public Boolean walletHas(final String address) {
    return jsonRpcClient
        .createRequest()
        .method("Filecoin.WalletHas")
        .params(address)
        .id(101)
        .returnAs(Boolean.class)
        .execute();
  }

  public List<String> walletList() {
    return jsonRpcClient
        .createRequest()
        .method("Filecoin.WalletList")
        .id(101)
        .returnAsList(String.class)
        .execute();
  }

  public FilecoinSignature walletSign(final String address, final Bytes data) {
    return jsonRpcClient
        .createRequest()
        .method("Filecoin.WalletSign")
        .id(101)
        .params(address, data) // metaData is not supported on FC local wallet signing API impl
        .returnAs(FilecoinSignature.class)
        .execute();
  }

  public Boolean walletVerify(
      final String address, final Bytes data, final FilecoinSignature signature) {
    return jsonRpcClient
        .createRequest()
        .method("Filecoin.WalletVerify")
        .id(202)
        .params(address, data, signature)
        .returnAs(Boolean.class)
        .execute();
  }

  public String executeRawJsonRpcRequest(final String request) throws IOException {
    final String url = getUrl() + rpcPath;
    final HttpPost post = new HttpPost(url);
    post.setEntity(new StringEntity(request, StandardCharsets.UTF_8));
    post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString());
    AUTH_TOKEN.ifPresent(token -> post.setHeader("Authorization", "Bearer " + token));
    try (final CloseableHttpClient httpClient = HttpClients.createDefault();
        final CloseableHttpResponse httpResponse = httpClient.execute(post)) {
      return EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
    }
  }

  public abstract String getUrl();
}
