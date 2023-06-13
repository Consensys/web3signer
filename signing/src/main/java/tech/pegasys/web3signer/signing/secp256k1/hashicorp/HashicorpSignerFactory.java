/*
 * Copyright 2020 ConsenSys AG.
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
package tech.pegasys.web3signer.signing.secp256k1.hashicorp;

import tech.pegasys.web3signer.keystorage.hashicorp.HashicorpConnection;
import tech.pegasys.web3signer.keystorage.hashicorp.HashicorpConnectionFactory;
import tech.pegasys.web3signer.keystorage.hashicorp.HashicorpException;
import tech.pegasys.web3signer.keystorage.hashicorp.config.HashicorpKeyConfig;
import tech.pegasys.web3signer.signing.secp256k1.Signer;
import tech.pegasys.web3signer.signing.secp256k1.common.SignerInitializationException;
import tech.pegasys.web3signer.signing.secp256k1.filebased.CredentialSigner;

import org.web3j.crypto.Credentials;

public class HashicorpSignerFactory {

  public HashicorpSignerFactory() {}

  public Signer create(final HashicorpKeyConfig keyConfig) {
    try (final HashicorpConnectionFactory connectionFactory = new HashicorpConnectionFactory()) {
      final HashicorpConnection connection =
          connectionFactory.create(keyConfig.getConnectionParams());
      final String secret = connection.fetchKey(keyConfig.getKeyDefinition());
      final Credentials credentials = Credentials.create(secret);
      return new CredentialSigner(credentials);
    } catch (final HashicorpException e) {
      throw new SignerInitializationException("Failed to extract secret from Hashicorp vault.", e);
    }
  }

  public void shutdown() {}
}
