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
package tech.pegasys.web3signer.core.service.jsonrpc;

import tech.pegasys.web3signer.common.JacksonSerializers.Base64Deserialiser;
import tech.pegasys.web3signer.common.JacksonSerializers.Base64Serialiser;
import tech.pegasys.web3signer.common.JacksonSerializers.BigIntegerDecimalStringDeserialiser;
import tech.pegasys.web3signer.common.JacksonSerializers.BigIntegerDecimalStringSerialiser;
import tech.pegasys.web3signer.common.JacksonSerializers.NumberUInt64Deserializer;
import tech.pegasys.web3signer.common.JacksonSerializers.NumberUInt64Serialiser;

import java.math.BigInteger;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;

public class FilecoinJsonRpcModule extends SimpleModule {

  public FilecoinJsonRpcModule() {
    super("FilecoinJsonRpcModule");
    addDeserializer(Bytes.class, new Base64Deserialiser());
    addSerializer(Bytes.class, new Base64Serialiser());
    addDeserializer(UInt64.class, new NumberUInt64Deserializer());
    addSerializer(UInt64.class, new NumberUInt64Serialiser());
    addDeserializer(BigInteger.class, new BigIntegerDecimalStringDeserialiser());
    addSerializer(BigInteger.class, new BigIntegerDecimalStringSerialiser());
  }
}
