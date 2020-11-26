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
package tech.pegasys.web3signer.slashingprotection.interchange;

import tech.pegasys.web3signer.slashingprotection.dao.LowWatermarkDao;
import tech.pegasys.web3signer.slashingprotection.dao.MetadataDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestationsDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlocksDao;
import tech.pegasys.web3signer.slashingprotection.dao.SigningWatermark;
import tech.pegasys.web3signer.slashingprotection.dao.Validator;
import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;
import tech.pegasys.web3signer.slashingprotection.interchange.model.Metadata;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes32;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

public class InterchangeV5Exporter {

  private static final Logger LOG = LogManager.getLogger();

  private static final String FORMAT_VERSION = "5";

  private final Jdbi jdbi;
  private final ValidatorsDao validatorsDao;
  private final SignedBlocksDao signedBlocksDao;
  private final SignedAttestationsDao signedAttestationsDao;
  private final MetadataDao metadataDao;
  private final LowWatermarkDao lowWatermarkDao;
  private final ObjectMapper mapper;

  public InterchangeV5Exporter(
      final Jdbi jdbi,
      final ValidatorsDao validatorsDao,
      final SignedBlocksDao signedBlocksDao,
      final SignedAttestationsDao signedAttestationsDao,
      final MetadataDao metadataDao,
      final LowWatermarkDao lowWatermarkDao,
      final ObjectMapper mapper) {
    this.jdbi = jdbi;
    this.validatorsDao = validatorsDao;
    this.signedBlocksDao = signedBlocksDao;
    this.signedAttestationsDao = signedAttestationsDao;
    this.metadataDao = metadataDao;
    this.lowWatermarkDao = lowWatermarkDao;
    this.mapper = mapper;
  }

  public void export(final OutputStream out) throws IOException {
    try (final JsonGenerator jsonGenerator = mapper.getFactory().createGenerator(out)) {
      final Optional<Bytes32> gvr = jdbi.inTransaction(metadataDao::findGenesisValidatorsRoot);
      if (gvr.isEmpty()) {
        throw new RuntimeException("No genesis validators root for slashing protection data");
      }

      jsonGenerator.writeStartObject();

      final Metadata metadata = new Metadata(FORMAT_VERSION, gvr.get());

      jsonGenerator.writeFieldName("metadata");
      mapper.writeValue(jsonGenerator, metadata);

      jsonGenerator.writeArrayFieldStart("data");
      populateInterchangeData(jsonGenerator);
      jsonGenerator.writeEndArray();

      jsonGenerator.writeEndObject();
    }
  }

  private void populateInterchangeData(final JsonGenerator jsonGenerator) {
    jdbi.useTransaction(
        h ->
            validatorsDao
                .findAllValidators(h)
                .forEach(
                    validator -> {
                      try {
                        populateValidatorRecord(h, validator, jsonGenerator);
                      } catch (final IOException e) {
                        throw new UncheckedIOException(
                            "Failed to construct a validator entry in json", e);
                      }
                    }));
  }

  private void populateValidatorRecord(
      final Handle handle, final Validator validator, final JsonGenerator jsonGenerator)
      throws IOException {
    final Optional<SigningWatermark> watermark =
        lowWatermarkDao.findLowWatermarkForValidator(handle, validator.getId());
    if (watermark.isEmpty()) {
      LOG.warn(
          "No low watermark available, producing empty export for validator {}",
          validator.getPublicKey());
      return;
    }
    LOG.info("Exporting entries for validator {}", validator.getPublicKey().toHexString());
    jsonGenerator.writeStartObject();
    jsonGenerator.writeStringField("pubkey", validator.getPublicKey().toHexString());
    writeBlocks(handle, watermark.get(), validator, jsonGenerator);
    writeAttestations(handle, watermark.get(), validator, jsonGenerator);
    jsonGenerator.writeEndObject();
  }

  private void writeBlocks(
      final Handle handle,
      final SigningWatermark watermark,
      final Validator validator,
      final JsonGenerator jsonGenerator)
      throws IOException {
    jsonGenerator.writeArrayFieldStart("signed_blocks");

    if (watermark.getSlot() == null) {
      LOG.warn(
          "No block slot low watermark exists for {}, producing empty block listing",
          validator.getPublicKey());
    } else {

      signedBlocksDao
          .findAllBlockSignedBy(handle, validator.getId())
          .forEach(
              block -> {
                if (block.getSlot().compareTo(watermark.getSlot()) >= 0) {
                  final tech.pegasys.web3signer.slashingprotection.interchange.model.SignedBlock
                      jsonBlock =
                          new tech.pegasys.web3signer.slashingprotection.interchange.model
                              .SignedBlock(block.getSlot(), block.getSigningRoot().orElse(null));
                  try {
                    mapper.writeValue(jsonGenerator, jsonBlock);
                  } catch (final IOException e) {
                    throw new UncheckedIOException(
                        "Failed to construct a signed_blocks entry in json", e);
                  }
                } else {
                  LOG.debug(
                      "Ignoring block in slot {} for validator {}, as is below watermark",
                      block.getSlot(),
                      validator.getPublicKey());
                }
              });
    }

    jsonGenerator.writeEndArray();
  }

  private void writeAttestations(
      final Handle handle,
      final SigningWatermark watermark,
      final Validator validator,
      final JsonGenerator jsonGenerator)
      throws IOException {
    jsonGenerator.writeArrayFieldStart("signed_attestations");

    if (watermark.getSourceEpoch() == null || watermark.getTargetEpoch() == null) {
      LOG.warn(
          "Missing attestation low watermark for {}, producing empty attestation listing",
          validator.getPublicKey());
    } else {
      signedAttestationsDao
          .findAllAttestationsSignedBy(handle, validator.getId())
          .forEach(
              attestation -> {
                if ((attestation.getSourceEpoch().compareTo(watermark.getSourceEpoch()) >= 0)
                    && (attestation.getTargetEpoch().compareTo(watermark.getTargetEpoch()) >= 0)) {
                  final tech.pegasys.web3signer.slashingprotection.interchange.model
                          .SignedAttestation
                      jsonAttestation =
                          new tech.pegasys.web3signer.slashingprotection.interchange.model
                              .SignedAttestation(
                              attestation.getSourceEpoch(),
                              attestation.getTargetEpoch(),
                              attestation.getSigningRoot().orElse(null));
                  try {
                    mapper.writeValue(jsonGenerator, jsonAttestation);
                  } catch (final IOException e) {
                    throw new UncheckedIOException(
                        "Failed to construct a signed_attestations entry in json", e);
                  }
                } else {
                  LOG.debug(
                      "Ignoring attestation with source epoch {}, and target epoch {}, for validator {}, as is below watermark",
                      attestation.getSourceEpoch(),
                      attestation.getTargetEpoch(),
                      validator.getPublicKey());
                }
              });
    }
    jsonGenerator.writeEndArray();
  }
}
