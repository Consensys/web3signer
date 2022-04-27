/*
 * Copyright 2022 ConsenSys AG.
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
package tech.pegasys.web3signer.signing.config;

import tech.pegasys.signers.aws.AwsSecretsManagerProvider;
import tech.pegasys.web3signer.signing.BlsArtifactSigner;
import tech.pegasys.web3signer.signing.config.metadata.SignerOrigin;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AwsBulkLoader extends KeystorageBulkLoader {

  public static Collection<BlsArtifactSigner> load(
      final AwsSecretsManagerProvider awsSecretsManagerProvider, final List<String> secretNames) {
    final Set<Optional<String>> secrets = fetchSecrets(awsSecretsManagerProvider, secretNames);
   return mapToSigner(secrets.stream(), SignerOrigin.AWS);
  }

  private static Set<Optional<String>> fetchSecrets(
      final AwsSecretsManagerProvider awsSecretsManagerProvider, final List<String> secretNames) {
    return awsSecretsManagerProvider.createAwsSecretsManager().mapSecrets()

  }
}
