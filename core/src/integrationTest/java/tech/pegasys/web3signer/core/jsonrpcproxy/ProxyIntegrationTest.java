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
package tech.pegasys.web3signer.core.jsonrpcproxy;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;

import java.util.List;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.Json;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.model.Header;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.NetVersion;

public class ProxyIntegrationTest extends IntegrationTestBase {

  private static final String LOGIN_BODY = "{\"username\":\"username1\",\"password\":\"pegasys\"}";
  private static final String LOGIN_RESPONSE = "{\"token\":\"eyJ0\"}";
  private static final Iterable<Entry<String, String>> REQUEST_HEADERS =
      singletonList(ImmutablePair.of("Accept", "*/*"));
  private static final Iterable<Entry<String, String>> RESPONSE_HEADERS =
      singletonList(ImmutablePair.of("Content-Type", "Application/Json"));

  private static final String ROOT_PATH = "/arbitraryRootPath";

  @BeforeAll
  public static void localSetup() {
    try {
      setupWeb3Signer(ROOT_PATH);
    } catch (final Exception e) {
      throw new RuntimeException("Failed to setup web3signer", e);
    }
  }

  @Test
  void requestWithHeadersIsProxied() {
    final String netVersionRequest = Json.encode(jsonRpc().netVersion());

    final Response<String> netVersion = new NetVersion();
    netVersion.setResult("4");
    final String netVersionResponse = Json.encode(netVersion);

    setUpEthNodeResponse(
        request.ethNode(netVersionRequest), response.ethNode(RESPONSE_HEADERS, netVersionResponse));

    sendPostRequestAndVerifyResponse(
        request.web3signer(REQUEST_HEADERS, netVersionRequest),
        response.web3Signer(RESPONSE_HEADERS, netVersionResponse));

    verifyEthNodeReceived(REQUEST_HEADERS, netVersionRequest);
  }

  @Test
  void requestWithHostHeaderIsRenamedToXForwardedHost() {
    final String netVersionRequest = Json.encode(jsonRpc().netVersion());

    final Response<String> netVersion = new NetVersion();
    netVersion.setResult("4");
    final String netVersionResponse = Json.encode(netVersion);

    setUpEthNodeResponse(
        request.ethNode(netVersionRequest), response.ethNode(RESPONSE_HEADERS, netVersionResponse));

    final Iterable<Entry<String, String>> requestHeaders =
        List.of(ImmutablePair.of("Accept", "*.*"), ImmutablePair.of("Host", "localhost"));

    sendPostRequestAndVerifyResponse(
        request.web3signer(requestHeaders, netVersionRequest),
        response.web3Signer(RESPONSE_HEADERS, netVersionResponse));

    final Iterable<Entry<String, String>> expectedForwardedHeaders =
        List.of(
            ImmutablePair.of("Accept", "*.*"), ImmutablePair.of("X-Forwarded-Host", "localhost"));

    verifyEthNodeReceived(expectedForwardedHeaders, netVersionRequest);
  }

  @Test
  void requestWithHostHeaderOverwritesExistingXForwardedHost() {
    final String netVersionRequest = Json.encode(jsonRpc().netVersion());

    final Response<String> netVersion = new NetVersion();
    netVersion.setResult("4");
    final String netVersionResponse = Json.encode(netVersion);

    setUpEthNodeResponse(
        request.ethNode(netVersionRequest), response.ethNode(RESPONSE_HEADERS, netVersionResponse));

    final Iterable<Entry<String, String>> requestHeaders =
        List.of(
            ImmutablePair.of("Accept", "*.*"),
            ImmutablePair.of("Host", "localhost"),
            ImmutablePair.of("X-Forwarded-Host", "nowhere"));

    sendPostRequestAndVerifyResponse(
        request.web3signer(requestHeaders, netVersionRequest),
        response.web3Signer(RESPONSE_HEADERS, netVersionResponse));

    final Iterable<Entry<String, String>> expectedForwardedHeaders =
        List.of(
            ImmutablePair.of("Accept", "*.*"), ImmutablePair.of("X-Forwarded-Host", "localhost"));

    verifyEthNodeReceived(expectedForwardedHeaders, netVersionRequest);
  }

  @Test
  void requestReturningErrorIsProxied() {
    final String ethProtocolVersionRequest = Json.encode(jsonRpc().ethProtocolVersion());

    setUpEthNodeResponse(
        request.ethNode(ethProtocolVersionRequest),
        response.ethNode("Not Found", HttpResponseStatus.NOT_FOUND));

    sendPostRequestAndVerifyResponse(
        request.web3signer(ethProtocolVersionRequest),
        response.web3Signer("Not Found", HttpResponseStatus.NOT_FOUND));

    verifyEthNodeReceived(ethProtocolVersionRequest);
  }

  @Test
  void postRequestToNonRootPathIsProxied() {
    setUpEthNodeResponse(
        request.ethNode(LOGIN_BODY),
        response.ethNode(RESPONSE_HEADERS, LOGIN_RESPONSE, HttpResponseStatus.OK));

    sendPostRequestAndVerifyResponse(
        request.web3signer(REQUEST_HEADERS, LOGIN_BODY),
        response.web3Signer(RESPONSE_HEADERS, LOGIN_RESPONSE),
        "/login");

    verifyEthNodeReceived(REQUEST_HEADERS, LOGIN_BODY, ROOT_PATH + "/login");
  }

  @Test
  void getRequestToNonRootPathIsProxied() {
    setUpEthNodeResponse(
        request.ethNode(LOGIN_BODY),
        response.ethNode(RESPONSE_HEADERS, LOGIN_RESPONSE, HttpResponseStatus.OK));

    // Whilst a get request doesn't normally have a body, it can and we want to ensure the request
    // is proxied as is
    sendGetRequestAndVerifyResponse(
        request.web3signer(REQUEST_HEADERS, LOGIN_BODY),
        response.web3Signer(RESPONSE_HEADERS, LOGIN_RESPONSE),
        "/login");

    verifyEthNodeReceived(REQUEST_HEADERS, LOGIN_BODY, ROOT_PATH + "/login");
  }

  @Test
  void putRequestToNonRootPathIsProxied() {
    setUpEthNodeResponse(
        request.ethNode(LOGIN_BODY),
        response.ethNode(RESPONSE_HEADERS, LOGIN_RESPONSE, HttpResponseStatus.OK));

    sendPutRequestAndVerifyResponse(
        request.web3signer(REQUEST_HEADERS, LOGIN_BODY),
        response.web3Signer(RESPONSE_HEADERS, LOGIN_RESPONSE),
        "/login");

    verifyEthNodeReceived(REQUEST_HEADERS, LOGIN_BODY, ROOT_PATH + "/login");
  }

  @Test
  void deleteRequestToNonRootPathIsProxied() {
    setUpEthNodeResponse(
        request.ethNode(LOGIN_BODY),
        response.ethNode(RESPONSE_HEADERS, LOGIN_RESPONSE, HttpResponseStatus.OK));

    sendDeleteRequestAndVerifyResponse(
        request.web3signer(REQUEST_HEADERS, LOGIN_BODY),
        response.web3Signer(RESPONSE_HEADERS, LOGIN_RESPONSE),
        "/login");

    verifyEthNodeReceived(REQUEST_HEADERS, LOGIN_BODY, ROOT_PATH + "/login");
  }

  @Test
  void requestWithOriginHeaderProducesResponseWithCorsHeader() {
    final String netVersionRequest = Json.encode(jsonRpc().netVersion());
    final Response<String> netVersion = new NetVersion();
    netVersion.setResult("4");
    final String netVersionResponse = Json.encode(netVersion);

    final String originDomain = "sample.com";

    setUpEthNodeResponse(
        request.ethNode(netVersionRequest), response.ethNode(RESPONSE_HEADERS, netVersionResponse));

    final List<Entry<String, String>> expectedResponseHeaders =
        Lists.newArrayList(RESPONSE_HEADERS);
    expectedResponseHeaders.add(
        ImmutablePair.of(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN.toString(), "sample.com"));

    final Iterable<Entry<String, String>> requestHeaders =
        List.of(
            ImmutablePair.of("Accept", "*/*"),
            ImmutablePair.of("Host", "localhost"),
            ImmutablePair.of("Origin", originDomain));

    sendPostRequestAndVerifyResponse(
        request.web3signer(requestHeaders, netVersionRequest),
        response.web3Signer(expectedResponseHeaders, netVersionResponse));

    // Cors headers should not be forwarded to the downstream web3 provider (CORS is handled
    // entirely within Web3Signer.
    assertThat(clientAndServer.retrieveRecordedRequests(request().withHeader(new Header("origin"))))
        .isEmpty();
  }

  @Test
  void requestWithMisMatchedDomainReceives403() {
    final String netVersionRequest = Json.encode(jsonRpc().netVersion());
    final Response<String> netVersion = new NetVersion();
    netVersion.setResult("4");
    final String originDomain = "notSample.com";
    final Iterable<Entry<String, String>> requestHeaders =
        List.of(
            ImmutablePair.of("Accept", "*/*"),
            ImmutablePair.of("Host", "localhost"),
            ImmutablePair.of("Origin", originDomain));

    sendPostRequestAndVerifyResponse(
        request.web3signer(requestHeaders, netVersionRequest),
        response.web3Signer("", HttpResponseStatus.FORBIDDEN));
  }

  @Test
  void multiValueHeadersFromDownstreamArePassedBackToCallingApplication() {

    final List<Entry<String, String>> multiValueResponseHeader =
        Lists.newArrayList(RESPONSE_HEADERS);
    multiValueResponseHeader.add(ImmutablePair.of("Random", "firstValue"));
    multiValueResponseHeader.add(ImmutablePair.of("Random", "secondValue"));

    final Iterable<Entry<String, String>> requestHeaders =
        List.of(ImmutablePair.of("Accept", "*/*"), ImmutablePair.of("Host", "localhost"));

    setUpEthNodeResponse(
        request.ethNode(LOGIN_BODY),
        response.ethNode(multiValueResponseHeader, LOGIN_RESPONSE, HttpResponseStatus.OK));

    sendPostRequestAndVerifyResponse(
        request.web3signer(requestHeaders, LOGIN_BODY),
        response.web3Signer(multiValueResponseHeader, LOGIN_RESPONSE),
        "/login");
  }
}
