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

import static java.util.Collections.emptySet;

import tech.pegasys.signers.aws.AwsSecretsManager;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.web3signer.signing.BlsArtifactSigner;
import tech.pegasys.web3signer.signing.config.metadata.SignerOrigin;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public class AwsBulkLoader extends SignerLoader {

  private static final Logger LOG = LogManager.getLogger();

  public static Collection<BlsArtifactSigner> load(
      final AwsSecretsManager awsSecretsManager, final List<String> secretNames) {
    ForkJoinPool forkJoinPool = null;
    try {
      return new ForkJoinPool(numberOfThreads())
          .submit(
              () ->
                  fetchSecrets(awsSecretsManager, secretNames).stream()
                      .filter(secret -> secret.isPresent())
                      .map(String.class::cast)
                      .map(secret -> Bytes.fromHexString(secret))
                      .map(bytes -> new BLSKeyPair(BLSSecretKey.fromBytes(Bytes32.wrap(bytes))))
                      .map(blsKeyPair -> new BlsArtifactSigner(blsKeyPair, SignerOrigin.AWS))
                      .collect(Collectors.toSet()))
          .get();
    } catch (final Exception e) {
      LOG.error("Unexpected error while bulk loading AWS secrets", e);
    } finally {
      if (forkJoinPool != null) {
        forkJoinPool.shutdown();
      }
    }
    return emptySet();
  }

  private static Set<Optional<String>> fetchSecrets(
      final AwsSecretsManager awsSecretsManager, final List<String> secretNames) {
    return secretNames.parallelStream()
        .map(awsSecretsManager::fetchSecret)
        .collect(Collectors.toSet());
  }
}
