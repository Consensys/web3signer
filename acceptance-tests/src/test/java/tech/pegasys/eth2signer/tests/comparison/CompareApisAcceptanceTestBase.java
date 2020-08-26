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
package tech.pegasys.eth2signer.tests.comparison;

import tech.pegasys.eth2signer.core.signing.KeyType;
import tech.pegasys.eth2signer.dsl.lotus.AddressesUtil;
import tech.pegasys.eth2signer.dsl.lotus.FilecoinKeyType;
import tech.pegasys.eth2signer.dsl.lotus.LotusNode;
import tech.pegasys.eth2signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.eth2signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.eth2signer.tests.AcceptanceTestBase;

import java.io.IOException;
import java.nio.file.Path;

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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;

public class CompareApisAcceptanceTestBase extends AcceptanceTestBase {
  protected static final LotusNode LOTUS_NODE =
      new LotusNode(Integer.parseInt(System.getenv("LOTUS_PORT")));
  protected static final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();

  @TempDir protected Path testDirectory;

  @BeforeAll
  static void setupWallet() {
    LOTUS_NODE.loadDefaultAddresses();
  }

  protected void initAndStartSigner(boolean initKeystoreDirectory) {
    if (initKeystoreDirectory) {
      initSignerKeystoreDirectory();
    }

    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withKeyStoreDirectory(testDirectory);
    startSigner(builder.build());
  }

  protected void initSignerKeystoreDirectory() {
    AddressesUtil.getDefaultFilecoinAddressMap()
        .forEach(
            (fcAddress, key) ->
                metadataFileHelpers.createUnencryptedYamlFileAt(
                    keyConfigFile(key.getPublicKey()),
                    key.getPrivateKeyHex(),
                    key.getType() == FilecoinKeyType.BLS ? KeyType.BLS : KeyType.SECP256K1));
  }

  protected String executeRawJsonRpcRequest(final String url, final String request)
      throws IOException {
    final HttpPost post = new HttpPost(url);
    post.setEntity(new StringEntity(request, Charsets.UTF_8));
    post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString());
    try (final CloseableHttpClient httpClient = HttpClients.createDefault();
        final CloseableHttpResponse httpResponse = httpClient.execute(post)) {
      return EntityUtils.toString(httpResponse.getEntity(), Charsets.UTF_8);
    }
  }

  protected JsonRpcClient getSignerJsonRpcClient() {
    return new JsonRpcClient(
        request -> executeRawJsonRpcRequest(signer.getUrl() + FC_RPC_PATH, request));
  }

  protected Boolean signerHasAddress(final String address) {
    return getSignerJsonRpcClient()
        .createRequest()
        .method("Filecoin.WalletHas")
        .params(address)
        .id(101)
        .returnAs(Boolean.class)
        .execute();
  }

  private Path keyConfigFile(final String publicKey) {
    return testDirectory.resolve(stripOxPrefix(publicKey) + ".yaml");
  }

  private String stripOxPrefix(final String publicKey) {
    return publicKey.startsWith("0x") || publicKey.startsWith("0X")
        ? publicKey.substring(2)
        : publicKey;
  }
}
