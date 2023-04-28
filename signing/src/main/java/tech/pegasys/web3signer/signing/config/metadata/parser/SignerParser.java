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
package tech.pegasys.web3signer.signing.config.metadata.parser;

import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.config.metadata.SigningMetadata;
import tech.pegasys.web3signer.signing.config.metadata.SigningMetadataException;

import java.util.List;

public interface SignerParser {

  List<SigningMetadata> readSigningMetadata(final String fileContent)
      throws SigningMetadataException;

  List<ArtifactSigner> parse(List<SigningMetadata> signingMetadataList)
      throws SigningMetadataException;
}
