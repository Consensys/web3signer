/*
 * Copyright 2025 ConsenSys AG.
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
package tech.pegasys.web3signer.signing;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;

/**
 * In-memory only validator manager that skips writing keystores to disk. Validators imported via
 * this manager will only exist in memory and will be lost when Web3Signer restarts.
 */
public class InMemoryValidatorManager implements ValidatorManager {

  private static final Logger LOG = LogManager.getLogger();
  private final ArtifactSignerProvider signerProvider;
  private final ObjectMapper objectMapper;

  public InMemoryValidatorManager(
      final ArtifactSignerProvider signerProvider, final ObjectMapper objectMapper) {
    this.signerProvider = signerProvider;
    this.objectMapper = objectMapper;
  }

  @Override
  public ObjectMapper getJsonMapper() {
    return objectMapper;
  }

  @Override
  public void deleteValidator(final Bytes publicKey) {
    try {
      signerProvider.removeSigner(publicKey.toHexString()).get();
      LOG.info(
          "Validator removed from memory (no files deleted): {}",
          publicKey.toHexString().toLowerCase(Locale.ROOT));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Unable to delete validator from memory", e);
    } catch (ExecutionException e) {
      throw new IllegalStateException("Unable to delete validator from memory", e);
    }
  }

  @Override
  public void addValidator(final BlsArtifactSigner signer) {
    try {
      signerProvider.addSigner(signer).get();
      LOG.info(
          "Validator added to memory only (no files written): {}",
          signer.getIdentifier().toLowerCase(Locale.ROOT));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Unable to add validator to memory", e);
    } catch (ExecutionException e) {
      throw new IllegalStateException("Unable to add validator to memory", e);
    }
  }

  @Override
  public void postAddValidator(
      final BlsArtifactSigner signer, final String jsonKeystoreData, final String password) {
    // do nothing
  }
}
