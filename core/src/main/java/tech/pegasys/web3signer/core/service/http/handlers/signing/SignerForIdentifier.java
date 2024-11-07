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
package tech.pegasys.web3signer.core.service.http.handlers.signing;

import tech.pegasys.web3signer.signing.ArtifactSignature;
import tech.pegasys.web3signer.signing.ArtifactSignerProvider;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;

/**
 * This class wraps the {@link ArtifactSignerProvider} and provides a way to check if a signer is
 * available for a given identifier and to sign a message.
 */
public class SignerForIdentifier {
  private static final Logger LOG = LogManager.getLogger();
  private final ArtifactSignerProvider signerProvider;

  public SignerForIdentifier(final ArtifactSignerProvider signerProvider) {
    this.signerProvider = signerProvider;
  }

  /**
   * Sign data for given identifier
   *
   * @param identifier The identifier for which to sign data.
   * @param data String in hex format which is signed
   * @return Optional String of signature (in hex format). Empty if no signer available for given
   *     identifier
   * @throws IllegalArgumentException if data is invalid i.e. not a valid hex string, null or empty.
   */
  public Optional<String> sign(final String identifier, final Bytes data) {
    return signerProvider.getSigner(identifier).map(signer -> signer.sign(data).asHex());
  }

  public Optional<ArtifactSignature> signAndGetArtifactSignature(
      final String identifier, final Bytes data) {
    return signerProvider.getSigner(identifier).map(signer -> signer.sign(data));
  }

  /**
   * Converts hex string to bytes
   *
   * @param data hex string
   * @return Bytes
   * @throws IllegalArgumentException if data is invalid i.e. not a valid hex string, null or empty
   */
  public static Bytes toBytes(final String data) {
    final Bytes dataToSign;
    try {
      if (StringUtils.isBlank(data)) {
        throw new IllegalArgumentException("Blank data");
      }
      dataToSign = Bytes.fromHexString(data);
    } catch (final IllegalArgumentException e) {
      LOG.debug("Invalid hex string {}", data, e);
      throw e;
    }
    return dataToSign;
  }

  /**
   * Checks whether a signer for the passed identifier is present
   *
   * @param identifier The identifier for which to sign data.
   * @return true is there's a signer for the corresponding identifier, otherwise false
   */
  public boolean isSignerAvailable(final String identifier) {
    return signerProvider.getSigner(identifier).isPresent();
  }
}
