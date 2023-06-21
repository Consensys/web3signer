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
package tech.pegasys.web3signer.signing.secp256k1.common;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import com.google.common.io.Files;

public class PasswordFileUtil {

  /**
   * Read password from the first line of specified file
   *
   * @param path The file to read the password from
   * @return Password
   * @throws IOException For file operations
   * @throws SignerInitializationException If password file is empty
   */
  public static String readPasswordFromFile(final Path path) throws IOException {
    return Optional.ofNullable(Files.asCharSource(path.toFile(), UTF_8).readFirstLine()).orElse("");
  }
}
