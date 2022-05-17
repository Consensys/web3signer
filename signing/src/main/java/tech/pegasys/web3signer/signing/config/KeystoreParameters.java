package tech.pegasys.web3signer.signing.config;

import java.nio.file.Path;

public interface KeystoreParameters {

  Path getKeystoresPath();

  Path getKeystoresPasswordsPath();

  boolean isEnabled();

}
