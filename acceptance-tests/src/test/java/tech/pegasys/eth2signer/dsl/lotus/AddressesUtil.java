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
package tech.pegasys.eth2signer.dsl.lotus;

import static tech.pegasys.eth2signer.dsl.lotus.FilecoinKeyType.BLS;
import static tech.pegasys.eth2signer.dsl.lotus.FilecoinKeyType.SECP256K1;

import java.util.Map;

public class AddressesUtil {
  private static final Map<String, FilecoinKey> ADDRESS_MAP;

  static {
    // generated using WalletNew and WallerExport against FileCoin node
    ADDRESS_MAP =
        Map.of(
            "t3q7sj7rgvvlfpc7gx7z7jeco5x3q3aa4g6s54w3rl5alzdb6xa422seznjmtp7agboegcvrakcv22eo5bjlna",
            new FilecoinKey(BLS, "NlWGbwCt8rEK7OTDYat3jy+3tj60cER81cIDUSEnFjU="),
            "t3rzhwtyxwmfbgikcddna3bv3eedn3meyt75gc6urmunbju26asfhaycsim6oc5qvyqbldziq53l3ujfpprhfa",
            new FilecoinKey(BLS, "tFzDgbfTT983FdhnZ8xZjr0JdP37DcijmVm+XvurhFY="),
            "t1jcaxt7yoonwcvllj52kjzh4buo7gjmzemm3c3ny",
            new FilecoinKey(SECP256K1, "5airIxsTE4wslOvXDcHoTnZE2ZWYGw/ZMwJQY0p7Pi4="),
            "t1te5vep7vlsxoh5vqz3fqlm76gewzpd63juum6jq",
            new FilecoinKey(SECP256K1, "0oKQu6xyg0bOCaqNqpHULzxDa4VDQu1D19iArDL8+JU="));
  }

  public static Map<String, FilecoinKey> getDefaultFilecoinAddressMap() {
    return ADDRESS_MAP;
  }
}
