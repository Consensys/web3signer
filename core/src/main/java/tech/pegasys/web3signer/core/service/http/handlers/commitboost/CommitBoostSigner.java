/*
 * Copyright 2024 ConsenSys AG.
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
package tech.pegasys.web3signer.core.service.http.handlers.commitboost;

import tech.pegasys.web3signer.core.service.http.handlers.commitboost.json.SignRequestType;
import tech.pegasys.web3signer.signing.ArtifactSignature;
import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.signing.BlsArtifactSignature;
import tech.pegasys.web3signer.signing.KeyType;
import tech.pegasys.web3signer.signing.SecpArtifactSignature;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.tuweni.bytes.Bytes32;

/**
 * This class wraps the {@link ArtifactSignerProvider} and provides a way to check if a signer is
 * available, consensus or proxy,and to sign a message.
 */
public class CommitBoostSigner {
  private final ArtifactSignerProvider artifactSignerProvider;

  public CommitBoostSigner(final ArtifactSignerProvider artifactSignerProvider) {
    this.artifactSignerProvider = artifactSignerProvider;
  }

  public boolean isSignerAvailable(final String identifier, final SignRequestType type) {
    return switch (type) {
      case CONSENSUS -> artifactSignerProvider.availableIdentifiers().contains(identifier);
      case PROXY_BLS -> {
        final Map<KeyType, Set<String>> proxyIdentifiers =
            artifactSignerProvider.getProxyIdentifiers(identifier);
        yield proxyIdentifiers.containsKey(KeyType.BLS)
            && proxyIdentifiers.get(KeyType.BLS).contains(identifier);
      }
      case PROXY_ECDSA -> {
        final Map<KeyType, Set<String>> proxyIdentifiers =
            artifactSignerProvider.getProxyIdentifiers(identifier);
        yield proxyIdentifiers.containsKey(KeyType.SECP256K1)
            && proxyIdentifiers.get(KeyType.SECP256K1).contains(identifier);
      }
    };
  }

  public Optional<String> sign(
      final String identifier, final SignRequestType type, final Bytes32 signingRoot) {
    final Optional<ArtifactSigner> optionalArtifactSigner =
        type == SignRequestType.CONSENSUS
            ? artifactSignerProvider.getSigner(identifier)
            : artifactSignerProvider.getProxySigner(identifier);

    return optionalArtifactSigner
        .map(
            signer -> {
              final ArtifactSignature artifactSignature = signer.sign(signingRoot);
              return switch (artifactSignature.getType()) {
                case BLS ->
                    Optional.of(
                        ((BlsArtifactSignature) artifactSignature).getSignatureData().toString());
                case SECP256K1 ->
                    Optional.of(
                        SecpArtifactSignature.toBytes((SecpArtifactSignature) artifactSignature)
                            .toHexString());
              };
            })
        .orElse(Optional.empty());
  }
}
