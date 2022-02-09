package tech.pegasys.web3signer.core.config;

public interface AwsSecretsManagerParameters {
  AwsAuthenticationMode getAwsAuthenticationMode();
  String getSecretName();
  String getSecretValue();
}
