package tech.pegasys.web3signer.core.multikey.metadata;

import tech.pegasys.web3signer.core.config.AwsAuthenticationMode;
import tech.pegasys.web3signer.core.config.AwsSecretsManagerParameters;
import tech.pegasys.web3signer.core.signing.ArtifactSigner;
import tech.pegasys.web3signer.core.signing.KeyType;

public class AwsKeySigningMetadata extends SigningMetadata implements AwsSecretsManagerParameters {

  private final String secretName;
  private final String secretValue;
  private final AwsAuthenticationMode authenticationMode;

  protected AwsKeySigningMetadata(final String secretName, final String secretValue, final AwsAuthenticationMode authenticationMode) {
    super(KeyType.BLS);
    this.secretName = secretName;
    this.secretValue = secretValue;
    this.authenticationMode = authenticationMode;
  }

  @Override
  public AwsAuthenticationMode getAuthenticationMode() {
    return this.authenticationMode;
  }

  @Override
  public String getSecretName() {
    return secretName;
  }

  @Override
  public String getSecretValue() {
    return secretValue;
  }

  @Override
  public ArtifactSigner createSigner(final ArtifactSignerFactory factory) {
    return factory.create(this);
  }

}
