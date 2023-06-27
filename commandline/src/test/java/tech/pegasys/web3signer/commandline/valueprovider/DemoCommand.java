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
package tech.pegasys.web3signer.commandline.valueprovider;

import java.util.List;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "demo",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "demo command")
public class DemoCommand {
  @Option(names = "--x", description = "x")
  int x;

  @Option(names = "-y", description = "y")
  int y;

  @Option(names = "--name", description = "Name")
  String name;

  @Option(
      names = {"--alias", "--aliases"},
      description = "Aliases")
  String alias;

  @Command(name = "country", description = "Country Codes")
  static class SubCommand {
    @Option(names = "--codes", split = ",")
    List<String> countryCodes;

    @Option(
        names = {"--subalias", "--subaliases"},
        description = "Subcommand Alias")
    String subalias;
  }
}
