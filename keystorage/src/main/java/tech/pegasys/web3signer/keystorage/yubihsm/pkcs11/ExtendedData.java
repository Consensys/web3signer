/*
 * Copyright 2023 ConsenSys AG.
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
package tech.pegasys.web3signer.keystorage.yubihsm.pkcs11;

import iaik.pkcs.pkcs11.objects.Attribute;
import iaik.pkcs.pkcs11.objects.ByteArrayAttribute;
import iaik.pkcs.pkcs11.objects.Data;
import org.apache.tuweni.bytes.Bytes;

/** Extends Data to provide support for ID (for findObjectInit) which is used by YubiHSM */
class ExtendedData extends Data {

  public ExtendedData(final short opaqueObjId) {
    super();
    // The ID (CKA_ID) attribute - used by YubiHSM PKCS11 module.
    final ByteArrayAttribute id = new ByteArrayAttribute(Attribute.ID);
    id.setByteArrayValue(Bytes.ofUnsignedShort(opaqueObjId).toArrayUnsafe());
    attributeTable.put(Attribute.ID, id);
  }
}
