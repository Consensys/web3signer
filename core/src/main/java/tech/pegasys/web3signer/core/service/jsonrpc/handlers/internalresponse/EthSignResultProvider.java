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
package tech.pegasys.web3signer.core.service.jsonrpc.handlers.internalresponse;

import static tech.pegasys.web3signer.core.service.jsonrpc.response.JsonRpcError.INVALID_PARAMS;
import static tech.pegasys.web3signer.core.service.jsonrpc.response.JsonRpcError.SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT;
import static tech.pegasys.web3signer.core.util.EthMessageUtil.getEthereumMessage;
import static tech.pegasys.web3signer.signing.KeyType.SECP256K1;

import tech.pegasys.web3signer.core.service.http.handlers.signing.SignerForIdentifier;
import tech.pegasys.web3signer.core.service.jsonrpc.JsonRpcRequest;
import tech.pegasys.web3signer.core.service.jsonrpc.exceptions.JsonRpcException;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.ResultProvider;
import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.signing.SecpArtifactSignature;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;

public class EthSignResultProvider implements ResultProvider<String> {

  private static final Logger LOG = LogManager.getLogger();

  private final ArtifactSignerProvider transactionSignerProvider;

  public EthSignResultProvider(final ArtifactSignerProvider transactionSignerProvider) {
    this.transactionSignerProvider = transactionSignerProvider;
  }

  @Override
  public String createResponseResult(final JsonRpcRequest request) {
    final List<String> params = getParams(request);
    if (params == null || params.size() != 2) {
      LOG.info(
          "eth_sign should have a list of 2 parameters, but has {}",
          params == null ? "null" : params.size());
      throw new JsonRpcException(INVALID_PARAMS);
    }

    final String address = params.get(0);
    final Optional<ArtifactSigner> transactionSigner = transactionSignerProvider.getSigner(address);
    if (transactionSigner.isEmpty()) {
      LOG.debug("Address ({}) does not match any available account", address);
      throw new JsonRpcException(SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT);
    }

    final Bytes ethMessage = getEthereumMessage(params.get(1));

    final SignerForIdentifier<SecpArtifactSignature> secpSigner =
        new SignerForIdentifier<>(transactionSignerProvider, this::formatSecpSignature, SECP256K1);

    return secpSigner.sign(address, ethMessage).get();
  }

  private List<String> getParams(final JsonRpcRequest request) {
    try {
      @SuppressWarnings("unchecked")
      final List<String> params = (List<String>) request.getParams();
      return params;
    } catch (final ClassCastException e) {
      LOG.debug(
          "eth_sign should have a list of 2 parameters, but received an object: {}",
          request.getParams());
      throw new JsonRpcException(INVALID_PARAMS);
    }
  }

  private String formatSecpSignature(final SecpArtifactSignature signature) {
    return SecpArtifactSignature.toBytes(signature).toHexString();
  }
}
