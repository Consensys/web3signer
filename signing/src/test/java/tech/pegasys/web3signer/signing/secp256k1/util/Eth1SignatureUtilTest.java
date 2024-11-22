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
package tech.pegasys.web3signer.signing.secp256k1.util;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.web3signer.signing.secp256k1.EthPublicKeyUtils;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;

import org.apache.tuweni.bytes.Bytes;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERSequence;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;

public class Eth1SignatureUtilTest {
  public static final byte[] DATA_TO_SIGN = Hash.sha3("hello".getBytes(StandardCharsets.UTF_8));

  @Test
  void signatureIsDerivedFromDerEncoded() throws Exception {
    final KeyPair keyPair = EthPublicKeyUtils.generateK256KeyPair();
    final ECKeyPair web3jECKeyPair = ECKeyPair.create(keyPair);

    // sign using web3j (which uses BouncyCastle ECDSA Signer)
    final ECDSASignature web3jSig = web3jECKeyPair.sign(DATA_TO_SIGN);

    // convert web3j's P1363 to ANS1/DER Encoded signature
    final ASN1EncodableVector v = new ASN1EncodableVector();
    v.add(new ASN1Integer(web3jSig.r));
    v.add(new ASN1Integer(web3jSig.s));
    final byte[] derEncodedSignedData = new DERSequence(v).getEncoded();

    // verify our logic
    final tech.pegasys.web3signer.signing.secp256k1.Signature signature =
        Eth1SignatureUtil.deriveSignatureFromDerEncoded(
            DATA_TO_SIGN, (ECPublicKey) keyPair.getPublic(), derEncodedSignedData);

    assertThat(signature.getR()).isEqualTo(web3jSig.r);
    assertThat(signature.getS()).isEqualTo(web3jSig.s);
  }

  @Test
  void signatureIsDerivedFromP1363Encoded() throws Exception {
    final KeyPair keyPair = EthPublicKeyUtils.generateK256KeyPair();
    final ECKeyPair web3jECKeyPair = ECKeyPair.create(keyPair);
    // sign using web3j (which uses BouncyCastle ECDSASigner)
    final ECDSASignature web3jSig = web3jECKeyPair.sign(DATA_TO_SIGN);

    // concatenate r || s to create byte[]
    final Bytes rBytes = Bytes.of(web3jSig.r.toByteArray()).trimLeadingZeros();
    final Bytes sBytes = Bytes.of(web3jSig.s.toByteArray()).trimLeadingZeros();
    final byte[] p1363Signature = Bytes.concatenate(rBytes, sBytes).toArray();

    // verify our logic
    final tech.pegasys.web3signer.signing.secp256k1.Signature signature =
        Eth1SignatureUtil.deriveSignatureFromP1363Encoded(
            DATA_TO_SIGN, (ECPublicKey) keyPair.getPublic(), p1363Signature);

    assertThat(signature.getR()).isEqualTo(web3jSig.r);
    assertThat(signature.getS()).isEqualTo(web3jSig.s);
  }
}
