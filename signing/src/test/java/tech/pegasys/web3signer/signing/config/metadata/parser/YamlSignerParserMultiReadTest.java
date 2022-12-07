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
package tech.pegasys.web3signer.signing.config.metadata.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.lenient;

import tech.pegasys.web3signer.BLSTestUtil;
import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.KeyType;
import tech.pegasys.web3signer.signing.config.metadata.BlsArtifactSignerFactory;
import tech.pegasys.web3signer.signing.config.metadata.Secp256k1ArtifactSignerFactory;
import tech.pegasys.web3signer.signing.config.metadata.SigningMetadataException;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class YamlSignerParserMultiReadTest {

  @Mock private BlsArtifactSignerFactory blsArtifactSignerFactory;
  @Mock private Secp256k1ArtifactSignerFactory secp256k1ArtifactSignerFactory;

  private YamlSignerParser signerParser;

  @BeforeEach
  public void setup() {
    Mockito.reset();
    lenient().when(blsArtifactSignerFactory.getKeyType()).thenReturn(KeyType.BLS);
    lenient().when(secp256k1ArtifactSignerFactory.getKeyType()).thenReturn(KeyType.SECP256K1);

    signerParser =
        new YamlSignerParser(List.of(blsArtifactSignerFactory, secp256k1ArtifactSignerFactory));
  }

  @Test
  void readMultiDoc() {
    final String prvKey1 = BLSTestUtil.randomKeyPair(1).getSecretKey().toBytes().toHexString();
    final String prvKey2 = BLSTestUtil.randomKeyPair(2).getSecretKey().toBytes().toHexString();

    final String multiYaml =
        String.format(
            "---\n"
                + "privateKey: \"%s\"\n"
                + "type: \"file-raw\"\n"
                + "---\n"
                + "privateKey: \"%s\"\n"
                + "type: \"file-raw\"",
            prvKey1, prvKey2);

    final List<ArtifactSigner> signingMetadataList = signerParser.parse(multiYaml);

    assertThat(signingMetadataList).hasSize(2);
  }

  @Test
  void invalidMultiDocThrowsException() {
    final String prvKey1 = BLSTestUtil.randomKeyPair(1).getSecretKey().toBytes().toHexString();
    final String prvKey2 = BLSTestUtil.randomKeyPair(2).getSecretKey().toBytes().toHexString();

    // missing type:
    final String multiYaml =
        String.format(
            "---\n"
                + "privateKey: \"%s\"\n"
                + "type: \"file-raw\"\n"
                + "---\n"
                + "privateKey: \"%s\"",
            prvKey1, prvKey2);

    assertThatExceptionOfType(SigningMetadataException.class)
        .isThrownBy(() -> signerParser.parse(multiYaml))
        .withMessage("Invalid signing metadata file format");
  }
}
