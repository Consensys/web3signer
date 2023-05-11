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
package tech.pegasys.web3signer.dsl.besu;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.io.Resources;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BesuNodeFactory {
  private static final Logger LOG = LogManager.getLogger();
  private static final Path runDirectory = Path.of(System.getProperty("user.dir"));
  private static final Path besuInstallDir =
      Path.of(System.getProperty("besuInstallDir", runDirectory.resolve("build/besu").toString()));
  private static final Path executablePath = besuInstallDir.resolve("bin/besu");

  public static BesuNode create(final BesuNodeConfig config) {
    checkBesuInstallation();

    final List<String> params = new ArrayList<>();
    params.add(executablePath.toString());

    if (config.getGenesisFile().isEmpty()) {
      params.add("--network");
      params.add("DEV");
    } else {
      params.add("--genesis-file");
      params.add(Resources.getResource(config.getGenesisFile().get()).getPath());
    }
    params.add("--data-path");
    params.add(config.getDataPath().toString());
    params.add("--logging");
    params.add("DEBUG");
    params.add("--miner-enabled");
    params.add("--miner-coinbase");
    params.add("1b23ba34ca45bb56aa67bc78be89ac00ca00da00");
    params.add("--host-whitelist");
    params.add("*");
    params.add("--p2p-port");
    params.add("0");
    params.add("--rpc-http-enabled=true");
    params.add("--rpc-http-port");
    params.add("0");
    params.add("--rpc-http-host");
    params.add(config.getHostName());
    params.add("--rpc-ws-enabled=true");
    params.add("--rpc-ws-port");
    params.add("0");
    params.add("--rpc-ws-host");
    params.add(config.getHostName());
    params.add("--rpc-http-apis");
    params.add("ETH,NET,WEB3,EEA");
    params.add("--min-gas-price=0");
    params.add("--privacy-enabled");
    params.add("--privacy-public-key-file");
    params.add(privacyPublicKeyFilePath());

    config.getCors().ifPresent(cors -> params.addAll(List.of("--rpc-http-cors-origins", cors)));

    params.addAll(config.getAdditionalCommandLineArgs());

    return new BesuNode(config, params.toArray(String[]::new), environment());
  }

  private static Map<String, String> environment() {
    if (Boolean.getBoolean("debugSubProcess")) {
      return Map.of(
          "JAVA_OPTS",
          "-Xdebug -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005 ");
    }
    return Collections.emptyMap();
  }

  private static void checkBesuInstallation() {
    LOG.info("Run Dir: {}", runDirectory);
    LOG.info("Besu Install Dir: {}", besuInstallDir);
    LOG.info("Executable Path: {}", executablePath);
    LOG.info("Exists? {}", executablePath.toFile().exists());

    if (!executablePath.toFile().exists()) {
      LOG.error(
          "Besu binary doesn't exist. Either run 'gradle extractBesu' or set system property 'besuInstallDir'");
      throw new IllegalStateException("Besu binary doesn't exist " + executablePath);
    }
  }

  private static String privacyPublicKeyFilePath() {
    final URL resource = Resources.getResource("besu/enclave_key.pub");
    return resource.getPath();
  }
}
