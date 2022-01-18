/*
 * Copyright 2021 ConsenSys AG.
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
package tech.pegasys.web3signer.tests.keymanager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;

import tech.pegasys.web3signer.core.service.http.handlers.keymanager.delete.DeleteKeystoresRequestBody;

import java.io.IOException;
import java.net.URISyntaxException;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

public class DeleteKeystoresAcceptanceTest extends KeyManagerTestBase {

  private final String singleEntrySlashingData =
      "{\"metadata\" : {\n"
          + "  \"interchange_format_version\" : \"5\",\n"
          + "  \"genesis_validators_root\" : \"0x04700007fabc8282644aed6d1c7c9e21d38a03a0c4ba193f3afe428824b3a673\"\n"
          + "},\n"
          + "\"data\" : [ {\n"
          + "  \"pubkey\" : \"0x98d083489b3b06b8740da2dfec5cc3c01b2086363fe023a9d7dc1f907633b1ff11f7b99b19e0533e969862270061d884\",\n"
          + "  \"signed_blocks\" : [ {\n"
          + "    \"slot\" : \"12345\",\n"
          + "    \"signing_root\" : \"0x4ff6f743a43f3b4f95350831aeaf0a122a1a392922c45d804280284a69eb850b\"\n"
          + "  } ],\n"
          + "  \"signed_attestations\" : [ {\n"
          + "    \"source_epoch\" : \"5\",\n"
          + "    \"target_epoch\" : \"6\",\n"
          + "    \"signing_root\" : \"0x30752da173420e64a66f6ca6b97c55a96390a3158a755ecd277812488bb84e57\"\n"
          + "  } ]\n"
          + "} ]\n"
          + "}";

  private final String emptySlashingData =
      "{\"metadata\" : {\n"
          + "  \"interchange_format_version\" : \"5\",\n"
          + "  \"genesis_validators_root\" : \"0x04700007fabc8282644aed6d1c7c9e21d38a03a0c4ba193f3afe428824b3a673\"\n"
          + "},\n"
          + "\"data\" : [ ]\n"
          + "}";

  @Test
  public void invalidRequestBodyReturnsError() throws URISyntaxException {
    setupSignerWithKeyManagerApi(true);
    final Response response = callDeleteKeystores("{\"invalid\": \"json body\"}");
    response.then().assertThat().statusCode(400);
  }

  @Test
  public void deletingNonExistingKeyReturnNotFound() throws URISyntaxException {
    createBlsKey("eth2/bls_keystore_2.json", "otherpassword");
    setupSignerWithKeyManagerApi(true);
    callDeleteKeystores(composeRequestBody())
        .then()
        .contentType(ContentType.JSON)
        .assertThat()
        .statusCode(200)
        .body("data[0].status", is("not_found"))
        .and()
        .body("slashing_protection", is(emptySlashingData));
  }

  @Test
  public void deletingExistingKeyReturnDeleted() throws URISyntaxException {
    createBlsKey("eth2/bls_keystore.json", "somepassword");
    setupSignerWithKeyManagerApi(true);
    callDeleteKeystores(composeRequestBody())
        .then()
        .contentType(ContentType.JSON)
        .assertThat()
        .statusCode(200)
        .body("data[0].status", is("deleted"))
        .and()
        .body("slashing_protection", is(singleEntrySlashingData));
  }

  @Test
  public void deletingExistingTwiceReturnsNotActive() throws URISyntaxException {
    createBlsKey("eth2/bls_keystore.json", "somepassword");
    setupSignerWithKeyManagerApi(true);
    callDeleteKeystores(composeRequestBody())
        .then()
        .contentType(ContentType.JSON)
        .assertThat()
        .statusCode(200)
        .body("data[0].status", is("deleted"))
        .and()
        .body("slashing_protection", is(singleEntrySlashingData));

    // call API again with same key should return not_active with the same exported slashing
    // protection data
    callDeleteKeystores(composeRequestBody())
        .then()
        .contentType(ContentType.JSON)
        .assertThat()
        .statusCode(200)
        .body("data[0].status", is("not_active"))
        .and()
        .body("slashing_protection", is(singleEntrySlashingData));
  }

  @Test
  public void deletingRemovesSignerFromActiveSigners() throws URISyntaxException {
    final String firstPubkey = createBlsKey("eth2/bls_keystore.json", "somepassword");
    final String secondPubKey = createBlsKey("eth2/bls_keystore_2.json", "otherpassword");
    setupSignerWithKeyManagerApi(true);

    callListKeys()
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("data.size()", is(2))
        .and()
        .body("data[0].validating_pubkey", is(firstPubkey))
        .and()
        .body("data[1].validating_pubkey", is(secondPubKey));

    callDeleteKeystores(composeRequestBody())
        .then()
        .contentType(ContentType.JSON)
        .assertThat()
        .statusCode(200)
        .body("data[0].status", is("deleted"))
        .and()
        .body("slashing_protection", is(singleEntrySlashingData));

    callListKeys()
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("data.size()", is(1))
        .and()
        .body("data[0].validating_pubkey", is(secondPubKey));
  }

  @Test
  public void testRequestBodyParsing() throws IOException {
    final ObjectMapper objectMapper = new ObjectMapper();
    final DeleteKeystoresRequestBody parsedBody =
        objectMapper.readValue(composeRequestBody(), DeleteKeystoresRequestBody.class);
    assertThat(parsedBody.getPubkeys().get(0))
        .isEqualTo(
            "0x98d083489b3b06b8740da2dfec5cc3c01b2086363fe023a9d7dc1f907633b1ff11f7b99b19e0533e969862270061d884");
  }

  private String composeRequestBody() {
    final JsonObject requestBody =
        new JsonObject()
            .put(
                "pubkeys",
                new JsonArray()
                    .add(
                        "0x98d083489b3b06b8740da2dfec5cc3c01b2086363fe023a9d7dc1f907633b1ff11f7b99b19e0533e969862270061d884"));
    return requestBody.toString();
  }
}
