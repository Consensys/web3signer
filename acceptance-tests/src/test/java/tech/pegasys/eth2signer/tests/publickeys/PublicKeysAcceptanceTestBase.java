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
package tech.pegasys.eth2signer.tests.publickeys;

import tech.pegasys.eth2signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.eth2signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.eth2signer.tests.AcceptanceTestBase;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.bls.BLSSecretKey;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.io.TempDir;

public class PublicKeysAcceptanceTestBase extends AcceptanceTestBase {
  static final String SIGNER_PUBLIC_KEYS_PATH = "/signer/publicKeys";

  private static final String PRIVATE_KEY_1 =
      "3ee2224386c82ffea477e2adf28a2929f5c349165a4196158c7f3a2ecca40f35";
  private static final String PRIVATE_KEY_2 =
      "32ae313afff2daa2ef7005a7f834bdf291855608fe82c24d30be6ac2017093a8";

  private static final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();

  @TempDir Path testDirectory;

  protected String[] privateKeys() {
    return new String[] {PRIVATE_KEY_1, PRIVATE_KEY_2};
  }

  protected String[] createKeys(boolean isValid, final String... privateKeys) {
    return Stream.of(privateKeys)
        .map(
            privateKey -> {
              final BLSKeyPair keyPair =
                  new BLSKeyPair(BLSSecretKey.fromBytes(Bytes.fromHexString(privateKey)));
              final Path keyConfigFile = configFileName(keyPair.getPublicKey());
              if (isValid) {
                metadataFileHelpers.createUnencryptedYamlFileAt(keyConfigFile, privateKey);
              } else {
                try {
                  Files.createFile(keyConfigFile);
                } catch (final IOException e) {
                  throw new UncheckedIOException(e);
                }
              }
              return keyPair.getPublicKey().toString();
            })
        .toArray(String[]::new);
  }

  private Path configFileName(final BLSPublicKey publicKey) {
    final String configFilename2 = publicKey.toString().substring(2);
    return testDirectory.resolve(configFilename2 + ".yaml");
  }

  protected void initAndStartSigner() {
    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withKeyStoreDirectory(testDirectory);
    startSigner(builder.build());
  }
}
