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

import tech.pegasys.eth2signer.core.service.jsonrpc.FilecoinMessage;
import tech.pegasys.eth2signer.core.service.jsonrpc.FilecoinSignature;
import tech.pegasys.eth2signer.core.service.jsonrpc.FilecoinSignedMessage;

import java.io.IOException;
import java.util.List;

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
import org.apache.tuweni.bytes.Bytes;

public class FilecoinJsonRequests {
  public static final int BLS_SIGTYPE = 1;
  public static final int SECP_SIGTYPE = 2;

  public static String walletNew(final JsonRpcClient jsonRpcClient, int sigType) {
    return jsonRpcClient
        .createRequest()
        .method("Filecoin.WalletNew")
        .params(sigType)
        .id(101)
        .returnAs(String.class)
        .execute();
  }

  public static FilecoinKey walletExport(final JsonRpcClient jsonRpcClient, final String address) {
    return jsonRpcClient
        .createRequest()
        .method("Filecoin.WalletExport")
        .params(address)
        .id(101)
        .returnAs(FilecoinKey.class)
        .execute();
  }

  public static Boolean walletHas(final JsonRpcClient jsonRpcClient, final String address) {
    return jsonRpcClient
        .createRequest()
        .method("Filecoin.WalletHas")
        .params(address)
        .id(101)
        .returnAs(Boolean.class)
        .execute();
  }

  public static List<String> walletList(final JsonRpcClient jsonRpcClient) {
    return jsonRpcClient
        .createRequest()
        .method("Filecoin.WalletList")
        .id(101)
        .returnAsList(String.class)
        .execute();
  }

  public static FilecoinSignature walletSign(
      final JsonRpcClient jsonRpcClient, final String address, final Bytes data) {
    return jsonRpcClient
        .createRequest()
        .method("Filecoin.WalletSign")
        .id(101)
        .params(address, data)
        .returnAs(FilecoinSignature.class)
        .execute();
  }

  public static Boolean walletVerify(
      final JsonRpcClient jsonRpcClient,
      final String address,
      final Bytes data,
      final FilecoinSignature signature) {
    return jsonRpcClient
        .createRequest()
        .method("Filecoin.WalletVerify")
        .id(202)
        .params(address, data, signature)
        .returnAs(Boolean.class)
        .execute();
  }

  public static FilecoinSignedMessage walletSignMessage(
      final JsonRpcClient jsonRpcClient, final String address, final FilecoinMessage message) {
    return jsonRpcClient
        .createRequest()
        .method("Filecoin.WalletSignMessage")
        .id(101)
        .params(address, message)
        .returnAs(FilecoinSignedMessage.class)
        .execute();
  }

  public static String executeRawJsonRpcRequest(final String url, final String request)
      throws IOException {
    final HttpPost post = new HttpPost(url);
    post.setEntity(new StringEntity(request, Charsets.UTF_8));
    post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString());
    final String authToken = System.getenv("ETH2SIGNER_BEARER_TOKEN");
    if (authToken != null) {
      post.setHeader("Authorization", "Bearer " + authToken);
    }
    try (final CloseableHttpClient httpClient = HttpClients.createDefault();
        final CloseableHttpResponse httpResponse = httpClient.execute(post)) {
      return EntityUtils.toString(httpResponse.getEntity(), Charsets.UTF_8);
    }
  }
}
