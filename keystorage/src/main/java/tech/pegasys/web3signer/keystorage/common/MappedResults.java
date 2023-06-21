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
package tech.pegasys.web3signer.keystorage.common;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Contains Collection of Secret value result and count of errors. */
public class MappedResults<R> {
  private final Collection<R> values;
  private int errorCount;

  MappedResults(final Collection<R> values, final int errorCount) {
    this.values = values;
    this.errorCount = errorCount;
  }

  public static <R> MappedResults<R> errorResult() {
    return new MappedResults<>(Collections.emptyList(), 1);
  }

  public static <R> MappedResults<R> newSetInstance() {
    return new MappedResults<>(new HashSet<>(), 0);
  }

  public static <R> MappedResults<R> newInstance(final Collection<R> values, final int errorCount) {
    return new MappedResults<>(values, errorCount);
  }

  public static <R> MappedResults<R> merge(
      final MappedResults<R> first, final MappedResults<R> second) {
    final List<R> combinedList =
        Stream.concat(first.values.stream(), second.values.stream()).collect(Collectors.toList());
    final int errorCount = first.errorCount + second.errorCount;
    return new MappedResults<>(combinedList, errorCount);
  }

  public void mergeErrorCount(final int otherErrorCount) {
    this.errorCount += otherErrorCount;
  }

  public Collection<R> getValues() {
    return values;
  }

  public int getErrorCount() {
    return errorCount;
  }
}
