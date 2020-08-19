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
package tech.pegasys.eth2signer.core.service.jsonrpc;

import static tech.pegasys.eth2signer.core.service.operations.IdentifierUtils.normaliseIdentifier;

import tech.pegasys.eth2signer.core.service.jsonrpc.exceptions.InvalidParamException;
import tech.pegasys.eth2signer.core.service.jsonrpc.exceptions.SignerNotFoundException;
import tech.pegasys.eth2signer.core.service.operations.KeyIdentifiers;
import tech.pegasys.eth2signer.core.service.operations.SignerForIdentifier;
import tech.pegasys.eth2signer.core.service.operations.Upcheck;
import tech.pegasys.eth2signer.core.signing.ArtifactSignature;
import tech.pegasys.eth2signer.core.signing.ArtifactSigner;
import tech.pegasys.eth2signer.core.signing.ArtifactSignerProvider;
import tech.pegasys.eth2signer.core.signing.KeyType;
import tech.pegasys.eth2signer.core.signing.SecpArtifactSignature;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcMethod;
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcParam;
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;

@JsonRpcService
public class SigningService {

  private static final Logger LOG = LogManager.getLogger();

  private final Upcheck upcheck = new Upcheck();
  private final KeyIdentifiers ethKeyIdentifiers;
  private final ArtifactSignerProvider fcSigners;
  private final List<SignerForIdentifier<?>> signerForIdentifierList;

  public SigningService(
      final KeyIdentifiers ethKeyIdentifiers,
      final ArtifactSignerProvider fcSigners,
      final List<SignerForIdentifier<?>> signerForIdentifierList) {
    this.ethKeyIdentifiers = ethKeyIdentifiers;
    this.fcSigners = fcSigners;
    this.signerForIdentifierList = signerForIdentifierList;
  }

  @JsonRpcMethod("public_keys")
  public String[] publicKeys(@JsonRpcParam("keyType") final KeyType keyType) {
    return ethKeyIdentifiers.list(keyType).toArray(String[]::new);
  }

  @JsonRpcMethod("sign")
  public String sign(
      @JsonRpcParam("identifier") final String identifier,
      @JsonRpcParam("data") final String dataToSign) {
    return signerForIdentifierList.stream()
        .map(
            signerForIdentifier ->
                signerForIdentifier.sign(normaliseIdentifier(identifier), convertData(dataToSign)))
        .flatMap(Optional::stream)
        .findFirst()
        .orElseThrow(
            () ->
                new SignerNotFoundException(
                    "Signer not found for identifier", new String[] {identifier}));
  }

  private Bytes convertData(final String dataToSign) {
    final Bytes data;
    try {
      data = SignerForIdentifier.toBytes(dataToSign);
    } catch (final IllegalArgumentException e) {
      // LOG it as this exception will be handled by JsonRPC Server (not vertx handler)
      LOG.error("Unable to convert data [{}] to bytes", dataToSign, e);
      throw new InvalidParamException(e.getMessage());
    }
    return data;
  }

  @JsonRpcMethod
  public String upcheck() {
    return upcheck.status();
  }

  @JsonRpcMethod("Filecoin.WalletList")
  public List<String> filecoinWalletList() {
    return fcSigners.availableIdentifiers().parallelStream().collect(Collectors.toList());
  }

  @JsonRpcMethod("Filecoin.WalletSign")
  public Bytes filecoinWalletSign(final String filecoinAddress, final String dataToSign) {
    // convert dataToSign from Base32 to Bytes
    final Optional<ArtifactSigner> signer = fcSigners.getSigner(filecoinAddress);

    final ArtifactSignature signature;
    if (signer.isPresent()) {
      final Bytes bytesToSign = Bytes.fromBase64String(dataToSign);
      signature = signer.get().sign(bytesToSign);
    } else {
      throw new IllegalArgumentException("No such signer");
    }

    if (signature.getType() == KeyType.SECP256K1) {
      final SecpArtifactSignature secpSig = (SecpArtifactSignature) signature;
      return Bytes.concatenate(
          Bytes.wrap(secpSig.getSignatureData().getR().toByteArray()),
          Bytes.wrap(secpSig.getSignatureData().getS().toByteArray()),
          Bytes.wrap(secpSig.getSignatureData().getV().toByteArray()));
    } else {
      throw new UnsupportedOperationException("Cannot perform BLS Signing ... yet");
    }
  }
}
