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
package tech.pegasys.web3signer.core.service.http;

/** Operation IDs as defined in web3signer.yaml */
@Deprecated(forRemoval = true)
public enum OpenApiOperationsId {
  ETH2_SIGN,
  ETH1_SIGN,
  ETH2_LIST,
  ETH1_LIST,
  RELOAD,
  UPCHECK,
  HEALTHCHECK,
  KEYMANAGER_LIST,
  KEYMANAGER_IMPORT,
  KEYMANAGER_DELETE,
}
