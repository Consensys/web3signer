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
package tech.pegasys.eth2signer.core.service.operations;

import tech.pegasys.eth2signer.core.signing.ArtifactSignerProvider;
import tech.pegasys.eth2signer.core.signing.BlsArtifactSigner;
import tech.pegasys.eth2signer.core.signing.Curve;
import tech.pegasys.eth2signer.core.signing.SecpArtifactSigner;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PublicKeys {

  private final ArtifactSignerProvider signerProviders;

  public PublicKeys(final ArtifactSignerProvider signerProviders) {
    this.signerProviders = signerProviders;
  }

  public List<String> list(final Curve curve) {
    final Set<String> identifier = signerProviders.availableIdentifiers();

    if (curve.equals(Curve.BLS)) {
      return identifier
          .parallelStream()
          .filter(i -> signerProviders.getSigner(Curve.BLS, i).get() instanceof BlsArtifactSigner)
          .collect(Collectors.toList());
    } else {
      return identifier
          .parallelStream()
          .filter(
              i ->
                  signerProviders.getSigner(Curve.SECP256K1, i).get() instanceof SecpArtifactSigner)
          .collect(Collectors.toList());
    }
  }
}
