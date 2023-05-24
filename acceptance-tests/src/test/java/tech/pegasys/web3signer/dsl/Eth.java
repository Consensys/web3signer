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
package tech.pegasys.web3signer.dsl;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;

public class Eth {

  private final Web3j jsonRpc;

  public Eth(final Web3j jsonRpc) {
    this.jsonRpc = jsonRpc;
  }

  public List<String> getAccounts() throws IOException {
    return jsonRpc.ethAccounts().send().getAccounts();
  }

  public BigInteger getBalance(final String account) throws IOException {
    return jsonRpc.ethGetBalance(account, DefaultBlockParameterName.LATEST).send().getBalance();
  }
}
