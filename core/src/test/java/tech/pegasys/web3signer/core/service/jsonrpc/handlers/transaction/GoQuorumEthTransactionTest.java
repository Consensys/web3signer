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
package tech.pegasys.web3signer.core.service.jsonrpc.handlers.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static tech.pegasys.web3signer.core.service.jsonrpc.response.JsonRpcError.ETHER_VALUE_NOT_SUPPORTED;

import tech.pegasys.web3signer.core.service.jsonrpc.EthSendTransactionJsonParameters;
import tech.pegasys.web3signer.core.service.jsonrpc.JsonRpcRequest;
import tech.pegasys.web3signer.core.service.jsonrpc.JsonRpcRequestId;
import tech.pegasys.web3signer.core.service.jsonrpc.exceptions.JsonRpcException;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.sendtransaction.EnclaveLookupIdProvider;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.sendtransaction.transaction.GoQuorumPrivateTransaction;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import io.vertx.core.json.JsonObject;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.web3j.utils.Base64String;

public class GoQuorumEthTransactionTest {

  private GoQuorumPrivateTransaction ethTransaction;
  private EthSendTransactionJsonParameters params;
  private EnclaveLookupIdProvider enclaveLookupIdProvider;

  @BeforeEach
  public void setup() {
    params = new EthSendTransactionJsonParameters("0x7577919ae5df4941180eac211965f275cdce314d");
    params.receiver("0xd46e8dd67c5d32be8058bb8eb970870f07244567");
    params.gas("0x76c0");
    params.gasPrice("0x9184e72a000");
    params.nonce("0x7");
    params.value("0x0");
    params.data(
        "0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675");
    // These extra attributes make it a GoQuorum private transaction via eth_sendTransaction
    params.privateFor(new String[] {"GV8m0VZAccYGAAYMBuYQtKEj0XtpXeaw2APcoBmtA2w="});

    enclaveLookupIdProvider =
        (x) ->
            "9aefeff5ef9cef1dfdeffccff0afefff6fef0ff9faef9feffaeff3ffeffcf8feefafefeffdefef98ba7aafef";
  }

  @Test
  @SuppressWarnings("unchecked")
  public void createsJsonRequestWithoutPrivateFrom() {
    createGoQuorumPrivateTransaction(Optional.empty());

    final JsonRpcRequestId id = new JsonRpcRequestId(2);
    final String transactionString =
        "0xf90114a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f0724456704a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a0fe72a92aede764ce41d06b163d28700b58e5ee8bb1af91d9d54979ea3bdb3e7ea046ae10c94c322fa44ddceb86677c2cd6cc17dfbd766924f41d10a244c512996dac5a6c617045736c3971444c50792f6538382b2f36797643554556497648383379304e3441367748754b58493dedac4756386d30565a41636359474141594d42755951744b456a3058747058656177324150636f426d744132773d8a72657374726963746564";
    final JsonRpcRequest jsonRpcRequest = ethTransaction.jsonRpcRequest(transactionString, id);

    assertThat(jsonRpcRequest.getMethod()).isEqualTo("eth_sendRawPrivateTransaction");
    assertThat(jsonRpcRequest.getVersion()).isEqualTo("2.0");
    assertThat(jsonRpcRequest.getId()).isEqualTo(id);
    final Object[] paramsArray = (Object[]) jsonRpcRequest.getParams();
    assertThat(paramsArray[0]).isEqualTo(transactionString);

    assertThat(paramsArray[1])
        .isEqualTo(getGoQuorumRawTxJsonParams(Optional.empty(), params.privateFor().get()));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void createsJsonRequestWithPrivateFrom() {
    createGoQuorumPrivateTransaction(Optional.of("ZlapEsl9qDLPy/e88+/6yvCUEVIvH83y0N4A6wHuKXI="));

    final JsonRpcRequestId id = new JsonRpcRequestId(2);
    final String transactionString =
        "0xf90114a0e04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f28609184e72a0008276c094d46e8dd67c5d32be8058bb8eb970870f0724456704a9d46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f07244567536a0fe72a92aede764ce41d06b163d28700b58e5ee8bb1af91d9d54979ea3bdb3e7ea046ae10c94c322fa44ddceb86677c2cd6cc17dfbd766924f41d10a244c512996dac5a6c617045736c3971444c50792f6538382b2f36797643554556497648383379304e3441367748754b58493dedac4756386d30565a41636359474141594d42755951744b456a3058747058656177324150636f426d744132773d8a72657374726963746564";
    final JsonRpcRequest jsonRpcRequest = ethTransaction.jsonRpcRequest(transactionString, id);

    assertThat(jsonRpcRequest.getMethod()).isEqualTo("eth_sendRawPrivateTransaction");
    assertThat(jsonRpcRequest.getVersion()).isEqualTo("2.0");
    assertThat(jsonRpcRequest.getId()).isEqualTo(id);
    final Object[] paramsArray = (Object[]) jsonRpcRequest.getParams();
    assertThat(paramsArray[0]).isEqualTo(transactionString);

    assertThat(paramsArray[1])
        .isEqualTo(getGoQuorumRawTxJsonParams(params.privateFrom(), params.privateFor().get()));
  }

  @Test
  public void createGoQuorumPrivateTransactionWithSomeValueShouldFail() {
    // Set the value to something non-zero
    params.value("0x7");

    final Condition<JsonRpcException> hasErrorType =
        new Condition<>(
            exception -> exception.getJsonRpcError().equals(ETHER_VALUE_NOT_SUPPORTED),
            "Must have correct error type: " + ETHER_VALUE_NOT_SUPPORTED);

    assertThatExceptionOfType(JsonRpcException.class)
        .isThrownBy(() -> GoQuorumPrivateTransaction.from(params, null, null, null))
        .has(hasErrorType);
  }

  private void createGoQuorumPrivateTransaction(final Optional<String> privateFrom) {
    privateFrom.ifPresent((p) -> params.privateFrom(p));
    ethTransaction =
        GoQuorumPrivateTransaction.from(
            params, () -> BigInteger.valueOf(7), enclaveLookupIdProvider, new JsonRpcRequestId(1));
    ethTransaction.updateFieldsIfRequired();
  }

  private JsonObject getGoQuorumRawTxJsonParams(
      final Optional<Base64String> privateFrom, final List<Base64String> privateFor) {
    final JsonObject jsonObject = new JsonObject();
    privateFrom.ifPresent((p) -> jsonObject.put("privateFrom", p.toString()));
    jsonObject.put("privateFor", Base64String.unwrapList(privateFor));
    return jsonObject;
  }
}
