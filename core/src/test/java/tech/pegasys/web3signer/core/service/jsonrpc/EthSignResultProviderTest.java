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
import static org.mockito.Mockito.doReturn;
import static tech.pegasys.web3signer.core.service.jsonrpc.response.JsonRpcError.INVALID_PARAMS;
import static tech.pegasys.web3signer.core.service.jsonrpc.response.JsonRpcError.SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT;

import tech.pegasys.web3signer.core.service.http.handlers.signing.SignerForIdentifier;
import tech.pegasys.web3signer.core.service.jsonrpc.exceptions.JsonRpcException;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.internalresponse.EthSignResultProvider;
import tech.pegasys.web3signer.core.util.EthMessageUtil;
import tech.pegasys.web3signer.signing.SecpArtifactSignature;
import tech.pegasys.web3signer.signing.secp256k1.Signature;

import java.math.BigInteger;
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
import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

@ExtendWith(MockitoExtension.class)
public class EthSignResultProviderTest {

  @Mock SignerForIdentifier<SecpArtifactSignature> transactionSignerProvider;

  @ParameterizedTest
  @ArgumentsSource(InvalidParamsProvider.class)
  @NullSource
  public void ifParamIsInvalidExceptionIsThrownWithInvalidParams(final Object params) {

    final EthSignResultProvider resultProvider =
        new EthSignResultProvider(transactionSignerProvider);

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_sign");
    request.setId(new JsonRpcRequestId(1));
    request.setParams(params);

    final Throwable thrown = catchThrowable(() -> resultProvider.createResponseResult(request));
    assertThat(thrown).isInstanceOf(JsonRpcException.class);
    final JsonRpcException rpcException = (JsonRpcException) thrown;
    assertThat(rpcException.getJsonRpcError()).isEqualTo(INVALID_PARAMS);
  }

  @Test
  public void ifAddressIsNotUnlockedExceptionIsThrownWithSigningNotUnlocked() {

    final EthSignResultProvider resultProvider =
        new EthSignResultProvider(transactionSignerProvider);
    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_sign");
    request.setId(new JsonRpcRequestId(1));
    request.setParams(List.of("address", "message"));
    final Throwable thrown = catchThrowable(() -> resultProvider.createResponseResult(request));
    assertThat(thrown).isInstanceOf(JsonRpcException.class);
    final JsonRpcException rpcException = (JsonRpcException) thrown;
    assertThat(rpcException.getJsonRpcError()).isEqualTo(SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT);
  }

  @Test
  public void signatureHasTheExpectedFormat() {
    final BigInteger v = BigInteger.ONE;
    final BigInteger r = BigInteger.TWO;
    final BigInteger s = BigInteger.TEN;
    final SecpArtifactSignature secpArtifactSignature =
        new SecpArtifactSignature(new Signature(v, r, s));

    doReturn(Optional.of(SecpArtifactSignature.toBytes(secpArtifactSignature).toHexString()))
        .when(transactionSignerProvider)
        .sign(any(), any(Bytes.class));

    final EthSignResultProvider resultProvider =
        new EthSignResultProvider(transactionSignerProvider);

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_sign");
    final int id = 1;
    request.setId(new JsonRpcRequestId(id));
    request.setParams(List.of("address", "message"));

    final Object result = resultProvider.createResponseResult(request);
    assertThat(result).isInstanceOf(String.class);
    final String hexSignature = (String) result;
    assertThat(hexSignature).hasSize(132);

    final byte[] signature = Numeric.hexStringToByteArray(hexSignature);

    assertThat(new BigInteger(1, signature, 0, 32)).isEqualTo(r);
    assertThat(new BigInteger(1, signature, 32, 32)).isEqualTo(s);
    assertThat(new BigInteger(1, signature, 64, 1)).isEqualTo(v);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "0xdeadbeaf",
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Tubulum"
            + " fuisse, qua illum, cuius is condemnatus est rogatione, P. Eaedem res maneant alio modo."
      })
  public void returnsExpectedSignature(final String message) {
    final ECKeyPair keyPair =
        ECKeyPair.create(
            Numeric.hexStringToByteArray(
                "0x1618fc3e47aec7e70451256e033b9edb67f4c469258d8e2fbb105552f141ae41"));
    doAnswer(
            answer -> {
              Bytes data = answer.getArgument(1, Bytes.class);
              final Sign.SignatureData signature = Sign.signMessage(data.toArrayUnsafe(), keyPair);
              return Optional.of(
                  SecpArtifactSignature.toBytes(
                          new SecpArtifactSignature(
                              new Signature(
                                  new BigInteger(signature.getV()),
                                  new BigInteger(1, signature.getR()),
                                  new BigInteger(1, signature.getS()))))
                      .toHexString());
            })
        .when(transactionSignerProvider)
        .sign(anyString(), any(Bytes.class));

    final EthSignResultProvider resultProvider =
        new EthSignResultProvider(transactionSignerProvider);

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_sign");
    final int id = 1;
    request.setId(new JsonRpcRequestId(id));
    request.setParams(List.of("address", message));

    final Object result = resultProvider.createResponseResult(request);
    assertThat(result).isInstanceOf(String.class);
    final String hexSignature = (String) result;
    final byte[] signature = Numeric.hexStringToByteArray(hexSignature);
    final ECDSASignature expectedSignature =
        keyPair.sign(Hash.sha3(EthMessageUtil.getEthereumMessage(message).toArray()));
    assertThat(new BigInteger(1, signature, 0, 32)).isEqualTo(expectedSignature.r);
    assertThat(new BigInteger(1, signature, 32, 32)).isEqualTo(expectedSignature.s);
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
}
