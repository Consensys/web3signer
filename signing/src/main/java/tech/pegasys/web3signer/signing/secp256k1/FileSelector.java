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
package tech.pegasys.web3signer.signing.secp256k1;

import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Path;

/*
A set of rules regarding how key files are to be selected.
Should be overridden by the calling application to define their naming
convention.
 */
public interface FileSelector<T> {
  Filter<Path> getConfigFilesFilter(T selectionCriteria);
}
