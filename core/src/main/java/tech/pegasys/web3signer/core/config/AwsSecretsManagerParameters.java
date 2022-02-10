package tech.pegasys.web3signer.core.config;

public interface AwsSecretsManagerParameters {
  AwsAuthenticationMode getAuthenticationMode();
  String getAccessKeyId();
  String getSecretAccessKey();
  String getSecretName();
  String getSecretValue();
  String getRegion();
}
