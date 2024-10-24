/*
 * Copyright 2022 ConsenSys AG.
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
package tech.pegasys.web3signer.commandline.config;

import static tech.pegasys.web3signer.commandline.DefaultCommandValues.PATH_FORMAT_HELP;

import tech.pegasys.web3signer.signing.config.KeystoresParameters;

import java.nio.file.Path;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

public class PicoCommitBoostApiParameters implements KeystoresParameters {
  @Spec private CommandSpec commandSpec; // injected by picocli

  @CommandLine.Option(
      names = {"--commit-boost-api-enabled"},
      paramLabel = "<BOOL>",
      description = "Enable the commit boost API (default: ${DEFAULT-VALUE}).",
      arity = "1")
  private boolean isCommitBoostApiEnabled = false;

  @Option(
      names = {"--proxy-keystores-path"},
      description =
          "The path to a writeable directory to store v3 and v4 proxy keystores for commit boost API.",
      paramLabel = PATH_FORMAT_HELP)
  private Path keystoresPath;

  @Option(
      names = {"--proxy-keystores-password-file"},
      description =
          "The path to the password file used to encrypt/decrypt proxy keystores for commit boost API.",
      paramLabel = PATH_FORMAT_HELP)
  private Path keystoresPasswordFile;

  @Override
  public Path getKeystoresPath() {
    return keystoresPath;
  }

  @Override
  public Path getKeystoresPasswordsPath() {
    return null;
  }

  @Override
  public Path getKeystoresPasswordFile() {
    return keystoresPasswordFile;
  }

  @Override
  public boolean isEnabled() {
    return isCommitBoostApiEnabled;
  }

  public void validateParameters() throws ParameterException {
    if (!isCommitBoostApiEnabled) {
      return;
    }

    if (keystoresPath == null) {
      throw new ParameterException(
          commandSpec.commandLine(),
          "Commit boost API is enabled, but --proxy-keystores-path not set");
    }

    if (keystoresPasswordFile == null) {
      throw new ParameterException(
          commandSpec.commandLine(),
          "Commit boost API is enabled, but --proxy-keystores-password-file not set");
    }
  }
}
