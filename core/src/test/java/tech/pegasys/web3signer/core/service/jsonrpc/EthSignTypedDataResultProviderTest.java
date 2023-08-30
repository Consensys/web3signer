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
package tech.pegasys.web3signer.core.service.jsonrpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.web3j.crypto.Keys.getAddress;
import static org.web3j.crypto.Sign.signMessage;
import static tech.pegasys.web3signer.core.service.jsonrpc.response.JsonRpcError.INVALID_PARAMS;
import static tech.pegasys.web3signer.core.service.jsonrpc.response.JsonRpcError.SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT;

import tech.pegasys.web3signer.core.service.http.handlers.signing.SignerForIdentifier;
import tech.pegasys.web3signer.core.service.jsonrpc.exceptions.JsonRpcException;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.internalresponse.EthSignTypedDataResultProvider;
import tech.pegasys.web3signer.signing.SecpArtifactSignature;
import tech.pegasys.web3signer.signing.secp256k1.Signature;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

@ExtendWith(MockitoExtension.class)
public class EthSignTypedDataResultProviderTest {
  private static final String PRIVATE_KEY_STRING =
      "a392604efc2fad9c0b3da43b5f698a2e3f270f170d859912be0d54742275c5f6";
  private static final String PUBLIC_KEY_STRING =
      "0x506bc1dc099358e5137292f4efdd57e400f29ba5132aa5d12b18dac1c1f6aab"
          + "a645c0b7b58158babbfa6c6cd5a48aa7340a8749176b120e8516216787a13dc76";

  private static final String EIP712_VALID_JSON =
      "{\"types\": {    \"EIP712Domain\": [      {\"name\": \"name\", \"type\": \"string\"},      {\"name\": \"version\", \"type\": \"string\"},      {\"name\": \"chainId\", \"type\": \"uint256\"},      {\"name\": \"verifyingContract\", \"type\": \"address\"}    ],    \"Person\": [      {\"name\": \"name\", \"type\": \"string\"},      {\"name\": \"wallet\", \"type\": \"address\"}    ]  },  \"domain\": {    \"name\": \"My Dapp\",    \"version\": \"1.0\",    \"chainId\": 1,    \"verifyingContract\": \"0xCcCCccccCCCCcCCCCCCcCcCccCcCCCcCcccccccC\"  },  \"primaryType\": \"Person\",  \"message\": {    \"name\": \"John Doe\",    \"wallet\": \"0xAb5801a7D398351b8bE11C439e05C5B3259aeC9B\"  }}";

  private static final BigInteger PRIVATE_KEY = Numeric.toBigInt(PRIVATE_KEY_STRING);
  private static final BigInteger PUBLIC_KEY = Numeric.toBigInt(PUBLIC_KEY_STRING);

  private static final ECKeyPair KEY_PAIR = new ECKeyPair(PRIVATE_KEY, PUBLIC_KEY);

  @Mock SignerForIdentifier<SecpArtifactSignature> transactionSignerProvider;

  @ParameterizedTest
  @ArgumentsSource(InvalidParamsProvider.class)
  @NullSource
  public void ifParamIsInvalidExceptionIsThrownWithInvalidParams(final Object params) {

    final EthSignTypedDataResultProvider resultProvider =
        new EthSignTypedDataResultProvider(transactionSignerProvider);

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_signTypedData");
    request.setId(new JsonRpcRequestId(1));
    request.setParams(params);

    final Throwable thrown = catchThrowable(() -> resultProvider.createResponseResult(request));
    assertThat(thrown).isInstanceOf(JsonRpcException.class);
    final JsonRpcException rpcException = (JsonRpcException) thrown;
    assertThat(rpcException.getJsonRpcError()).isEqualTo(INVALID_PARAMS);
  }

  @Test
  public void ifAddressIsNotUnlockedExceptionIsThrownWithSigningNotUnlocked()
      throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {

    final EthSignTypedDataResultProvider resultProvider =
        new EthSignTypedDataResultProvider(transactionSignerProvider);
    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_signTypedData");
    request.setId(new JsonRpcRequestId(1));
    request.setParams(
        List.of(getAddress(Keys.createEcKeyPair().getPublicKey()), EIP712_VALID_JSON));
    final Throwable thrown = catchThrowable(() -> resultProvider.createResponseResult(request));
    assertThat(thrown).isInstanceOf(JsonRpcException.class);
    final JsonRpcException rpcException = (JsonRpcException) thrown;
    assertThat(rpcException.getJsonRpcError()).isEqualTo(SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT);
  }

  @ParameterizedTest
  @ValueSource(strings = {EIP712_VALID_JSON})
  public void returnsExpectedSignature(final String message) throws IOException {

    doAnswer(
            answer -> {
              Bytes data = answer.getArgument(1, Bytes.class);
              final Sign.SignatureData signatureData = signMessage(data.toArrayUnsafe(), KEY_PAIR);
              return Optional.of(hexFromSignatureData(signatureData));
            })
        .when(transactionSignerProvider)
        .sign(anyString(), any(Bytes.class));

    final EthSignTypedDataResultProvider resultProvider =
        new EthSignTypedDataResultProvider(transactionSignerProvider);

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_signTypedData");
    final int id = 1;
    request.setId(new JsonRpcRequestId(id));
    request.setParams(List.of("address", message));

    final Object result = resultProvider.createResponseResult(request);
    assertThat(result).isInstanceOf(String.class);
    final String hexSignature = (String) result;
    Sign.SignatureData expectedSignature = Sign.signTypedData(message, KEY_PAIR);
    assertThat(hexSignature).isEqualTo(hexFromSignatureData(expectedSignature));
  }

  private static class InvalidParamsProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(final ExtensionContext context) {
      return Stream.of(
          Arguments.of(Collections.emptyList()),
          Arguments.of(Collections.singleton(2)),
          Arguments.of(List.of(1, 2, 3)),
          Arguments.of(new Object()));
    }
  }

  private String hexFromSignatureData(Sign.SignatureData signature) {
    return SecpArtifactSignature.toBytes(
            new SecpArtifactSignature(
                new Signature(
                    new BigInteger(signature.getV()),
                    new BigInteger(1, signature.getR()),
                    new BigInteger(1, signature.getS()))))
        .toHexString();
  }
}
