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
package tech.pegasys.eth2signer.commandline.valueprovider;

import picocli.CommandLine.IDefaultValueProvider;
import picocli.CommandLine.Model.ArgSpec;

public class CascadingDefaultProvider implements IDefaultValueProvider {
  private final IDefaultValueProvider defaultValueProvider1;
  private final IDefaultValueProvider defaultValueProvider2;

  public CascadingDefaultProvider(
      final IDefaultValueProvider defaultValueProvider1,
      final IDefaultValueProvider defaultValueProvider2) {
    this.defaultValueProvider1 = defaultValueProvider1;
    this.defaultValueProvider2 = defaultValueProvider2;
  }

  @Override
  public String defaultValue(final ArgSpec argSpec) throws Exception {
    final String defaultValueFromProvider1 = defaultValueProvider1.defaultValue(argSpec);
    return defaultValueFromProvider1 == null
        ? defaultValueProvider2.defaultValue(argSpec)
        : defaultValueFromProvider1;
  }
}
