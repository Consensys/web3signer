/*
 * Copyright 2019 ConsenSys AG.
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
package tech.pegasys.eth2signer.core.multikey;

import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.io.Resources;

public class MetadataFileFixture {

  public static final String CONFIG_FILE_EXTENSION = ".yaml";
  public static String LOWERCASE_ADDRESS = "627306090abab3a6e1400e9345bc60c78a8bef57";

  public static String PREFIX_MIXEDCASE_ADDRESS = "f17f52151ebef6c7334fad080c5704d77216b732";
  public static String PREFIX_MIXEDCASE_FILENAME =
      "UTC--2019-10-23T04-00-04.860366000Z--" + PREFIX_MIXEDCASE_ADDRESS + CONFIG_FILE_EXTENSION;

  public static String SUFFIX_ADDRESS = "627306090abab3a6e1400e9345bc60c78a8bef60";
  public static String PREFIX_LOWERCASE_DUPLICATE_ADDRESS =
      "627306090abab3a6e1400e9345bc60c78a8bef61";
  public static String PREFIX_LOWERCASE_DUPLICATE_FILENAME_1 =
      "duplicate_foo_" + PREFIX_LOWERCASE_DUPLICATE_ADDRESS + CONFIG_FILE_EXTENSION;
  public static String PREFIX_LOWERCASE_DUPLICATE_FILENAME_2 =
      "duplicate_bar_" + PREFIX_LOWERCASE_DUPLICATE_ADDRESS + CONFIG_FILE_EXTENSION;

  static void copyMetadataFileToDirectory(
      final Path configsDirectory, final String metadataFilename) {
    final Path newMetadataFile = configsDirectory.resolve(metadataFilename);

    try {
      Files.copy(
          Path.of(Resources.getResource("metadata-configs").toURI()).resolve(metadataFilename),
          newMetadataFile);
    } catch (Exception e) {
      fail(
          "Error copying metadata files configDirectory="
              + configsDirectory
              + " metadataFilename="
              + metadataFilename,
          e);
    }
  }
}
