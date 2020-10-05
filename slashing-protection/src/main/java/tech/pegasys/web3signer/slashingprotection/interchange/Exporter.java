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
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package tech.pegasys.web3signer.slashingprotection.interchange;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Signed;
import org.jdbi.v3.core.Jdbi;
import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestationsDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlocksDao;
import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;
import tech.pegasys.web3signer.slashingprotection.interchange.model.ExportFormat;
import tech.pegasys.web3signer.slashingprotection.interchange.model.Metadata;
import tech.pegasys.web3signer.slashingprotection.interchange.model.Metadata.Format;
import tech.pegasys.web3signer.slashingprotection.interchange.model.SignedArtifacts;
import tech.pegasys.web3signer.slashingprotection.interchange.model.SignedAttestation;
import tech.pegasys.web3signer.slashingprotection.interchange.model.SignedBlock;

public class Exporter {

  private final Jdbi jdbi;
  private final ValidatorsDao validatorsDao;
  private final SignedBlocksDao signedBlocksDao;
  private final SignedAttestationsDao signedAttestationsDao;
  private final ObjectMapper mapper;

  public Exporter(final Jdbi jdbi,
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

  public void exportTo(final OutputStream out) throws IOException {
    final Metadata metadata = new Metadata(Format.COMPLETE, 4, "notApplicable");
    final List<SignedArtifacts> signedArtifacts = generateModelFromDatabase();

    final ExportFormat toExport = new ExportFormat(metadata, signedArtifacts);

    mapper.writeValue(out, toExport);
  }

  private List<SignedArtifacts> generateModelFromDatabase() {
    return jdbi.inTransaction(h -> {
          final List<SignedArtifacts> result = Lists.newArrayList();
          validatorsDao.retrieveAllValidators(h).forEach(validator -> {
            final List<SignedBlock> blocks =
                signedBlocksDao.getAllBlockSignedBy(h, validator.getId()).stream()
                    .map(b -> new SignedBlock(b.getSlot().toString(),
                        b.getSigningRoot().toHexString()))
                    .collect(
                        Collectors.toList());
            final List<SignedAttestation> attestations =
                signedAttestationsDao.getAllAttestationsSignedBy(h, validator.getId()).stream()
                    .map(a -> new SignedAttestation(a.getSourceEpoch().toString(),
                        a.getTargetEpoch().toString(), a.getSigningRoot().toHexString())).collect(
                    Collectors.toList());
            result
                .add(new SignedArtifacts(validator.getPublicKey().toHexString(), blocks,
                    attestations));
          });
          return result;
        });
  }
}
