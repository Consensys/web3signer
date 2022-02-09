package tech.pegasys.web3signer.core.config;

public interface AwsSecretsManagerParameters {
  AwsAuthenticationMode getAuthenticationMode();
  String getSecretName();
  String getSecretValue();
}
