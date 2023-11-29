/*
 * Copyright 2022 ConsenSys AG.
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
package tech.pegasys.web3signer.signing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static tech.pegasys.teku.bls.keystore.model.Pbkdf2PseudoRandomFunction.HMAC_SHA256;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.keystore.KeyStore;
import tech.pegasys.teku.bls.keystore.model.Cipher;
import tech.pegasys.teku.bls.keystore.model.CipherFunction;
import tech.pegasys.teku.bls.keystore.model.KeyStoreData;
import tech.pegasys.teku.bls.keystore.model.Pbkdf2Param;
import tech.pegasys.web3signer.BLSTestUtil;
import tech.pegasys.web3signer.signing.config.metadata.parser.SigningMetadataModule;
import tech.pegasys.web3signer.signing.config.metadata.parser.YamlMapperFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileValidatorManagerTest {
  private static final Bytes SALT =
      Bytes.fromHexString("0x9ac471d9d421bc06d9aefe2b46cf96d11829c51e36ed0b116132be57a9f8c22b");
  private static final Bytes IV = Bytes.fromHexString("0xcca2c67ec95a1dd13edd986fea372789");
  private static final BLSKeyPair BLS_KEY_PAIR = BLSTestUtil.randomKeyPair(1);
  private static final ObjectMapper OBJECT_MAPPER =
      JsonMapper.builder().addModule(new SigningMetadataModule()).build();

  @Mock private ArtifactSignerProvider artifactSignerProvider;
  @Mock private KeystoreFileManager keystoreFileManager;

  private static final YAMLMapper YAML_MAPPER = YamlMapperFactory.createYamlMapper();

  @Test
  @SuppressWarnings("unchecked")
  void addsValidator() throws IOException, ExecutionException, InterruptedException {
    final Future<Void> futureAddSigner = Mockito.mock(Future.class);
    when(artifactSignerProvider.addSigner(any())).thenReturn(futureAddSigner);

    final String keystoreJson = createKeystoreString();

    final FileValidatorManager fileValidatorManager =
        new FileValidatorManager(artifactSignerProvider, keystoreFileManager, YAML_MAPPER);
    fileValidatorManager.addValidator(
        BLS_KEY_PAIR.getPublicKey().toBytesCompressed(), keystoreJson, "password");

    verify(keystoreFileManager)
        .createKeystoreFiles(
            eq(BLS_KEY_PAIR.getPublicKey().toString()), eq(keystoreJson), eq("password"));
    verify(futureAddSigner).get();
  }

  @Test
  @SuppressWarnings("unchecked")
  void deletesValidator() throws ExecutionException, InterruptedException {
    final Future<Void> futureDeleteSigner = Mockito.mock(Future.class);
    when(artifactSignerProvider.removeSigner(any())).thenReturn(futureDeleteSigner);

    final FileValidatorManager fileValidatorManager =
        new FileValidatorManager(artifactSignerProvider, keystoreFileManager, YAML_MAPPER);
    fileValidatorManager.deleteValidator(BLS_KEY_PAIR.getPublicKey().toBytesCompressed());

    verify(artifactSignerProvider).removeSigner(eq(BLS_KEY_PAIR.getPublicKey().toString()));
    verify(futureDeleteSigner).get();
  }

  private String createKeystoreString() throws JsonProcessingException {
    final Cipher cipher = new Cipher(CipherFunction.AES_128_CTR, IV);
    final Pbkdf2Param pbkdf2Param = new Pbkdf2Param(32, 262144, HMAC_SHA256, SALT);
    final KeyStoreData keyStoreData =
        KeyStore.encrypt(
            BLS_KEY_PAIR.getSecretKey().toBytes(),
            BLS_KEY_PAIR.getPublicKey().toBytesCompressed(),
            "password",
            "",
            pbkdf2Param,
            cipher);

    return OBJECT_MAPPER.writeValueAsString(keyStoreData);
  }
}
