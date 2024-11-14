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
package tech.pegasys.web3signer.core.service.http.handlers.commitboost.datastructure;

import tech.pegasys.teku.infrastructure.json.types.DeserializableTypeDefinition;
import tech.pegasys.teku.infrastructure.ssz.schema.SszPrimitiveSchemas;
import tech.pegasys.teku.infrastructure.ssz.schema.collections.impl.SszByteVectorSchemaImpl;
import tech.pegasys.teku.infrastructure.ssz.schema.json.SszPrimitiveTypeDefinitions;
import tech.pegasys.teku.infrastructure.ssz.tree.TreeNode;

public class SszSECPPublicKeySchema extends SszByteVectorSchemaImpl<SszSECPPublicKey> {
  private static final int SECP_COMPRESSED_PUBLIC_KEY_SIZE = 33;

  public static final SszSECPPublicKeySchema INSTANCE = new SszSECPPublicKeySchema();

  private SszSECPPublicKeySchema() {
    super(SszPrimitiveSchemas.BYTE_SCHEMA, SECP_COMPRESSED_PUBLIC_KEY_SIZE);
  }

  @Override
  protected DeserializableTypeDefinition<SszSECPPublicKey> createTypeDefinition() {
    return SszPrimitiveTypeDefinitions.sszSerializedType(this, "Bytes33 hexadecimal");
  }

  @Override
  public SszSECPPublicKey createFromBackingNode(final TreeNode node) {
    return new SszSECPPublicKey(node);
  }
}
