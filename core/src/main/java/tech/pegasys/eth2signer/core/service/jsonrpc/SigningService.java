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
package tech.pegasys.eth2signer.core.service.jsonrpc;

import tech.pegasys.eth2signer.core.service.jsonrpc.exceptions.InvalidDataFormatException;
import tech.pegasys.eth2signer.core.service.jsonrpc.exceptions.SignerNotFoundException;
import tech.pegasys.eth2signer.core.service.operations.PublicKeys;
import tech.pegasys.eth2signer.core.service.operations.SignerForIdentifier;
import tech.pegasys.eth2signer.core.service.operations.Upcheck;

import java.util.List;
import java.util.Optional;

import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcMethod;
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcParam;
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcService;

@JsonRpcService
public class SigningService {
  private final Upcheck upcheck = new Upcheck();
  private final PublicKeys publicKeys;
  private final List<SignerForIdentifier<?>> signerForIdentifierList;

  public SigningService(
      final PublicKeys publicKeys, final List<SignerForIdentifier<?>> signerForIdentifierList) {
    this.publicKeys = publicKeys;
    this.signerForIdentifierList = signerForIdentifierList;
  }

  @JsonRpcMethod("public_keys")
  public String[] publicKeys() {
    return publicKeys.list().toArray(String[]::new);
  }

  @JsonRpcMethod("sign")
  public String sign(
      @JsonRpcParam("identifier") final String identifier,
      @JsonRpcParam("data") final String data) {

    try {
      return signerForIdentifierList.stream()
          .map(signerForIdentifier -> signerForIdentifier.sign(identifier, data))
          .flatMap(Optional::stream)
          .findFirst()
          .orElseThrow(
              () ->
                  new SignerNotFoundException(
                      "Signer not found for identifier", new String[] {data}));
    } catch (final IllegalArgumentException e) {
      throw new InvalidDataFormatException(e.getMessage());
    }
  }

  @JsonRpcMethod
  public String upcheck() {
    return upcheck.status();
  }
}
