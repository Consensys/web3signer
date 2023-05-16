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
package tech.pegasys.web3signer.core.service.jsonrpc.handlers.internalresponse;

import tech.pegasys.web3signer.core.Eth1AddressSignerIdentifier;
import tech.pegasys.web3signer.core.service.jsonrpc.JsonRpcRequest;
import tech.pegasys.web3signer.core.service.jsonrpc.exceptions.JsonRpcException;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.ResultProvider;
import tech.pegasys.web3signer.core.service.jsonrpc.response.JsonRpcError;

import java.security.interfaces.ECPublicKey;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EthAccountsResultProvider implements ResultProvider<List<String>> {

  private static final Logger LOG = LogManager.getLogger();

  private final Supplier<Set<ECPublicKey>> publicKeySupplier;

  public EthAccountsResultProvider(final Supplier<Set<ECPublicKey>> publicKeySupplier) {
    this.publicKeySupplier = publicKeySupplier;
  }

  @Override
  public List<String> createResponseResult(final JsonRpcRequest request) {
    final Object params = request.getParams();

    if (isPopulated(params) && isNotEmptyArray(params)) {
      LOG.info("eth_accounts should have no parameters, but has {}", request.getParams());
      throw new JsonRpcException(JsonRpcError.INVALID_PARAMS);
    }

    return publicKeySupplier.get().stream()
        .map(Eth1AddressSignerIdentifier::fromPublicKey)
        .map(signerIdentifier -> "0x" + signerIdentifier.toStringIdentifier())
        .sorted()
        .collect(Collectors.toList());
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
