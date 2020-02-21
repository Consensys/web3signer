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
package tech.pegasys.eth2signer.core.http;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.tuweni.bytes.Bytes;

public class SigningRequestBody {

  private final Bytes domain;
  private final String publicKey;
  private final Bytes dataToSign;

  @JsonCreator
  public SigningRequestBody(
      @JsonProperty("publicKey") final String publicKey,
      @JsonProperty("domain") final String domain,
      @JsonProperty("dataToSign") final String dataToSign) {
    this.publicKey = publicKey;
    this.domain = Bytes.fromHexString(domain);
    this.dataToSign = Bytes.fromHexString(dataToSign);
  }

  public Bytes domain() {
    return domain;
  }

  @JsonGetter("publicKey")
  public String publicKey() {
    return publicKey;
  }

  public Bytes dataToSign() {
    return dataToSign;
  }

  @JsonGetter("domain")
  public String getDomain() {
    return domain.toHexString();
  }

  @JsonGetter("dataToSign")
  public String getDataToSign() {
    return dataToSign.toHexString();
  }
}
