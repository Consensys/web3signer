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

import tech.pegasys.eth2signer.core.service.jsonrpc.FilecoinJsonRpcModule;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
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
  public static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().registerModule(new FilecoinJsonRpcModule());

  public LotusNode(final String host, final int port) {
    fcUrl = String.format(FC_URL_FORMAT, host, port);
    jsonRpcClient =
        new JsonRpcClient(
            request -> FilecoinJsonRequests.executeRawJsonRpcRequest(fcUrl, request),
            OBJECT_MAPPER);
  }

  public LotusNode(final int port) {
    this("127.0.0.1", port);
  }

  public Map<String, FilecoinKey> createKeys(final int blsKeysCount, final int secpKeysCount) {
    final Set<String> addresses = new HashSet<>();

    for (int i = 0; i < blsKeysCount; i++) {
      final String address = FilecoinJsonRequests.walletNew(jsonRpcClient, BLS_SIGTYPE);
      addresses.add(address);
    }

    for (int i = 0; i < secpKeysCount; i++) {
      final String address = FilecoinJsonRequests.walletNew(jsonRpcClient, SECP_SIGTYPE);
      addresses.add(address);
    }

    return addresses
        .parallelStream()
        .collect(
            Collectors.toMap(
                Function.identity(),
                address -> FilecoinJsonRequests.walletExport(jsonRpcClient, address)));
  }

  public JsonRpcClient getJsonRpcClient() {
    return jsonRpcClient;
  }
}
