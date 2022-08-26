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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lotus is expected to be running outside of acceptance test. It can be executed using following
 * docker command <code>
 *     docker run --rm --name lotuslocalnet -e TEXLOTUSDEVNET_SPEED=1500 \ -e
 * TEXLOTUSDEVNET_BIGSECTORS=false -p 1234:7777 \ -v /tmp/import:/tmp/import textile/lotus-devnet
 * </code>
 */
public class LotusNode extends FilecoinJsonRpcEndpoint {

  private final String fcUrl;

  public LotusNode(final String host, final int port) {
    super("/rpc/v0");
    fcUrl = String.format("http://%s:%d", host, port);
  }

  public LotusNode(final int port) {
    this("127.0.0.1", port);
  }

  public Map<String, FilecoinKey> createKeys(final int blsKeysCount, final int secpKeysCount) {
    final Set<String> addresses = new HashSet<>();

    for (int i = 0; i < blsKeysCount; i++) {
      final String address = walletNew(BLS_SIGTYPE);
      addresses.add(address);
    }

    for (int i = 0; i < secpKeysCount; i++) {
      final String address = walletNew(SECP_SIGTYPE);
      addresses.add(address);
    }

    return addresses.parallelStream()
        .collect(Collectors.toMap(Function.identity(), this::walletExport));
  }

  @Override
  public String getUrl() {
    return fcUrl;
  }
}
