package tech.pegasys.web3signer.commandline.config;

import java.nio.file.Path;
import picocli.CommandLine.Option;
import tech.pegasys.web3signer.signing.config.KeystoreParameters;

public class PicoKeystoreParameters  implements KeystoreParameters {

  @Option(
      names = {"--keystores-path"},
      description =
          "The path to a directory storing Eth2 keystores")
  private Path keystoresPath;

  @Option(
      names = {"--keystores-passwords-path"},
      description =
          "The path to a directory with the corresponding passwords file for the keystores."
              + " Filename for the password without the extension must match the keystore filename.")
  private Path keystoresPasswordsPath;

  @Override
  public Path getKeystoresPath() {
    return keystoresPath;
  }

  @Override
  public Path getKeystoresPasswordsPath() {
    return keystoresPasswordsPath;
  }

  @Override
  public boolean isEnabled() {
    return keystoresPath != null;
  }

}
