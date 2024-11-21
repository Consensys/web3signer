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
package tech.pegasys.web3signer.core.service.jsonrpc;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static tech.pegasys.web3signer.core.service.jsonrpc.response.JsonRpcError.INVALID_PARAMS;

import tech.pegasys.web3signer.core.service.jsonrpc.exceptions.JsonRpcException;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.internalresponse.EthAccountsResultProvider;
import tech.pegasys.web3signer.signing.secp256k1.EthPublicKeyUtils;

import java.security.interfaces.ECPublicKey;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Keys;

public class EthAccountsResultProviderTest {
  private static ECPublicKey publicKeyA;
  private static ECPublicKey publicKeyB;
  private static ECPublicKey publicKeyC;

  private static String addressA;
  private static String addressB;
  private static String addressC;

  @BeforeAll
  static void init() {
    publicKeyA = (ECPublicKey) EthPublicKeyUtils.generateK256KeyPair().getPublic();
    publicKeyB = (ECPublicKey) EthPublicKeyUtils.generateK256KeyPair().getPublic();
    publicKeyC = (ECPublicKey) EthPublicKeyUtils.generateK256KeyPair().getPublic();

    addressA = Keys.getAddress(EthPublicKeyUtils.toHexString(publicKeyA));
    addressB = Keys.getAddress(EthPublicKeyUtils.toHexString(publicKeyB));
    addressC = Keys.getAddress(EthPublicKeyUtils.toHexString(publicKeyC));
  }

  @Test
  public void valueFromBodyProviderInsertedToResult() {
    final int id = 1;
    final EthAccountsResultProvider resultProvider =
        new EthAccountsResultProvider(() -> Set.of(publicKeyA));

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_accounts");
    request.setId(new JsonRpcRequestId(id));
    request.setParams(emptyList());

    final List<String> addresses = resultProvider.createResponseResult(request);
    assertThat(addresses).containsExactly("0x" + addressA);
  }

  @Test
  public void ifParamsContainsANonEmptyArrayExceptionIsThrownWithInvalidParams() {
    final int id = 1;
    final EthAccountsResultProvider resultProvider =
        new EthAccountsResultProvider(() -> Set.of(publicKeyA));

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_accounts");
    request.setId(new JsonRpcRequestId(id));
    request.setParams(singletonList(5));

    final Throwable thrown = catchThrowable(() -> resultProvider.createResponseResult(request));
    assertThat(thrown).isInstanceOf(JsonRpcException.class);
    final JsonRpcException rpcException = (JsonRpcException) thrown;
    assertThat(rpcException.getJsonRpcError()).isEqualTo(INVALID_PARAMS);
  }

  @Test
  public void ifParamIsAnObjectExceptionIsThrownWithInvalidParams() {
    final int id = 1;
    final EthAccountsResultProvider resultProvider =
        new EthAccountsResultProvider(() -> Set.of(publicKeyA));

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_accounts");
    request.setId(new JsonRpcRequestId(id));
    request.setParams(5);

    final Throwable thrown = catchThrowable(() -> resultProvider.createResponseResult(request));
    assertThat(thrown).isInstanceOf(JsonRpcException.class);
    final JsonRpcException rpcException = (JsonRpcException) thrown;
    assertThat(rpcException.getJsonRpcError()).isEqualTo(INVALID_PARAMS);
  }

  @Test
  public void missingParametersIsOk() {
    final int id = 1;
    final EthAccountsResultProvider resultProvider =
        new EthAccountsResultProvider(() -> Set.of(publicKeyA));

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_accounts");
    request.setId(new JsonRpcRequestId(id));

    final List<String> addressses = resultProvider.createResponseResult(request);
    assertThat(addressses).containsExactly("0x" + addressA);
  }

  @Test
  public void multipleValueFromBodyProviderInsertedToResult() {
    final Set<ECPublicKey> availableAddresses = Set.of(publicKeyA, publicKeyB, publicKeyC);
    final int id = 1;
    final EthAccountsResultProvider resultProvider =
        new EthAccountsResultProvider(() -> availableAddresses);

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_accounts");
    request.setId(new JsonRpcRequestId(id));
    request.setParams(emptyList());

    final List<String> reportedAddresses = resultProvider.createResponseResult(request);
    assertThat(reportedAddresses)
        .containsExactlyInAnyOrder("0x" + addressA, "0x" + addressB, "0x" + addressC);
  }

  @Test
  public void accountsReturnedAreDynamicallyFetchedFromProvider() {
    final Set<ECPublicKey> addresses = Sets.newHashSet(publicKeyA, publicKeyB, publicKeyC);

    final Supplier<Set<ECPublicKey>> supplier = () -> addresses;
    final EthAccountsResultProvider resultProvider = new EthAccountsResultProvider(supplier);

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_accounts");
    request.setId(new JsonRpcRequestId(1));
    request.setParams(emptyList());

    List<String> reportedAddresses = resultProvider.createResponseResult(request);
    assertThat(reportedAddresses)
        .containsExactlyElementsOf(
            Stream.of("0x" + addressA, "0x" + addressB, "0x" + addressC)
                .sorted()
                .collect(Collectors.toList()));

    addresses.remove(publicKeyA);

    reportedAddresses = resultProvider.createResponseResult(request);
    assertThat(reportedAddresses)
        .containsExactlyElementsOf(
            Stream.of("0x" + addressB, "0x" + addressC).sorted().collect(Collectors.toList()));
  }

  @Test
  public void accountsReturnedAreSortedAlphabetically() {
    final Supplier<Set<ECPublicKey>> supplier = () -> Set.of(publicKeyA, publicKeyB, publicKeyC);
    final EthAccountsResultProvider resultProvider = new EthAccountsResultProvider(supplier);

    final JsonRpcRequest request = new JsonRpcRequest("2.0", "eth_accounts");
    request.setId(new JsonRpcRequestId(1));
    request.setParams(emptyList());

    List<String> reportedAddresses = resultProvider.createResponseResult(request);
    assertThat(reportedAddresses)
        .containsExactlyElementsOf(
            Stream.of("0x" + addressA, "0x" + addressB, "0x" + addressC)
                .sorted()
                .collect(Collectors.toList()));
  }
}
