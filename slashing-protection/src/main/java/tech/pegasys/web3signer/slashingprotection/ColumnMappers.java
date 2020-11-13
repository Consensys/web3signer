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
package tech.pegasys.web3signer.slashingprotection;

import java.lang.reflect.Type;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMapperFactory;

public class ColumnMappers {

  public static class BytesColumnMapper implements ColumnMapperFactory {

    @Override
    public Optional<ColumnMapper<?>> build(final Type type, final ConfigRegistry config) {
      if (!Bytes.class.equals(GenericTypes.getErasedType(type))) {
        return Optional.empty();
      }

      return Optional.of(
          (ColumnMapper<Bytes>)
              (r, columnNumber, ctx) ->
                  r.getBytes(columnNumber) == null ? null : Bytes.wrap(r.getBytes(columnNumber)));
    }
  }

  public static class Bytes32ColumnMapper implements ColumnMapperFactory {

    @Override
    public Optional<ColumnMapper<?>> build(final Type type, final ConfigRegistry config) {
      if (!Bytes32.class.equals(GenericTypes.getErasedType(type))) {
        return Optional.empty();
      }

      return Optional.of(
          (ColumnMapper<Bytes>)
              (r, columnNumber, ctx) ->
                  r.getBytes(columnNumber) == null ? null : Bytes32.wrap(r.getBytes(columnNumber)));
    }
  }

  public static class UInt64ColumnMapper implements ColumnMapperFactory {

    @Override
    public Optional<ColumnMapper<?>> build(final Type type, final ConfigRegistry config) {
      if (!UInt64.class.equals(GenericTypes.getErasedType(type))) {
        return Optional.empty();
      }

      return Optional.of(
          (ColumnMapper<UInt64>)
              (r, columnNumber, ctx) ->
                  r.getBigDecimal(columnNumber) == null
                      ? null
                      : UInt64.valueOf(r.getBigDecimal(columnNumber).toBigInteger()));
    }
  }
}
