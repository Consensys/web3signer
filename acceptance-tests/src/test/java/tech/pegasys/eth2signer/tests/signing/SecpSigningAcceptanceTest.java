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
package tech.pegasys.eth2signer.tests.signing;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.web3j.crypto.Sign.publicKeyFromPrivate;
import static org.web3j.crypto.Sign.signedMessageToKey;
import static tech.pegasys.signers.secp256k1.MultiKeyTomlFileUtil.createAzureTomlFileAt;

import tech.pegasys.eth2signer.dsl.HashicorpSigningParams;
import tech.pegasys.eth2signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.signers.hashicorp.dsl.HashicorpNode;

import java.io.File;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.SignatureException;

import com.google.common.io.Resources;
import io.restassured.response.Response;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariables;
import org.junit.jupiter.api.io.TempDir;
import org.web3j.crypto.Sign.SignatureData;

public class SecpSigningAcceptanceTest extends SigningAcceptanceTestBase {

  private static final Bytes DATA = Bytes.wrap("42".getBytes(UTF_8));
  private static final String PRIVATE_KEY =
      "8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63";
  public static final String PUBLIC_KEY_HEX_STRING =
      "09b02f8a5fddd222ade4ea4528faefc399623af3f736be3c44f03e2df22fb792f3931a4d9573d333ca74343305762a753388c3422a86d98b713fc91c1ea04842";

  private final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();

  @Test
  @EnabledIfEnvironmentVariables({
    @EnabledIfEnvironmentVariable(named = "AZURE_CLIENT_ID", matches = ".*"),
    @EnabledIfEnvironmentVariable(named = "AZURE_CLIENT_SECRET", matches = ".*"),
    @EnabledIfEnvironmentVariable(named = "AZURE_KEY_VAULT_NAME", matches = ".*")
  })
  public void signDataWithKeyInAzure(@TempDir Path tomlDirectory) {
    final String clientId = System.getenv("AZURE_CLIENT_ID");
    final String clientSecret = System.getenv("AZURE_CLIENT_SECRET");
    final String keyVaultName = System.getenv("AZURE_KEY_VAULT_NAME");
    createAzureTomlFileAt(
        tomlDirectory.resolve("arbitrary_prefix" + PUBLIC_KEY_HEX_STRING + ".toml"),
        clientId,
        clientSecret,
        keyVaultName);

    signAndVerifySignature();
  }

  @Test
  public void signDataWithFileBasedKey(@TempDir Path keyConfigDirectory) throws URISyntaxException {
    final String keyPath =
        new File(Resources.getResource("secp256k1/wallet.json").toURI()).getAbsolutePath();

    // final Path passwordPath = keyConfigDirectory.resolve("password");
    // Files.write(passwordPath, "pass".getBytes(UTF_8));

    metadataFileHelpers.createKeyStoreYamlFileAt(
        keyConfigDirectory.resolve(PUBLIC_KEY_HEX_STRING + ".yaml"), Path.of(keyPath), "pass");

    signAndVerifySignature();
  }

  @Test
  public void signDataWithKeyFromHashicorp(@TempDir Path tomlDirectory) {
    final HashicorpNode hashicorpNode = HashicorpNode.createAndStartHashicorp(true);
    try {
      final String secretPath = "acceptanceTestSecretPath";
      final String secretName = "secretName";
      hashicorpNode.addSecretsToVault(singletonMap(secretName, PRIVATE_KEY), secretPath);

      final HashicorpSigningParams hashicorpSigningParams =
          new HashicorpSigningParams(hashicorpNode, secretPath, secretName);

      metadataFileHelpers.createHashicorpYamlFileAt(
          tomlDirectory.resolve(PUBLIC_KEY_HEX_STRING + ".yaml"), hashicorpSigningParams);

      signAndVerifySignature();
    } finally {
      hashicorpNode.shutdown();
    }
  }

  private void signAndVerifySignature() {
    setupSigner();

    // openapi
    final Response response = sign(PUBLIC_KEY_HEX_STRING, DATA);
    final Bytes signature = verifyAndGetSignatureResponse(response);
    verifySignature(signature);

    // jsonrpc
    final Response jsonResponse = callJsonRpcSign(PUBLIC_KEY_HEX_STRING, DATA.toHexString());
    final Bytes jsonResponseSignature = verifyAndGetJsonRpcSignatureResponse(jsonResponse);
    verifySignature(jsonResponseSignature);
  }

  void verifySignature(final Bytes signature) {
    final BigInteger privateKey = new BigInteger(1, Bytes.fromHexString(PRIVATE_KEY).toArray());
    final BigInteger expectedPublicKey = publicKeyFromPrivate(privateKey);

    final byte[] r = signature.slice(0, 32).toArray();
    final byte[] s = signature.slice(32, 32).toArray();
    final byte[] v = signature.slice(64).toArray();
    final BigInteger messagePublicKey = recoverPublicKey(new SignatureData(v, r, s));
    assertThat(messagePublicKey).isEqualTo(expectedPublicKey);
  }

  private BigInteger recoverPublicKey(final SignatureData signature) {
    try {
      return signedMessageToKey(DATA.toArray(), signature);
    } catch (final SignatureException e) {
      throw new IllegalStateException("signature cannot be recovered", e);
    }
  }
}
