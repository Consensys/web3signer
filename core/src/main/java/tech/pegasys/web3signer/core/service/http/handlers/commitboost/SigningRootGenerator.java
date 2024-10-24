/*
 * Copyright 2024 ConsenSys AG.
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
package tech.pegasys.web3signer.core.service.http.handlers.commitboost;

import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.infrastructure.bytes.Bytes4;
import tech.pegasys.teku.infrastructure.ssz.Merkleizable;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.web3signer.core.service.http.handlers.commitboost.datastructure.BlsProxyKeySchema;
import tech.pegasys.web3signer.core.service.http.handlers.commitboost.datastructure.SECPProxyKeySchema;
import tech.pegasys.web3signer.core.service.http.handlers.commitboost.json.ProxyKeyMessage;
import tech.pegasys.web3signer.core.service.http.handlers.commitboost.json.ProxyKeySignatureScheme;
import tech.pegasys.web3signer.core.util.Web3SignerSigningRootUtil;
import tech.pegasys.web3signer.signing.secp256k1.EthPublicKeyUtils;

import java.security.interfaces.ECPublicKey;

import com.google.common.annotations.VisibleForTesting;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public class SigningRootGenerator {
  private static final Bytes4 COMMIT_BOOST_DOMAIN = Bytes4.fromHexString("0x6d6d6f43");
  private final Bytes32 domain;

  public SigningRootGenerator(final Spec eth2Spec, final Bytes32 genesisValidatorsRoot) {
    final Bytes4 genesisForkVersion = eth2Spec.getGenesisSpec().getConfig().getGenesisForkVersion();
    domain =
        Web3SignerSigningRootUtil.computeDomain(
            COMMIT_BOOST_DOMAIN, genesisForkVersion, genesisValidatorsRoot);
  }

  public Bytes computeSigningRoot(
      final ProxyKeyMessage proxyKeyMessage, final ProxyKeySignatureScheme scheme) {

    final BLSPublicKey delegator = BLSPublicKey.fromHexString(proxyKeyMessage.blsPublicKey());
    final Merkleizable proxyKeyMessageToSign;
    if (scheme == ProxyKeySignatureScheme.BLS) {
      final BLSPublicKey proxy = BLSPublicKey.fromHexString(proxyKeyMessage.proxyPublicKey());
      proxyKeyMessageToSign = new BlsProxyKeySchema().create(delegator, proxy);
    } else {
      final ECPublicKey proxy =
          EthPublicKeyUtils.createPublicKey(Bytes.fromHexString(proxyKeyMessage.proxyPublicKey()));
      proxyKeyMessageToSign = new SECPProxyKeySchema().create(delegator, proxy);
    }
    return Web3SignerSigningRootUtil.computeSigningRoot(proxyKeyMessageToSign, domain);
  }

  @VisibleForTesting
  Bytes32 getDomain() {
    return domain;
  }
}
