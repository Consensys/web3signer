/*
 * Copyright 2023 ConsenSys AG.
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
package tech.pegasys.web3signer.core.service.jsonrpc.handlers;

import tech.pegasys.web3signer.core.service.jsonrpc.JsonRpcRequest;
import tech.pegasys.web3signer.core.service.jsonrpc.exceptions.JsonRpcException;
import tech.pegasys.web3signer.core.service.jsonrpc.response.JsonRpcError;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Eth1AccountsHandler implements ResultProvider<List<String>> {
  private static final Logger LOG = LogManager.getLogger();
  private final Supplier<Set<String>> publicKeySupplier;

  public Eth1AccountsHandler(final Supplier<Set<String>> publicKeySupplier) {
    this.publicKeySupplier = publicKeySupplier;
  }

  @Override
  public List<String> createResponseResult(final JsonRpcRequest jsonRpcRequest) {

    final Object params = jsonRpcRequest.getParams();

    if (isPopulated(params) && isNotEmptyArray(params)) {
      LOG.info("eth_accounts should have no parameters, but has {}", params);
      throw new JsonRpcException(JsonRpcError.INVALID_PARAMS);
    }

    return publicKeySupplier.get().stream().sorted().collect(Collectors.toList());
  }

  private boolean isPopulated(final Object params) {
    return params != null;
  }

  private boolean isNotEmptyArray(final Object params) {
    boolean arrayIsEmpty = false;
    final boolean paramsIsArray = (params instanceof Collection);
    if (paramsIsArray) {
      final Collection<?> collection = (Collection<?>) params;
      arrayIsEmpty = collection.isEmpty();
    }

    return !(paramsIsArray && arrayIsEmpty);
  }
}
