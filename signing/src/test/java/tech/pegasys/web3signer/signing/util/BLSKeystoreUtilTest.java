/*
 * Copyright 2026 ConsenSys AG.
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
package tech.pegasys.web3signer.signing.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static tech.pegasys.web3signer.bls.keystore.model.Pbkdf2PseudoRandomFunction.HMAC_SHA256;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.web3signer.BLSTestUtil;
import tech.pegasys.web3signer.bls.keystore.KeyStore;
import tech.pegasys.web3signer.bls.keystore.KeyStoreValidationException;
import tech.pegasys.web3signer.bls.keystore.model.Cipher;
import tech.pegasys.web3signer.bls.keystore.model.CipherFunction;
import tech.pegasys.web3signer.bls.keystore.model.KeyStoreData;
import tech.pegasys.web3signer.bls.keystore.model.Pbkdf2Param;
import tech.pegasys.web3signer.signing.BlsArtifactSigner;
import tech.pegasys.web3signer.signing.config.metadata.parser.SigningMetadataModule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;

class BLSKeystoreUtilTest {
  private static final Bytes SALT =
      Bytes.fromHexString("0x9ac471d9d421bc06d9aefe2b46cf96d11829c51e36ed0b116132be57a9f8c22b");
  private static final Bytes IV = Bytes.fromHexString("0xcca2c67ec95a1dd13edd986fea372789");
  private static final BLSKeyPair BLS_KEY_PAIR = BLSTestUtil.randomKeyPair(1);
  private static final BLSKeyPair OTHER_KEY_PAIR = BLSTestUtil.randomKeyPair(2);
  private static final ObjectMapper OBJECT_MAPPER =
      JsonMapper.builder().addModule(new SigningMetadataModule()).build();

  @Test
  void decryptKeystoreThrowsExceptionForInvalidKeystoreJson() {
    assertThatThrownBy(
            () -> BLSKeystoreUtil.decryptKeystore(OBJECT_MAPPER, "invalid json", "password"))
        .isInstanceOf(KeyStoreValidationException.class)
        .hasMessageContaining("Failed to parse keystore JSON");
  }

  @Test
  void decryptKeystoreThrowsExceptionForIncorrectPassword() throws JsonProcessingException {
    final String keystoreJson = createKeystoreString(BLS_KEY_PAIR, "password");

    assertThatThrownBy(
            () -> BLSKeystoreUtil.decryptKeystore(OBJECT_MAPPER, keystoreJson, "wrongpassword"))
        .isInstanceOf(KeyStoreValidationException.class);
  }

  @Test
  void decryptKeystoreSucceeds() throws JsonProcessingException {
    final String keystoreJson = createKeystoreString(BLS_KEY_PAIR, "password");

    final BlsArtifactSigner signer =
        BLSKeystoreUtil.decryptKeystore(OBJECT_MAPPER, keystoreJson, "password");

    assertThat(signer.getIdentifier()).isEqualTo(BLS_KEY_PAIR.getPublicKey().toString());
  }

  @Test
  void decryptKeystoreThrowsExceptionForPubkeyMismatch() throws JsonProcessingException {
    // Build a keystore for BLS_KEY_PAIR but tamper the pubkey field to OTHER_KEY_PAIR's pubkey
    final String keystoreJson = createKeystoreString(BLS_KEY_PAIR, "password");
    final ObjectNode node = (ObjectNode) OBJECT_MAPPER.readTree(keystoreJson);
    node.put("pubkey", OTHER_KEY_PAIR.getPublicKey().toBytesCompressed().toUnprefixedHexString());
    final String tamperedJson = OBJECT_MAPPER.writeValueAsString(node);

    assertThatThrownBy(
            () -> BLSKeystoreUtil.decryptKeystore(OBJECT_MAPPER, tamperedJson, "password"))
        .isInstanceOf(KeyStoreValidationException.class)
        .hasMessageContaining("Keystore pubkey does not match decrypted key");
  }

  private String createKeystoreString(final BLSKeyPair keyPair, final String password)
      throws JsonProcessingException {
    final Cipher cipher = new Cipher(CipherFunction.AES_128_CTR, IV);
    final Pbkdf2Param pbkdf2Param = new Pbkdf2Param(32, 262144, HMAC_SHA256, SALT);
    final KeyStoreData keyStoreData =
        KeyStore.encrypt(
            keyPair.getSecretKey().toBytes(),
            keyPair.getPublicKey().toBytesCompressed(),
            password,
            "",
            pbkdf2Param,
            cipher);

    return OBJECT_MAPPER.writeValueAsString(keyStoreData);
  }
}
