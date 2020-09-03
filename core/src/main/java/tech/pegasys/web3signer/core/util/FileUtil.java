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
package tech.pegasys.web3signer.core.util;

import tech.pegasys.web3signer.core.InitializationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class FileUtil {
  public static String readFirstLineFromFile(final Path file) throws IOException {
    final List<String> lines = Files.readAllLines(file);
    try (final Stream<String> linesStream = lines.stream()) {
      return linesStream
          .findFirst()
          .orElseThrow(() -> new InitializationException("Cannot read from empty file"));
    }
  }
}
