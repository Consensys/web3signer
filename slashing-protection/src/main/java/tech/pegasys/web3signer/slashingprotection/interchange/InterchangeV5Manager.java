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

import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestationsDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlocksDao;
import tech.pegasys.web3signer.slashingprotection.dao.Validator;
import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;
import tech.pegasys.web3signer.slashingprotection.interchange.model.Metadata;

import java.io.IOException;
import java.io.OutputStream;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tuweni.bytes.Bytes;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

public class InterchangeV5Manager implements InterchangeManager {

  private static final int FORMAT_VERSION = 5;

  private final Jdbi jdbi;
  private final ValidatorsDao validatorsDao;
  private final SignedBlocksDao signedBlocksDao;
  private final SignedAttestationsDao signedAttestationsDao;
  private final ObjectMapper mapper;

  public InterchangeV5Manager(
      final Jdbi jdbi,
      final ValidatorsDao validatorsDao,
      final SignedBlocksDao signedBlocksDao,
      final SignedAttestationsDao signedAttestationsDao,
      final ObjectMapper mapper) {
    this.jdbi = jdbi;
    this.validatorsDao = validatorsDao;
    this.signedBlocksDao = signedBlocksDao;
    this.signedAttestationsDao = signedAttestationsDao;
    this.mapper = mapper;
  }

  @Override
  public void export(final OutputStream out) throws IOException {
    try (final JsonGenerator jsonGenerator = mapper.getFactory().createGenerator(out)) {
      jsonGenerator.writeStartObject();

      final Metadata metadata = new Metadata(FORMAT_VERSION, Bytes.fromHexString("FFFFFFFF"));

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
                      } catch (IOException e) {
                        throw new RuntimeException("Failed to construct a validator entry in json");
                      }
                    }));
  }

  private void populateValidatorRecord(
      final Handle h, final Validator validator, final JsonGenerator jsonGenerator)
      throws IOException {
    jsonGenerator.writeStartObject();
    jsonGenerator.writeStringField("pubkey", validator.getPublicKey().toHexString());
    writeBlocks(h, validator, jsonGenerator);
    writeAttestations(h, validator, jsonGenerator);
    jsonGenerator.writeEndObject();
  }

  private void writeBlocks(
      final Handle h, final Validator validator, final JsonGenerator jsonGenerator)
      throws IOException {
    jsonGenerator.writeArrayFieldStart("signed_blocks");

    signedBlocksDao
        .findAllBlockSignedBy(h, validator.getId())
        .forEach(
            b -> {
              final tech.pegasys.web3signer.slashingprotection.interchange.model.SignedBlock
                  jsonBlock =
                      new tech.pegasys.web3signer.slashingprotection.interchange.model.SignedBlock(
                          b.getSlot(), b.getSigningRoot().orElse(null));
              try {
                mapper.writeValue(jsonGenerator, jsonBlock);
              } catch (IOException e) {
                throw new RuntimeException("Failed to construct a signed_blocks entry in json");
              }
            });

    jsonGenerator.writeEndArray();
  }

  private void writeAttestations(
      final Handle h, final Validator validator, final JsonGenerator jsonGenerator)
      throws IOException {
    jsonGenerator.writeArrayFieldStart("signed_attestations");

    signedAttestationsDao
        .findAllAttestationsSignedBy(h, validator.getId())
        .forEach(
            a -> {
              final tech.pegasys.web3signer.slashingprotection.interchange.model.SignedAttestation
                  jsonAttestation =
                      new tech.pegasys.web3signer.slashingprotection.interchange.model
                          .SignedAttestation(
                          a.getSourceEpoch(), a.getTargetEpoch(), a.getSigningRoot().orElse(null));
              try {
                mapper.writeValue(jsonGenerator, jsonAttestation);
              } catch (IOException e) {
                throw new RuntimeException(
                    "Failed to construct a signed_attestations entry in json");
              }
            });
    jsonGenerator.writeEndArray();
  }
}
