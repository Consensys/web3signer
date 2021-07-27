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
import tech.pegasys.web3signer.slashingprotection.dao.Validator;
import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;
import tech.pegasys.web3signer.slashingprotection.interchange.model.Metadata;
import tech.pegasys.web3signer.slashingprotection.validator.GenesisValidatorRootValidator;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

public class InterchangeV5Importer {

  private static final Logger LOG = LogManager.getLogger();

  private static final String FORMAT_VERSION = "5";

  private final Jdbi jdbi;
  private final ValidatorsDao validatorsDao;
  private final SignedBlocksDao signedBlocksDao;
  private final SignedAttestationsDao signedAttestationsDao;
  private final MetadataDao metadataDao;
  private final LowWatermarkDao lowWatermarkDao;
  private final ObjectMapper mapper;

  public InterchangeV5Importer(
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

  public void importData(final InputStream input) throws IOException {

    try (final JsonParser jsonParser = mapper.getFactory().createParser(input)) {
      final ObjectNode rootNode = mapper.readTree(jsonParser);

      final JsonNode metadataJsonNode = rootNode.get("metadata");
      final Metadata metadata = mapper.treeToValue(metadataJsonNode, Metadata.class);

      if (!metadata.getFormatVersion().equals(FORMAT_VERSION)) {
        throw new IllegalStateException(
            "Expecting an interchange_format_version of " + FORMAT_VERSION);
      }

      final Bytes32 gvr = Bytes32.wrap(metadata.getGenesisValidatorsRoot());
      final GenesisValidatorRootValidator genesisValidatorRootValidator =
          new GenesisValidatorRootValidator(jdbi, metadataDao);
      if (!genesisValidatorRootValidator.checkGenesisValidatorsRootAndInsertIfEmpty(gvr)) {
        throw new IllegalArgumentException(
            String.format(
                "Supplied genesis validators root %s does not match value in database", gvr));
      }

      final ArrayNode dataNode = rootNode.withArray("data");
      jdbi.useTransaction(
          h -> {
            for (int i = 0; i < dataNode.size(); i++) {
              try {
                final JsonNode validatorNode = dataNode.get(i);
                parseValidator(h, validatorNode);
              } catch (final IllegalArgumentException e) {
                LOG.error("Failed to parse validator {}, due to {}", i, e.getMessage());
                throw e;
              }
            }
          });
    }
  }

  private void parseValidator(final Handle handle, final JsonNode node)
      throws JsonProcessingException {
    if (node.isArray()) {
      throw new IllegalStateException("Element of 'data' was not an object");
    }
    final ObjectNode parentNode = (ObjectNode) node;
    final String pubKey = parentNode.required("pubkey").textValue();
    final List<Validator> validators =
        validatorsDao.registerValidators(handle, List.of(Bytes.fromHexString(pubKey)));
    if (validators.isEmpty()) {
      throw new IllegalStateException("Unable to register validator " + pubKey);
    }
    final Validator validator = validators.get(0);

    final ArrayNode signedBlocksNode = parentNode.withArray("signed_blocks");
    importBlocks(handle, validator, signedBlocksNode);

    final ArrayNode signedAttestationNode = parentNode.withArray("signed_attestations");
    importAttestations(handle, validator, signedAttestationNode);
  }

  private void importBlocks(
      final Handle handle, final Validator validator, final ArrayNode signedBlocksNode)
      throws JsonProcessingException {

    final BlockImporter blockImporter =
        new BlockImporter(validator, handle, mapper, lowWatermarkDao, signedBlocksDao);
    blockImporter.importFrom(signedBlocksNode);
  }

  private void importAttestations(
      final Handle handle, final Validator validator, final ArrayNode signedAttestationNode)
      throws JsonProcessingException {

    final AttestationImporter attestationImporter =
        new AttestationImporter(validator, handle, mapper, lowWatermarkDao, signedAttestationsDao);

    attestationImporter.importFrom(signedAttestationNode);
  }
}
