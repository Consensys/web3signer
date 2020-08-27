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

import static tech.pegasys.eth2signer.dsl.lotus.FilecoinJsonRequests.BLS_SIGTYPE;
import static tech.pegasys.eth2signer.dsl.lotus.FilecoinJsonRequests.SECP_SIGTYPE;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.github.arteam.simplejsonrpc.client.JsonRpcClient;

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
    jsonRpcClient =
        new JsonRpcClient(request -> FilecoinJsonRequests.executeRawJsonRpcRequest(fcUrl, request));
  }

  public LotusNode(final int port) {
    this("127.0.0.1", port);
  }

  public Map<String, FilecoinKey> loadAddresses(final int blsKeys, final int secpKeys) {
    final Set<String> addresses = new HashSet<>();
    final Map<String, FilecoinKey> addressKeys = new HashMap<>();

    for (int i = 0; i < blsKeys; i++) {
      final String address = FilecoinJsonRequests.walletNew(jsonRpcClient, BLS_SIGTYPE);
      addresses.add(address);
    }

    for (int i = 0; i < secpKeys; i++) {
      final String address = FilecoinJsonRequests.walletNew(jsonRpcClient, SECP_SIGTYPE);
      addresses.add(address);
    }

    addresses.forEach(
        address -> {
          final FilecoinKey filecoinKey = FilecoinJsonRequests.walletExport(jsonRpcClient, address);
          addressKeys.put(address, filecoinKey);
        });

    return Map.copyOf(addressKeys);
  }

  public JsonRpcClient getJsonRpcClient() {
    return jsonRpcClient;
  }
}
