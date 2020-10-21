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
package tech.pegasys.web3signer.slashingprotection.interchange;

import tech.pegasys.web3signer.common.JacksonSerializers.HexDeserialiser;
import tech.pegasys.web3signer.common.JacksonSerializers.HexSerialiser;
import tech.pegasys.web3signer.common.JacksonSerializers.StringUInt64Deserializer;
import tech.pegasys.web3signer.common.JacksonSerializers.StringUInt64Serialiser;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;

public class InterchangeModule extends SimpleModule {

  public InterchangeModule() {
    super("InterchangeModule");
    addDeserializer(Bytes.class, new HexDeserialiser());
    addSerializer(Bytes.class, new HexSerialiser());
    addDeserializer(UInt64.class, new StringUInt64Deserializer());
    addSerializer(UInt64.class, new StringUInt64Serialiser());
  }
}
