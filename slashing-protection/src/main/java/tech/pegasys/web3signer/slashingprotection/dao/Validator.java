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
package tech.pegasys.web3signer.slashingprotection.dao;

import org.apache.tuweni.bytes.Bytes;

public class Validator {
  private int id;
  private Bytes publicKey;

  // needed for JDBI bean mapping
  public Validator() {}

  public Validator(final int id, final Bytes publicKey) {
    this.id = id;
    this.publicKey = publicKey;
  }

  public int getId() {
    return id;
  }

  public void setId(final int id) {
    this.id = id;
  }

  public Bytes getPublicKey() {
    return publicKey;
  }

  public void setPublicKey(final Bytes publicKey) {
    this.publicKey = publicKey;
  }

  @Override
  public String toString() {
    return "Validator{" + "id=" + id + ", publicKey=" + publicKey + '}';
  }
}
