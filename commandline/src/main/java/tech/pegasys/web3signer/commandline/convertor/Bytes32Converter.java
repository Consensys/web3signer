/*
 * Copyright 2024 ConsenSys AG.
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
package tech.pegasys.web3signer.commandline.convertor;

import org.apache.tuweni.bytes.Bytes32;
import picocli.CommandLine;

public class Bytes32Converter implements CommandLine.ITypeConverter<Bytes32> {
  @Override
  public Bytes32 convert(final String hexString) throws Exception {
    try {
      return Bytes32.fromHexString(hexString);
    } catch (final Exception e) {
      throw new CommandLine.TypeConversionException("Invalid hex string: " + hexString);
    }
  }
}
