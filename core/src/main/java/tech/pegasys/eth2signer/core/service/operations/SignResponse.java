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
package tech.pegasys.eth2signer.core.service.operations;

public class SignResponse {
  public enum Type {
    SIGNER_NOT_FOUND,
    INVALID_DATA,
    SIGNATURE_OK
  }

  private Type responseType;
  private String response;

  public SignResponse(final Type responseType, final String response) {
    this.responseType = responseType;
    this.response = response;
  }

  public Type getResponseType() {
    return responseType;
  }

  public String getResponse() {
    return response;
  }
}
