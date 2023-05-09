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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.google.common.collect.Lists;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

public class JsonRpcRequestTest {

  @Test
  public void basicDecoding() {

    final JsonObject input = new JsonObject();
    input.put("jsonrpc", 2.0);
    input.put("method", "mine");
    input.put("params", 5);

    final JsonRpcRequest request = input.mapTo(JsonRpcRequest.class);
    assertThat(request.getVersion()).isEqualTo("2.0");
    assertThat(request.getMethod()).isEqualTo("mine");
    assertThat(request.getParams()).isEqualTo(5);
  }

  @Test
  public void arrayOfParamsDecodesIntoSingleObject() {

    final JsonObject input = new JsonObject();
    input.put("jsonrpc", 2.0);
    input.put("method", "mine");
    input.put("params", singletonList(5));

    final JsonRpcRequest request = input.mapTo(JsonRpcRequest.class);
    assertThat(request.getVersion()).isEqualTo("2.0");
    assertThat(request.getMethod()).isEqualTo("mine");
    assertThat(request.getParams()).isEqualTo(singletonList(5));
  }

  @Test
  public void multiEntriesInParamsIsValid() {
    final JsonObject input = new JsonObject();
    input.put("jsonrpc", 2.0);
    input.put("method", "mine");
    input.put("params", Lists.newArrayList(5, 5));

    final JsonRpcRequest request = input.mapTo(JsonRpcRequest.class);
    assertThat(request.getParams()).isInstanceOf(List.class);
    @SuppressWarnings("unchecked")
    final List<Object> paramArray = (List) request.getParams();
    assertThat(paramArray).containsExactly(5, 5);
    assertThat(request.getVersion()).isEqualTo("2.0");
    assertThat(request.getMethod()).isEqualTo("mine");
  }

  @Test
  public void emptyParametersIsValid() {
    final JsonObject input = new JsonObject();
    input.put("jsonrpc", 2.0);
    input.put("method", "mine");
    input.put("params", emptyList());

    final JsonRpcRequest request = input.mapTo(JsonRpcRequest.class);
    assertThat(request.getParams()).isInstanceOf(List.class);
    @SuppressWarnings("unchecked")
    final List<Object> paramArray = (List<Object>) request.getParams();
    assertThat(paramArray.size()).isZero();
    assertThat(request.getVersion()).isEqualTo("2.0");
    assertThat(request.getMethod()).isEqualTo("mine");
  }

  @Test
  public void ensureEqualityWorksForLists() {
    final JsonRpcRequest first = new JsonRpcRequest("2.0", "method");
    first.setParams(Lists.newArrayList(1, 2));
    final JsonRpcRequest second = new JsonRpcRequest("2.0", "method");
    second.setParams(Lists.newArrayList(1, 2));

    assertThat(first.equals(second)).isTrue();
  }

  @Test
  public void ensureEqualityWorksForArrays() {
    final JsonRpcRequest first = new JsonRpcRequest("2.0", "method");
    first.setParams(new Object[] {1, 2});
    final JsonRpcRequest second = new JsonRpcRequest("2.0", "method");
    second.setParams(new Object[] {1, 2});

    assertThat(first.equals(second)).isTrue();
  }
}
