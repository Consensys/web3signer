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
package tech.pegasys.web3signer.commandline.config;

import java.io.File;

import picocli.CommandLine;

/**
 * Because of config file/environment default value provider, PicoCLI ArgGroup feature cannot be
 * used. This class provides similar set of validation for TLS options.
 */
public class PicoCliTlsServerOptionsValidator {
  private final CommandLine.Model.CommandSpec spec;
  private final PicoCliTlsServerOptions picoCliTlsServerOptions;

  public PicoCliTlsServerOptionsValidator(
      final CommandLine.Model.CommandSpec spec,
      final PicoCliTlsServerOptions picoCliTlsServerOptions) {
    this.spec = spec;
    this.picoCliTlsServerOptions = picoCliTlsServerOptions;
  }

  public void validate() throws CommandLine.ParameterException {
    final File keyStoreFile = picoCliTlsServerOptions.getKeyStoreFile();
    final File keyStorePasswordFile = picoCliTlsServerOptions.getKeyStorePasswordFile();

    // no need to further validate if keystore file/password are not specified
    if (keyStoreFile == null && keyStorePasswordFile == null) {
      return;
    }

    // if tls keystore is specified, the password file must be specified.
    if ((keyStoreFile != null && keyStorePasswordFile == null)
        || (keyStoreFile == null && keyStorePasswordFile != null)) {
      throw new CommandLine.ParameterException(
          spec.commandLine(),
          "Error: --tls-keystore-file must be specified together with --tls-keystore-password-file");
    }

    final PicoCliClientAuthConstraints picoCliClientAuthConstraints =
        picoCliTlsServerOptions.getPicoCliClientAuthConstraints();
    if (picoCliTlsServerOptions.isTlsAllowAnyClient()
        && (picoCliClientAuthConstraints.getKnownClientsFile().isPresent()
            || picoCliClientAuthConstraints.isCaAuthorizedClientAllowed())) {
      throw new CommandLine.ParameterException(
          spec.commandLine(),
          "Error: --tls-allow-any-client cannot be set to true when --tls-known-clients-file is specified or --tls-allow-ca-clients is set to true");
    }

    if (!picoCliTlsServerOptions.isTlsAllowAnyClient()
        && picoCliClientAuthConstraints.getKnownClientsFile().isEmpty()
        && !picoCliClientAuthConstraints.isCaAuthorizedClientAllowed()) {
      throw new CommandLine.ParameterException(
          spec.commandLine(),
          "Error: --tls-known-clients-file must be specified if both --tls-allow-any-client and --tls-allow-ca-clients are set to false");
    }
  }
}
