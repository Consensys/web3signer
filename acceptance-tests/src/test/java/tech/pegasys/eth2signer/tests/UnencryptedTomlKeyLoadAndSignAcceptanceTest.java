/*
 * Copyright 2020 ConsenSys AG.
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
package tech.pegasys.eth2signer.tests;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.pegasys.eth2signer.crypto.KeyPair;
import tech.pegasys.eth2signer.crypto.PublicKey;
import tech.pegasys.eth2signer.crypto.SecretKey;
import tech.pegasys.eth2signer.dsl.signer.Signer;
import tech.pegasys.eth2signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.eth2signer.dsl.utils.TomlHelpers;

public class UnencryptedTomlKeyLoadAndSignAcceptanceTest {

  private final TomlHelpers tomlHelpers = new TomlHelpers();

  private final String privateKeyString =
      "000000000000000000000000000000003ee2224386c82ffea477e2adf28a2929f5c349165a4196158c7f3a2ecca40f35";
  private final SecretKey key = SecretKey.fromBytes(Bytes.fromHexString(privateKeyString));
  private final KeyPair keyPair = new KeyPair(key);

  @Test
  public void signDataWithKeyLoadedFromUnencryptedFile(@TempDir final Path testDirectory)
      throws Exception {
    final String tomlKeyFilename = keyPair.publicKey().toString().substring(2);
    final Path keyConfigFile = testDirectory.resolve(tomlKeyFilename + ".toml");
    tomlHelpers.createUnencryptedTomlFileAt(keyConfigFile, privateKeyString);

    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withKeyStoreDirectory(testDirectory);
    final Signer signer = new Signer(builder.build());
    try {
      signer.start();

      final String expectedSignature =
          "0x810A4B8E878A1AD0B30F3EAE7ED35E17450E82FDDE6AAA8B500EB5A59A78E3B07684F72B014F92EBED64BD7FFEF680A00A63B84CA92A6299265A0C2339547F0432C3DEE612665C4FEE5D4D93B42D84F2E963700842F60DAE7E5B641F5BB01E64";

      final Bytes message = Bytes.wrap("Hello, world!".getBytes(UTF_8));
      final Bytes domain = Bytes.ofUnsignedLong(42L);
      final String responseText = signer.signData(keyPair.publicKey(), message, domain);

      assertThat(responseText.toLowerCase()).isEqualTo(expectedSignature.toLowerCase());
    } finally {
      signer.shutdown();
    }
  }

  @Test
  public void receiveA404IfRequestedKeyDoesNotExist() {
    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    final Signer signer = new Signer(builder.build());
    signer.start();

    try {
      final Bytes message = Bytes.wrap("Hello, world!".getBytes(UTF_8));
      final Bytes domain = Bytes.ofUnsignedLong(42L);
      assertThatThrownBy(() -> signer.signData(PublicKey.random(), message, domain))
          .isInstanceOf(ExecutionException.class);
    } finally {
      signer.shutdown();
    }
  }
}
