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
package tech.pegasys.web3signer.signing.filecoin;

import tech.pegasys.web3signer.signing.filecoin.exceptions.InvalidFilecoinNetworkException;

import java.util.Arrays;

public enum FilecoinNetwork {
  MAINNET("f"),
  TESTNET("t");

  private final String networkValue;

  FilecoinNetwork(final String addrValue) {
    this.networkValue = addrValue;
  }

  public String getNetworkValue() {
    return networkValue;
  }

  public static FilecoinNetwork findByNetworkValue(final String networkValue) {
    return Arrays.stream(values())
        .filter(p -> p.networkValue.equals(networkValue))
        .findFirst()
        .orElseThrow(InvalidFilecoinNetworkException::new);
  }
}
