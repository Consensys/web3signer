/*
 * Copyright 2023 ConsenSys AG.
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
package tech.pegasys.web3signer.signing.secp256k1.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;

public class JsonFilesUtil {
  public static List<Path> loadJsonExtPaths(final Path keystoresPath) throws IOException {
    try (final Stream<Path> fileStream = Files.list(keystoresPath)) {
      return fileStream
          .filter(path -> FilenameUtils.getExtension(path.toString()).equalsIgnoreCase("json"))
          .collect(Collectors.toList());
    }
  }
}
