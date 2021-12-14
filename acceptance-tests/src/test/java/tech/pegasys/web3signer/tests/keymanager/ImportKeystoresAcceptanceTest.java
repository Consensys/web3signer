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
import static org.hamcrest.Matchers.hasItem;

import com.fasterxml.jackson.core.JsonProcessingException;
import tech.pegasys.web3signer.core.service.http.handlers.keymanager.imports.ImportKeystoreStatus;
import tech.pegasys.web3signer.core.service.http.handlers.keymanager.imports.ImportKeystoresRequestBody;
import tech.pegasys.web3signer.core.signing.KeyType;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.tuweni.bytes.Bytes;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

public class ImportKeystoresAcceptanceTest extends KeyManagerTestBase {

  private static final String PUBLIC_KEY =
      "0x98d083489b3b06b8740da2dfec5cc3c01b2086363fe023a9d7dc1f907633b1ff11f7b99b19e0533e969862270061d884";

  @Test
  public void invalidRequestBodyReturnsError() {
    setupSignerWithKeyManagerApi();
    final Response response = callImportKeystores("{\"invalid\": \"json body\"}");
    response.then().assertThat().statusCode(400);
  }

  @Test
  public void validRequestBodyReturnsSuccess() throws IOException, URISyntaxException {
    setupSignerWithKeyManagerApi();
    final Response response = callImportKeystores(composeRequestBody());
    response
        .then()
        .contentType(ContentType.JSON)
        .assertThat()
        .statusCode(200)
        .body("data.status", hasItem("imported"));
  }

  @Test
  public void existingKeyReturnsDuplicate() throws IOException, URISyntaxException {
    createBlsKey("eth2/bls_keystore.json", "somepassword");
    setupSignerWithKeyManagerApi();

    assertThat(signer.listPublicKeys(KeyType.BLS).size()).isEqualTo(1);

    final Response response = callImportKeystores(composeRequestBody());
    response
        .then()
        .contentType(ContentType.JSON)
        .assertThat()
        .statusCode(200)
        .body("data.status", hasItem("duplicate"));

    assertThat(signer.listPublicKeys(KeyType.BLS).size()).isEqualTo(1);
  }

  @Test
  public void importLoadsNewKeys() throws IOException, URISyntaxException {
    setupSignerWithKeyManagerApi();
    assertThat(signer.listPublicKeys(KeyType.BLS).size()).isEqualTo(0);

    callImportKeystores(composeRequestBody()).then().statusCode(200);

    assertThat(signer.listPublicKeys(KeyType.BLS).size()).isEqualTo(1);
    assertThat(signer.listPublicKeys(KeyType.BLS).get(0)).isEqualTo(PUBLIC_KEY);
  }

  @Test
  public void importLoadsSlashingData() throws IOException, URISyntaxException {
    setupSignerWithKeyManagerApi();
    final Jdbi jdbi = Jdbi.create(signer.getSlashingDbUrl(), DB_USERNAME, DB_PASSWORD);
    final List<Map<String, Object>> validatorsBefore =
        jdbi.withHandle(h -> h.select("SELECT * from validators").mapToMap().list());
    assertThat(validatorsBefore).hasSize(0);

    callImportKeystores(composeRequestBody()).then().statusCode(200);
    final List<Map<String, Object>> validatorsAfter =
        jdbi.withHandle(h -> h.select("SELECT * from validators").mapToMap().list());
    assertThat(validatorsAfter).hasSize(1);
    assertThat(validatorsAfter.get(0).get("public_key"))
        .isEqualTo(Bytes.fromHexString(PUBLIC_KEY).toArray());
  }

  @Test
  public void importingAnExistingKeyDoesntImportSlashingData()
      throws IOException, URISyntaxException {
    // start with a loaded key and no slashing data
    createBlsKey("eth2/bls_keystore.json", "somepassword");
    setupSignerWithKeyManagerApi();
    final Jdbi jdbi = Jdbi.create(signer.getSlashingDbUrl(), DB_USERNAME, DB_PASSWORD);
    // import the same key that was already loaded
    callImportKeystores(composeRequestBody()).then().statusCode(200);
    final List<Map<String, Object>> validators =
        jdbi.withHandle(h -> h.select("SELECT * from validators").mapToMap().list());
    assertThat(validators).hasSize(0);
  }

  @Test
  public void testRequestBodyParsing() throws IOException, URISyntaxException {
    final ObjectMapper objectMapper = new ObjectMapper();
    final ImportKeystoresRequestBody parsedBody =
        objectMapper.readValue(composeRequestBody(), ImportKeystoresRequestBody.class);
    assertThat(new JsonObject(parsedBody.getKeystores().get(0)).getInteger("version")).isEqualTo(4);
    assertThat(new JsonObject(parsedBody.getKeystores().get(0)).getString("pubkey"))
        .isEqualTo(
            "98d083489b3b06b8740da2dfec5cc3c01b2086363fe023a9d7dc1f907633b1ff11f7b99b19e0533e969862270061d884");
    assertThat(parsedBody.getPasswords().get(0)).isEqualTo("somepassword");
    assertThat(
            new JsonObject(parsedBody.getSlashingProtection())
                .getJsonArray("data")
                .getJsonObject(0)
                .getString("pubkey"))
        .isEqualTo(
            "0x8f3f44b74d316c3293cced0c48c72e021ef8d145d136f2908931090e7181c3b777498128a348d07b0b9cd3921b5ca537");
  }

  private String composeRequestBody() throws IOException, URISyntaxException {
    String keystoreData = readFile("eth2/bls_keystore.json");
    String password = "somepassword";
    String slashingProtectionData = readFile("slashing/slashingImport.json");
    final JsonObject requestBody =
        new JsonObject()
            .put("keystores", new JsonArray().add(keystoreData))
            .put("passwords", new JsonArray().add(password))
            .put("slashing_protection", slashingProtectionData);
    return requestBody.toString();
  }
}
