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
import tech.pegasys.web3signer.slashingprotection.interchange.model.InterchangeV4Format;
import tech.pegasys.web3signer.slashingprotection.interchange.model.Metadata;
import tech.pegasys.web3signer.slashingprotection.interchange.model.Metadata.Format;
import tech.pegasys.web3signer.slashingprotection.interchange.model.SignedArtifacts;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.core.Jdbi;

public class InterchangeV4Manager implements InterchangeManager {

  private final Jdbi jdbi;
  private final ValidatorsDao validatorsDao;
  private final SignedBlocksDao signedBlocksDao;
  private final SignedAttestationsDao signedAttestationsDao;
  private final ObjectMapper mapper;

  public InterchangeV4Manager(
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
  public void exportTo(final OutputStream out) throws IOException {
    final Metadata metadata = new Metadata(Format.COMPLETE, 4, "notApplicable");
    final List<SignedArtifacts> signedArtifacts = generateModelFromDatabase();

    final InterchangeV4Format toExport = new InterchangeV4Format(metadata, signedArtifacts);

    mapper.writerWithDefaultPrettyPrinter().writeValue(out, toExport);
  }

  @Override
  public void importFrom(final InputStream in) throws IOException {
    final InterchangeV4Format toImport = mapper.readValue(in, InterchangeV4Format.class);

    validateMetaData(toImport.getMetdata());

    toImport
        .getSignedArtifacts()
        .forEach(
            artifact -> {
              jdbi.useTransaction(
                  h -> {
                    final Validator validator =
                        validatorsDao.insertIfNotExist(
                            h, Bytes.fromHexString(artifact.getPublicKey()));

                    artifact
                        .getSignedAttestations()
                        .forEach(
                            sa ->
                                signedAttestationsDao.insertAttestation(
                                    h,
                                    new tech.pegasys.web3signer.slashingprotection.dao
                                        .SignedAttestation(
                                        validator.getId(),
                                        UInt64.fromHexString(sa.getSourceEpoch()),
                                        UInt64.fromHexString(sa.getTargetEpoch()),
                                        Bytes.fromHexString(sa.getSigningRoot()))));

                    artifact
                        .getSignedBlocks()
                        .forEach(
                            sb ->
                                signedBlocksDao.insertBlockProposal(
                                    h,
                                    new tech.pegasys.web3signer.slashingprotection.dao.SignedBlock(
                                        validator.getId(),
                                        UInt64.fromHexString(sb.getSlot()),
                                        Bytes.fromHexString(sb.getSigningRoot()))));
                  });
            });
  }

  private void validateMetaData(final Metadata metdata) {
    if (metdata.getFormatVersion() != 4) {
      throw new RuntimeException("Web3signer can only accept version 4 of the interchange format");
    }

    if (metdata.getFormat() != Format.COMPLETE) {
      throw new RuntimeException("Web3Signer can only read complete data sets (not minimal)");
    }

    // SHOULD validate the genesis root, but ... we don't really do that atm
  }

  private List<SignedArtifacts> generateModelFromDatabase() {
    return jdbi.inTransaction(
        h -> {
          final List<SignedArtifacts> result = Lists.newArrayList();
          validatorsDao
              .retrieveAllValidators(h)
              .forEach(
                  validator -> {
                    final List<
                            tech.pegasys.web3signer.slashingprotection.interchange.model
                                .SignedBlock>
                        blocks =
                            signedBlocksDao.getAllBlockSignedBy(h, validator.getId()).stream()
                                .map(
                                    b ->
                                        new tech.pegasys.web3signer.slashingprotection.interchange
                                            .model.SignedBlock(
                                            b.getSlot().toString(),
                                            b.getSigningRoot().toHexString()))
                                .collect(Collectors.toList());
                    final List<
                            tech.pegasys.web3signer.slashingprotection.interchange.model
                                .SignedAttestation>
                        attestations =
                            signedAttestationsDao.getAllAttestationsSignedBy(h, validator.getId())
                                .stream()
                                .map(
                                    a ->
                                        new tech.pegasys.web3signer.slashingprotection.interchange
                                            .model.SignedAttestation(
                                            a.getSourceEpoch().toString(),
                                            a.getTargetEpoch().toString(),
                                            a.getSigningRoot().toHexString()))
                                .collect(Collectors.toList());
                    result.add(
                        new SignedArtifacts(
                            validator.getPublicKey().toHexString(), blocks, attestations));
                  });
          return result;
        });
  }
}
