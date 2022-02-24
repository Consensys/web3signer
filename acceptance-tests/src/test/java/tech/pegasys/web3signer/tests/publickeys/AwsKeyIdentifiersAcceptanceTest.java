package tech.pegasys.web3signer.tests.publickeys;

import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.DeleteSecretRequest;
import tech.pegasys.web3signer.core.signing.KeyType;

import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static tech.pegasys.web3signer.core.signing.KeyType.BLS;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AwsKeyIdentifiersAcceptanceTest extends KeyIdentifiersAcceptanceTestBase {

  private final String RW_AWS_ACCESS_KEY_ID = System.getenv("RW_AWS_ACCESS_KEY_ID");
  private final String RW_AWS_SECRET_ACCESS_KEY = System.getenv("RW_AWS_SECRET_ACCESS_KEY");

  private final String RO_AWS_ACCESS_KEY_ID = System.getenv("RO_AWS_ACCESS_KEY_ID");
  private final String RO_AWS_SECRET_ACCESS_KEY = System.getenv("RO_AWS_SECRET_ACCESS_KEY");

  private final String AWS_REGION = "us-east-2";

  private String secretName;
  private String privateKey; // secret value
  private String publicKey;

  private SecretsManagerClient secretsManagerClient;

  private void checkEnvironmentVariables() {
    Assumptions.assumeTrue(
      RW_AWS_ACCESS_KEY_ID != null, "Set RW_AWS_ACCESS_KEY_ID environment variable");
    Assumptions.assumeTrue(
      RW_AWS_SECRET_ACCESS_KEY != null, "Set RW_AWS_SECRET_ACCESS_KEY environment variable");
    Assumptions.assumeTrue(
      RO_AWS_ACCESS_KEY_ID != null, "Set RO_AWS_ACCESS_KEY_ID environment variable");
    Assumptions.assumeTrue(
      RO_AWS_SECRET_ACCESS_KEY != null, "Set RO_AWS_SECRET_ACCESS_KEY environment variable");
  }

  private void setupSecretsManagerClient() {
    final AwsBasicCredentials awsBasicCredentials =
      AwsBasicCredentials.create(RW_AWS_ACCESS_KEY_ID, RW_AWS_SECRET_ACCESS_KEY);
    final StaticCredentialsProvider credentialsProvider =
      StaticCredentialsProvider.create(awsBasicCredentials);
    secretsManagerClient =
      SecretsManagerClient.builder()
        .credentialsProvider(credentialsProvider)
        .region(Region.of(AWS_REGION))
        .build();
  }

  private void createSecret() {

    secretName = "signers-aws-integration/" + UUID.randomUUID();

    privateKey = privateKeys(KeyType.BLS)[0];
    publicKey = BLS_PUBLIC_KEY_1;
    final CreateSecretRequest secretRequest =
      CreateSecretRequest.builder().name(secretName).secretString(privateKey).build();
    secretsManagerClient.createSecret(secretRequest);
  }

  private void deleteSecret() {
    final DeleteSecretRequest secretRequest =
      DeleteSecretRequest.builder().secretId(secretName).build();
    secretsManagerClient.deleteSecret(secretRequest);
  }

  private void closeClients() {
    secretsManagerClient.close();
  }

  @BeforeAll
  void setup() {
    checkEnvironmentVariables();
    setupSecretsManagerClient();
    createSecret();
  }

  @AfterAll
  void teardown() {
    deleteSecret();
    closeClients();
  }

  @Test
  public void awsKeysReturnAppropriatePublicKey() {
    metadataFileHelpers.createAwsYamlFileAt(
      testDirectory.resolve(publicKey + ".yaml"),
      AWS_REGION,
      RO_AWS_ACCESS_KEY_ID,
      RO_AWS_SECRET_ACCESS_KEY,
      secretName
      );
    initAndStartSigner("eth2");
    final Response response = callApiPublicKeysWithoutOpenApiClientSideFilter(BLS);
    validateApiResponse(response, containsInAnyOrder(publicKey));
  }
}
