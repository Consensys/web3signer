/*
 * Copyright 2019 ConsenSys AG.
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
package tech.pegasys.eth2signer.core.multikey.metadata;

import tech.pegasys.eth2signer.core.multikey.MultiSignerFactory;
import tech.pegasys.eth2signer.core.signing.ArtifactSigner;

public abstract class SigningMetadataFile {

  private static final String FILE_EXTENSION = ".yaml";
  private static final String FILE_EXTENSION_REGEX = "\\.yaml";
  protected String baseFilename;

  public SigningMetadataFile(final String filename) {
    this.baseFilename = getFilenameWithoutExtension(filename);
  }

  public String getBaseFilename() {
    return baseFilename;
  }

  private String getFilenameWithoutExtension(final String filename) {
    if (filename.endsWith(FILE_EXTENSION)) {
      return filename.replaceAll(FILE_EXTENSION_REGEX, "");
    } else {
      throw new IllegalArgumentException("Invalid config filename extension: " + filename);
    }
  }

  public abstract ArtifactSigner createSigner(final MultiSignerFactory factory);
}
