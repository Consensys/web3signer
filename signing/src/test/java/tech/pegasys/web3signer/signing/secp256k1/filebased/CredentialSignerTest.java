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
package tech.pegasys.web3signer.signing.secp256k1.filebased;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;

class CredentialSignerTest {

  @Test
  void needToHashFlagAffectsProducedSignature() {
    final Credentials credentials = Credentials.create(ECKeyPair.create(BigInteger.ONE));

    final CredentialSigner hashingSigner = new CredentialSigner(credentials);
    final CredentialSigner nonHashingSigner = new CredentialSigner(credentials, false);

    final byte[] data = "Hello World".getBytes(UTF_8);

    assertThat(hashingSigner.getPublicKey().getEncoded())
        .isEqualTo(nonHashingSigner.getPublicKey().getEncoded());
    assertThat(hashingSigner.sign(data))
        .usingRecursiveComparison()
        .isEqualTo(nonHashingSigner.sign(Hash.sha3(data)));
  }
}
