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

import java.nio.file.Path;

import com.google.common.base.Objects;

public class FileBasedSigningMetadataFile extends SigningMetadataFile {

  private final Path keyPath;
  private final Path passwordPath;

  public FileBasedSigningMetadataFile(
      final String filename, final Path keyPath, final Path passwordPath) {
    super(filename);
    this.keyPath = keyPath;
    this.passwordPath = passwordPath;
  }

  public Path getKeyPath() {
    return keyPath;
  }

  public Path getPasswordPath() {
    return passwordPath;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final FileBasedSigningMetadataFile that = (FileBasedSigningMetadataFile) o;
    return Objects.equal(baseFilename, that.baseFilename)
        && Objects.equal(keyPath, that.keyPath)
        && Objects.equal(passwordPath, that.passwordPath);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(baseFilename, keyPath, passwordPath);
  }

  @Override
  public ArtifactSigner createSigner(final MultiSignerFactory factory) {
    return factory.createSigner(this);
  }
}
