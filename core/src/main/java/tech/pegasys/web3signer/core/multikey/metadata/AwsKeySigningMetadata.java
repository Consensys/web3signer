package tech.pegasys.web3signer.core.multikey.metadata;

import tech.pegasys.web3signer.core.config.AwsAuthenticationMode;
import tech.pegasys.web3signer.core.config.AwsSecretsManagerParameters;
import tech.pegasys.web3signer.core.signing.ArtifactSigner;
import tech.pegasys.web3signer.core.signing.KeyType;

public class AwsKeySigningMetadata extends SigningMetadata implements AwsSecretsManagerParameters {

  private final AwsAuthenticationMode authenticationMode;
  private final String region;
  private final String accessKeyId;
  private final String secretAccessKey;
  private final String secretName;
  private final String secretValue;

  protected AwsKeySigningMetadata(final AwsAuthenticationMode authenticationMode, final String region, final String accessKeyId, final String secretAccessKey, final String secretName, final String secretValue) {
    super(KeyType.BLS);
    this.authenticationMode = authenticationMode;
    this.region = region;
    this.accessKeyId = accessKeyId;
    this.secretAccessKey = secretAccessKey;
    this.secretName = secretName;
    this.secretValue = secretValue;

  }

  @Override
  public AwsAuthenticationMode getAuthenticationMode() {
    return this.authenticationMode;
  }

  @Override
  public String getAccessKeyId() { return accessKeyId; }

  @Override
  public String getSecretAccessKey() { return secretAccessKey; }

  @Override
  public String getSecretName() {
    return secretName;
  }

  @Override
  public String getSecretValue() {
    return secretValue;
  }

  @Override
  public String getRegion() { return region; }

  @Override
  public ArtifactSigner createSigner(final ArtifactSignerFactory factory) {
    return factory.create(this);
  }

}
