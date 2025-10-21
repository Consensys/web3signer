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
package tech.pegasys.web3signer.core.service.jsonrpc.handlers.internalresponse;

import static tech.pegasys.web3signer.core.service.jsonrpc.response.JsonRpcError.INVALID_PARAMS;
import static tech.pegasys.web3signer.core.service.jsonrpc.response.JsonRpcError.SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT;
import static tech.pegasys.web3signer.signing.util.IdentifierUtils.normaliseIdentifier;

import tech.pegasys.web3signer.core.service.http.handlers.signing.SignerForIdentifier;
import tech.pegasys.web3signer.core.service.jsonrpc.JsonRpcRequest;
import tech.pegasys.web3signer.core.service.jsonrpc.exceptions.JsonRpcException;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.ResultProvider;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.web3j.crypto.StructuredDataEncoder;

public class EthSignTypedDataResultProvider implements ResultProvider<String> {

  private static final Logger LOG = LogManager.getLogger();
  // This is not a global instance because it is only meant to decode the typed data JSON
  private static final ObjectMapper OBJECT_MAPPER =
      JsonMapper.builder()
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .defaultPropertyInclusion(
              JsonInclude.Value.construct(
                  JsonInclude.Include.NON_NULL, JsonInclude.Include.USE_DEFAULTS))
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .build();

  private final SignerForIdentifier transactionSignerProvider;

  public EthSignTypedDataResultProvider(final SignerForIdentifier transactionSignerProvider) {
    this.transactionSignerProvider = transactionSignerProvider;
  }

  @Override
  public String createResponseResult(final JsonRpcRequest request) {
    LOG.debug("Processing eth_signTypedData request {}", request.getId());

    final List<?> params = validateAndGetParams(request);

    // Address validation
    final Object addressParam = params.getFirst();
    if (!(addressParam instanceof String addressStr)) {
      throw new JsonRpcException(INVALID_PARAMS);
    }
    final String normalizedAddress = normaliseIdentifier(addressStr);

    if (!transactionSignerProvider.isSignerAvailable(normalizedAddress)) {
      LOG.debug("Address {} not available for signing", normalizedAddress);
      throw new JsonRpcException(SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT);
    }

    // Typed data processing
    final String typedDataJson = convertToTypedDataJson(params.get(1));

    try {
      final StructuredDataEncoder dataEncoder = new StructuredDataEncoder(typedDataJson);
      final Bytes structuredData = Bytes.of(dataEncoder.getStructuredData());

      return transactionSignerProvider
          .sign(normalizedAddress, structuredData)
          .orElseThrow(
              () -> {
                LOG.debug("Unexpected failure signing for {}", normalizedAddress);
                return new JsonRpcException(SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT);
              });
    } catch (final IOException e) {
      LOG.warn("EIP-712 encoding failed for request {}", request.getId(), e);
      throw new JsonRpcException(INVALID_PARAMS);
    }
  }

  private List<?> validateAndGetParams(final JsonRpcRequest request) {
    final Object params = request.getParams();
    if (params == null) {
      throw new JsonRpcException(INVALID_PARAMS);
    }
    // param 1: account address, param 2: JSON data
    if (params instanceof List<?> paramList) {
      if (paramList.size() < 2) {
        throw new JsonRpcException(INVALID_PARAMS);
      }
      return paramList;
    } else {
      throw new JsonRpcException(INVALID_PARAMS);
    }
  }

  private String convertToTypedDataJson(final Object jsonData) {
    try {
      if (jsonData instanceof String jsonStr) {
        OBJECT_MAPPER.readTree(jsonStr); // Validate
        return jsonStr;
      }
      return OBJECT_MAPPER.writeValueAsString(jsonData);
    } catch (IOException e) {
      throw new JsonRpcException(INVALID_PARAMS);
    }
  }
}
