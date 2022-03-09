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
package tech.pegasys.web3signer.core.service.http.handlers.keymanager.delete;

import tech.pegasys.web3signer.core.signing.ArtifactSigner;
import tech.pegasys.web3signer.core.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.core.signing.BlsArtifactSigner;
import tech.pegasys.web3signer.core.util.IdentifierUtils;
import tech.pegasys.web3signer.slashingprotection.SlashingProtection;
import tech.pegasys.web3signer.slashingprotection.interchange.IncrementalExporter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;

public class DeleteKeystoresProcessor {

  private static final Logger LOG = LogManager.getLogger();

  private final KeystoreFileManager keystoreFileManager;
  private final Optional<SlashingProtection> slashingProtection;
  private final ArtifactSignerProvider signerProvider;

  public DeleteKeystoresProcessor(
      final KeystoreFileManager keystoreFileManager,
      final Optional<SlashingProtection> slashingProtection,
      final ArtifactSignerProvider signerProvider) {
    this.keystoreFileManager = keystoreFileManager;
    this.slashingProtection = slashingProtection;
    this.signerProvider = signerProvider;
  }

  public DeleteKeystoresResponse process(final DeleteKeystoresRequestBody requestBody) {
    final List<DeleteKeystoreResult> results = new ArrayList<>();
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    try (final IncrementalExporter incrementalExporter = createIncrementalExporter(outputStream)) {
      // normalize incoming keys to delete
      final List<String> pubkeysToDelete =
          requestBody.getPubkeys().stream()
              .map(IdentifierUtils::normaliseIdentifier)
              .collect(Collectors.toList());

      for (String pubkey : pubkeysToDelete) {
        results.add(processKeyToDelete(pubkey, incrementalExporter));
      }

      try {
        incrementalExporter.finalise();
      } catch (IOException ioException) {
        LOG.error("Failed to export slashing data", ioException);
        setAllResultsToError(results, ioException);
      }
    } catch (Exception e) {
      // Any unhandled error we want to bubble up so that we return an internal error response
      throw new RuntimeException("Error deleting keystores", e);
    }

    final String slashingProtectionExport = outputStream.toString(StandardCharsets.UTF_8);
    return new DeleteKeystoresResponse(results, slashingProtectionExport);
  }

  private void setAllResultsToError(
      final List<DeleteKeystoreResult> results, final IOException ioException) {
    final List<DeleteKeystoreResult> errorResults =
        results.stream()
            .map(
                result ->
                    new DeleteKeystoreResult(
                        DeleteKeystoreStatus.ERROR,
                        "Error exporting slashing data: " + ioException.getMessage()))
            .collect(Collectors.toList());
    results.clear();
    results.addAll(errorResults);
  }

  private IncrementalExporter createIncrementalExporter(final ByteArrayOutputStream outputStream) {
    return slashingProtection.isPresent()
        ? slashingProtection.get().createIncrementalExporter(outputStream)
        // Using no-op exporter instead of returning an optional so can use try with for closing
        : new IncrementalExporter() {
          @Override
          public void export(final String publicKey) {}

          @Override
          public void finalise() {}

          @Override
          public void close() {}
        };
  }

  private DeleteKeystoreResult processKeyToDelete(
      String pubkey, final IncrementalExporter incrementalExporter) {
    try {
      final Optional<ArtifactSigner> signer = signerProvider.getSigner(pubkey);

      // check that key is active
      if (signer.isEmpty()) {
        final boolean slashingProtectionDataExistsForPubKey =
            slashingProtection
                .map(sp -> sp.hasSlashingProtectionDataFor(Bytes.fromHexString(pubkey)))
                .orElse(false);

        if (slashingProtectionDataExistsForPubKey) {
          final Optional<DeleteKeystoreResult> exportError =
              exportSlashingData(pubkey, incrementalExporter);
          return exportError.orElseGet(
              () -> new DeleteKeystoreResult(DeleteKeystoreStatus.NOT_ACTIVE, ""));
        } else {
          return new DeleteKeystoreResult(DeleteKeystoreStatus.NOT_FOUND, "");
        }
      }

      // Check that key is read only, if so return an error status
      if (signer.get() instanceof BlsArtifactSigner
          && ((BlsArtifactSigner) signer.get()).isReadOnlyKey()) {
        return new DeleteKeystoreResult(
            DeleteKeystoreStatus.ERROR, "Unable to delete readonly key: " + pubkey);
      }

      // Remove active key from memory first, will stop any further signing with this key
      signerProvider.removeSigner(pubkey).get();
      // Then, delete the corresponding keystore file
      keystoreFileManager.deleteKeystoreFiles(pubkey);
    } catch (Exception e) {
      LOG.error("Failed to delete keystore files", e);
      return new DeleteKeystoreResult(
          DeleteKeystoreStatus.ERROR, "Error deleting keystore file: " + e.getMessage());
    }

    final Optional<DeleteKeystoreResult> exportError =
        exportSlashingData(pubkey, incrementalExporter);
    return exportError.orElseGet(() -> new DeleteKeystoreResult(DeleteKeystoreStatus.DELETED, ""));
  }

  private Optional<DeleteKeystoreResult> exportSlashingData(
      final String pubkey, final IncrementalExporter incrementalExporter) {
    try {
      incrementalExporter.export(pubkey);
      return Optional.empty();
    } catch (Exception e) {
      LOG.error("Failed to export slashing data for public key {}", pubkey, e);
      return Optional.of(
          new DeleteKeystoreResult(
              DeleteKeystoreStatus.ERROR, "Error exporting slashing data: " + e.getMessage()));
    }
  }
}
