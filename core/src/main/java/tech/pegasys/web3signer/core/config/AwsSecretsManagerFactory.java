package tech.pegasys.web3signer.core.config;


import tech.pegasys.signers.aws.AwsSecretsManager;

public class AwsSecretsManagerFactory {

  public static AwsSecretsManager createAwsSecretsManager(
      final AwsSecretsManagerParameters awsSecretsManagerParameters) {
    switch (awsSecretsManagerParameters.getAuthenticationMode()) {
      case SPECIFIED:
        return AwsSecretsManager.createAwsSecretsManager(
          awsSecretsManagerParameters.getAccessKeyId(),
          awsSecretsManagerParameters.getSecretAccessKey(),
          awsSecretsManagerParameters.getRegion());
      default:
        return AwsSecretsManager.createAwsSecretsManager(
          awsSecretsManagerParameters.getRegion());
    }
  }

}
