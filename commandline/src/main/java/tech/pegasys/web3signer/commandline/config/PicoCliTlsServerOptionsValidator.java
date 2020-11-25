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

import tech.pegasys.web3signer.core.config.ClientAuthConstraints;

import java.io.File;

import picocli.CommandLine;

/**
 * This class provides similar validation for TLS options as PicoCli ArgGroups (which cannot be used
 * due to limitations of config file/environment value provider).
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
    if (onlyOneInitialized(keyStoreFile, keyStorePasswordFile)) {
      throw new CommandLine.ParameterException(
          spec.commandLine(),
          "--tls-keystore-file must be specified together with --tls-keystore-password-file");
    }

    final ClientAuthConstraints picoCliClientAuthConstraints =
        picoCliTlsServerOptions.clientAuthConstraints;
    if (picoCliTlsServerOptions.tlsAllowAnyClient
        && (picoCliClientAuthConstraints.getKnownClientsFile().isPresent()
            || picoCliClientAuthConstraints.isCaAuthorizedClientAllowed())) {
      throw new CommandLine.ParameterException(
          spec.commandLine(),
          "--tls-allow-any-client cannot be set to true when --tls-known-clients-file is specified or --tls-allow-ca-clients is set to true");
    }

    if (!picoCliTlsServerOptions.tlsAllowAnyClient
        && picoCliClientAuthConstraints.getKnownClientsFile().isEmpty()
        && !picoCliClientAuthConstraints.isCaAuthorizedClientAllowed()) {
      throw new CommandLine.ParameterException(
          spec.commandLine(),
          "--tls-known-clients-file must be specified if both --tls-allow-any-client and --tls-allow-ca-clients are set to false");
    }
  }

  private static boolean onlyOneInitialized(final Object o1, final Object o2) {
    return (o1 == null) != (o2 == null);
  }
}
