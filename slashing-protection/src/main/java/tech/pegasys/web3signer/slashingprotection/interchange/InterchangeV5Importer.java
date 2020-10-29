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
import tech.pegasys.web3signer.slashingprotection.interchange.model.SignedAttestation;
import tech.pegasys.web3signer.slashingprotection.interchange.model.SignedBlock;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.tuweni.bytes.Bytes;
import org.jdbi.v3.core.Jdbi;

public class InterchangeV5Importer {

  private static final int FORMAT_VERSION = 5;

  private final Jdbi jdbi;
  private final ValidatorsDao validatorsDao;
  private final SignedBlocksDao signedBlocksDao;
  private final SignedAttestationsDao signedAttestationsDao;
  private final ObjectMapper mapper;

  public InterchangeV5Importer(
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

  public void importData(final InputStream input) throws IOException {

    try (JsonParser jsonParser = mapper.getFactory().createParser(input)) {
      final ObjectNode rootNode = mapper.readTree(jsonParser);

      final JsonNode metadataJsonNode = rootNode.get("metadata");
      final Metadata metadata = mapper.treeToValue(metadataJsonNode, Metadata.class);

      if (metadata.getFormatVersion() != FORMAT_VERSION) {
        throw new IllegalStateException("Expecting an interchange_format_version of 5");
      }

      final JsonNode dataJsonNode = rootNode.get("data");
      final ArrayNode dataNode = (ArrayNode) dataJsonNode;
      for (int i = 0; i < dataNode.size(); i++) {
        final JsonNode validatorNode = dataNode.get(i);
        parseValidator(validatorNode);
      }
    }
  }

  private void parseValidator(final JsonNode node) throws JsonProcessingException {
    if (node.getNodeType() != JsonNodeType.OBJECT) {
      throw new IllegalStateException("Element of 'data' was not an object");
    }
    final ObjectNode parentNode = (ObjectNode) node;
    final String pubKey = parentNode.get("pubkey").textValue();
    final Validator validator =
        jdbi.inTransaction(h -> validatorsDao.insertIfNotExist(h, Bytes.fromHexString(pubKey)));

    final ArrayNode signedBlocksNode = parentNode.withArray("signed_blocks");
    for (int i = 0; i < signedBlocksNode.size(); i++) {
      final SignedBlock jsonBlock = mapper.treeToValue(signedBlocksNode.get(i), SignedBlock.class);
      jdbi.useTransaction(
          h ->
              signedBlocksDao.insertBlockProposal(
                  h,
                  new tech.pegasys.web3signer.slashingprotection.dao.SignedBlock(
                      validator.getId(), jsonBlock.getSlot(), jsonBlock.getSigningRoot())));
    }

    final ArrayNode signedAttestationNode = parentNode.withArray("signed_attestations");
    for (int i = 0; i < signedAttestationNode.size(); i++) {
      final SignedAttestation jsonAttestation =
          mapper.treeToValue(signedAttestationNode.get(i), SignedAttestation.class);
      jdbi.useTransaction(
          h ->
              signedAttestationsDao.insertAttestation(
                  h,
                  new tech.pegasys.web3signer.slashingprotection.dao.SignedAttestation(
                      validator.getId(),
                      jsonAttestation.getSourceEpoch(),
                      jsonAttestation.getTargetEpoch(),
                      jsonAttestation.getSigningRoot())));
    }
  }
}
