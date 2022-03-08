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
    // normalize incoming keys to delete
    final List<String> pubkeysToDelete =
        requestBody.getPubkeys().stream()
            .map(IdentifierUtils::normaliseIdentifier)
            .collect(Collectors.toList());

    final List<DeleteKeystoreResult> results = new ArrayList<>();
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    try (final IncrementalExporter incrementalExporter = createIncrementalExporter(outputStream)) {
      for (String pubkey : pubkeysToDelete) {
        results.add(processKeyToDelete(pubkey, incrementalExporter));
      }
      incrementalExporter.finalise();
    } catch (Exception e) {
      LOG.error("Failed to export slashing data", e);
      // if export fails - set all results to error
      final List<DeleteKeystoreResult> errorResults =
          results.stream()
              .map(
                  result ->
                      new DeleteKeystoreResult(
                          DeleteKeystoreStatus.ERROR,
                          "Error exporting slashing data: " + e.getMessage()))
              .collect(Collectors.toList());
      results.clear();
      results.addAll(errorResults);
    }

    final String slashingProtectionExport = outputStream.toString(StandardCharsets.UTF_8);
    return new DeleteKeystoresResponse(results, slashingProtectionExport);
  }

  private IncrementalExporter createIncrementalExporter(final ByteArrayOutputStream outputStream) {
    return slashingProtection.isPresent()
        ? slashingProtection.get().createIncrementalExporter(outputStream)
        // return no-op exporter if no slashing protection
        : new IncrementalExporter() {
          @Override
          public void addPublicKey(final String publicKey) {}

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
        // if not active, check if we ever had this key registered in the slashing DB
        final boolean wasRegistered =
            slashingProtection
                .map(protection -> protection.isRegisteredValidator(Bytes.fromHexString(pubkey)))
                .orElse(false);

        // if it was registered previously, return not_active and add to list of keys to export,
        // otherwise not_found
        if (wasRegistered) {
          final Optional<DeleteKeystoreResult> exportSlashingDataError =
              exportSlashingData(pubkey, incrementalExporter);
          return exportSlashingDataError.orElseGet(
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

    final Optional<DeleteKeystoreResult> exportSlashingDataError =
        exportSlashingData(pubkey, incrementalExporter);
    return exportSlashingDataError.orElseGet(
        () -> new DeleteKeystoreResult(DeleteKeystoreStatus.DELETED, ""));
  }

  private Optional<DeleteKeystoreResult> exportSlashingData(
      final String pubkey, final IncrementalExporter incrementalExporter) {
    try {
      incrementalExporter.addPublicKey(pubkey);
      return Optional.empty();
    } catch (Exception e) {
      LOG.error("Failed to export slashing data for public key {}", pubkey, e);
      return Optional.of(
          new DeleteKeystoreResult(
              DeleteKeystoreStatus.ERROR, "Error exporting slashing data: " + e.getMessage()));
    }
  }
}
