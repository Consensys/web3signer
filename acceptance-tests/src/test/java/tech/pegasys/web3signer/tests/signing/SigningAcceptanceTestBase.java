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
package tech.pegasys.web3signer.tests.signing;

import static java.nio.charset.StandardCharsets.UTF_8;

import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.tests.AcceptanceTestBase;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.io.Resources;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.io.TempDir;

public class SigningAcceptanceTestBase extends AcceptanceTestBase {
  protected @TempDir Path testDirectory;
  protected @TempDir Path tempBinDirectory;

  protected void setupSigner(final String mode) {
    setupSigner(mode, null);
  }

  protected void setupSigner(final String mode, final Map<String, String> env) {
    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withKeyStoreDirectory(testDirectory).withMode(mode).withEnvironment(env);
    startSigner(builder.build());
  }

  protected Bytes verifyAndGetSignatureResponse(final Response response) {
    response.then().contentType(ContentType.TEXT).statusCode(200);
    return Bytes.fromHexString(response.body().print());
  }

  protected Map<String, String> yubiHsmShellEnvMap() {
    // TODO: Find a better way than to modify the executeable script
    try {
      final Path source = Path.of(Resources.getResource("YubiShellSimulator").getPath());

      final List<String> sourceLines = new ArrayList<>(Files.readAllLines(source));
      sourceLines.set(0, "#!" + getJvmPath());

      final Path dest = tempBinDirectory.resolve(source.getFileName());
      Files.write(dest, sourceLines, UTF_8);
      // make dest file executeable
      dest.toFile().setExecutable(true);

      return Map.of("WEB3SIGNER_YUBIHSM_SHELL_PATH", dest.toString());
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private String getJvmPath() {
    return Path.of(System.getProperty("java.home"), "bin", "java") + " --source 11";
  }
}
