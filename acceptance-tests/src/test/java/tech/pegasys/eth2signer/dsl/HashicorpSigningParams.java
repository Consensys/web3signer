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
package tech.pegasys.eth2signer.dsl;

import tech.pegasys.eth2signer.core.signing.Curve;
import tech.pegasys.signers.hashicorp.dsl.HashicorpNode;
import tech.pegasys.signers.hashicorp.dsl.certificates.SelfSignedCertificate;

import java.util.Optional;

public class HashicorpSigningParams {

  private final HashicorpNode hashicorpNode;
  private final String secretPath;
  private final String secretName;
  private final Curve curve;

  public HashicorpSigningParams(
      final HashicorpNode hashicorpNode,
      final String secretPath,
      final String secretName,
      final Curve curve) {
    this.hashicorpNode = hashicorpNode;
    this.secretPath = secretPath;
    this.secretName = secretName;
    this.curve = curve;
  }

  public int getPort() {
    return hashicorpNode.getPort();
  }

  public String getHost() {
    return hashicorpNode.getHost();
  }

  public String getVaultToken() {
    return hashicorpNode.getVaultToken();
  }

  public Optional<SelfSignedCertificate> getServerCertificate() {
    return hashicorpNode.getServerCertificate();
  }

  public String getSecretHttpPath() {
    return hashicorpNode.getHttpApiPathForSecret(secretPath);
  }

  public String getSecretName() {
    return secretName;
  }

  public Curve getCurve() {
    return curve;
  }

  public void shutdown() {
    hashicorpNode.shutdown();
  }
}
