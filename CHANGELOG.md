# Changelog

## Next Version

### Bugs fixed
- Update postgresql to fix CVE-2024-1597
- Fix cached gvr to be thread-safe during first boot. [#978](https://github.com/Consensys/web3signer/issues/978)

## 24.2.0

This is a required update for Mainnet users containing the configuration for the Deneb upgrade on March 13th. This update is required for Gnosis Deneb network upgrade on March 11th. For all other networks, this update is optional.

### Upcoming Breaking Changes
- `--Xworker-pool-size` cli option will be removed in a future release. This option has been replaced with `--vertx-worker-pool-size`

### Features Added
- Add Deneb configuration for Mainnet [#971](https://github.com/Consensys/web3signer/pull/971)
- Improve Key Manager API import operation to use parallel processing instead of serial processing

### Bugs fixed
- Ensure that Web3Signer stops the http server when a sigterm is received

## 24.1.1

This is an optional release for mainnet Ethereum and it includes the updated network configuration for the Sepolia, Holesky and Chiado Deneb forks.

## 24.1.0

This is an optional release for mainnet Ethereum and it includes the updated network configuration for the Goerli Deneb fork.

### Upcoming Breaking Changes
- `--Xworker-pool-size` cli option will be removed in a future release. This option has been replaced with `--vertx-worker-pool-size`.

### Bugs fixed
- Update reactor-netty-http to fix CVE-2023-34062

### Features Added
- Add Deneb configuration for Goerli [#960](https://github.com/Consensys/web3signer/pull/960)

## 23.11.0
### Upcoming Breaking Changes
- `--Xworker-pool-size` cli option will be removed in a future release. This option has been replaced with `--vertx-worker-pool-size`.

### Bugs fixed
- Update netty to fix CVE-2023-44487

### Features Added
- Google Cloud Secret Manager bulk loading support for BLS keys in eth2 mode via PR [#928](https://github.com/Consensys/web3signer/pull/928) contributed by [Sergey Kisel](https://github.com/skisel-bt).
- Removed hidden option `--Xtrusted-setup` as Web3Signer does not need KZG trusted setup file anymore.
- Make Vert.x worker pool size configurable using cli option `--vertx-worker-pool-size` (replaces the now deprecated: `--Xworker-pool-size`). [#920](https://github.com/Consensys/web3signer/pull/920)

## 23.9.1

### Breaking Changes
- Remove --validator-ids option from watermark-repair subcommand [#909](https://github.com/Consensys/web3signer/pull/909)

### Features Added
- Aws bulk loading for secp256k1 keys in eth1 mode [#889](https://github.com/Consensys/web3signer/pull/889)
- Add High Watermark functionality [#696](https://github.com/Consensys/web3signer/issues/696) 
  - Update `watermark-repair` subcommand with new options `--set-high-watermark`, `--remove-high-watermark` [#912](https://github.com/Consensys/web3signer/pull/912)
  - Add GET `/highWatermark` to eth2 endpoints [#908](https://github.com/Consensys/web3signer/pull/908)
- Add network configuration for revised Holesky testnet

## 23.9.0

### Features Added
- Signing support for BlobSidecar and BlindedBlobSidecar in Deneb fork.
- Add `--azure-response-timeout` to allow request response timeout to be configurable, the field `timeout` is also accepted in the Azure metadata file. [#888](https://github.com/Consensys/web3signer/pull/888)
- Bulk load Ethereum v3 wallet files in eth1 mode.
- Eth2 Signing request body now supports both `signingRoot` and the `signing_root` property
- Add network configuration for Holesky testnet
- Add `eth_signTypedData` RPC method under the eth1 subcommand. [#893](https://github.com/Consensys/web3signer/pull/893)

### Bugs fixed
- Upcheck was using application/json accept headers instead text/plain accept headers

## 23.8.1

### Bugs fixed
- Update grpc library to version 1.57.2 to fix CVE-2023-33953

## 23.8.0

### Breaking Changes
- Use Java 17 for build and runtime. Remove Java 11 variant of docker image. zip/tar.gz distributions will require Java 17 or above to run Web3Signer.
- Eth2 Azure command line option --azure-secrets-tags is now deprecated and is replaced with --azure-tags. The --azure-secrets-tags option will be removed in a future release.

- 
### Features Added
- Add support for SECP256K1 remote signing using AWS Key Management Service. [#501](https://github.com/Consensys/web3signer/issues/501)
- Azure bulk mode support for loading multiline (`\n` delimited, up to 200) keys per secret.
- Hashicorp connection properties can now override http protocol to HTTP/1.1 from the default of HTTP/2. [#817](https://github.com/ConsenSys/web3signer/pull/817)
- Add --key-config-path as preferred alias to --key-store-path [#826](https://github.com/Consensys/web3signer/pull/826)
- Add eth_signTransaction RPC method under the eth1 subcommand [#822](https://github.com/ConsenSys/web3signer/pull/822)
- Add eth_sendTransaction RPC method under the eth1 subcommand [#835](https://github.com/Consensys/web3signer/pull/835)
- Add EIP-1559 support for eth1 public transactions for eth_sendTransaction and eth_signTransaction [#836](https://github.com/Consensys/web3signer/pull/836)
- Add Azure bulk loading for secp256k1 keys in eth1 mode [#850](https://github.com/Consensys/web3signer/pull/850)
- Added Gnosis configuration for the 🦉 CAPELLA 🦉 network fork due at epoch 648704, UTC Tue 01/08/2023, 11:34:20 [#865](https://github.com/Consensys/web3signer/pull/865)
- Java 17 for build and runtime. [#870](https://github.com/Consensys/web3signer/pull/870)
- Update internal teku library to 23.8.0 [#876](https://github.com/Consensys/web3signer/pull/876)
- Add support for [Lukso network](https://lukso.network/) `--network=lukso`
- Deprecate `signingRoot` while currently supporting both `signingRoot` and `signing_root` in Eth2 signing request body.  

### Bugs fixed
- Support long name aliases in environment variables and YAML configuration [#825](https://github.com/Consensys/web3signer/pull/825)

---
## 23.6.0

As part of our ongoing commitment to deliver the best remote signing solutions, we are announcing a change in our product offerings.

We have decided to deprecate our [EthSigner](https://github.com/Consensys/EthSigner) product to focus our efforts on enhancing Web3Signer, our newly comprehensive remote signing solution. This is rooted in our strategy to streamline our offerings and focus on a single, robust product that will provide functionality for both transaction and Ethereum validator signing. We hope this makes it applicable to all your use-cases like public Ethereum signing, staking infrastructure offerings, and in private network contexts.

Rest assured, we are not dropping existing EthSigner functionality. We are updating Web3Signer to incorporate the functionalities of EthSigner alongside everything else in Web3Signer. We will ensure a smooth transition by maintaining EthSigner with necessary patches for an additional six months. We hope this provides ample time for any necessary migration to Web3Signer.

**We have begun adding EthSigner functionality to Web3Signer. This is a work in progress and not complete.**  

### Features Added
- Optional Azure bulk loading tags support using cli option `--azure-secrets-tags`.
- Support Prometheus Push Gateway Metrics [#796](https://github.com/ConsenSys/web3signer/pull/796)
- Cache Genesis Validators Root (GVR) in-memory on first database lookup. This would eliminate further database lookups 
for GVR during sign operations and improve their performance. [#600](https://github.com/ConsenSys/web3signer/issues/600)
- Add RPC proxy support to execution client under the eth1 subcommand [#775](https://github.com/ConsenSys/web3signer/pull/775)
- Add eth_accounts RPC method under the eth1 subcommand [#784](https://github.com/ConsenSys/web3signer/pull/784)

### Bugs Fixed
- Upgrade jackson and vertx to upgrade snakeyaml to 2.0 to fix CVE-2022-1471
- Fixed handling of very large number (30,000+) of signing metadata files with Hashicorp connection by introducing 
experimental flag to disable parallel processing `--Xmetadata-files-parallel-processing-enabled`. 
[#794](https://github.com/ConsenSys/web3signer/pull/794)
- Fixed startup error with web3signer where openAPI spec cannot be loaded [#772](https://github.com/ConsenSys/web3signer/issues/772)
- Removed unmaintained and out-of-date helm chart [#802](https://github.com/ConsenSys/web3signer/pull/802)

---
## 23.3.1
### Features Added
- Add support for Capella milestone in Mainnet
- Enhanced Healthcheck endpoint reporting status of loading of signers keys [#738](https://github.com/ConsenSys/web3signer/pull/738)
- Optional AWS endpoint overriding for bulk loading `--aws-endpoint-override`. Useful for local testing against localstack. [#730](https://github.com/ConsenSys/web3signer/issues/730)

### Bugs Fixed
- Update of Azure libraries (transitive via signers library) and manual override to fix CVE-2023-1370
- Fix issue with some third party libraries not including logs in the web3signer logs due missing slf4j2 library

---
## 23.3.0
### Breaking Changes
- Slashing protection database schema has been updated to support indexes with bigint type and after the upgrade will no longer work with older versions of Web3Signer.

### Features Added
- Add support for Capella milestone in Goerli
- Introduced cli option `--key-store-config-file-max-size` to change the default value of configuration file size. [#719](https://github.com/ConsenSys/web3signer/issues/719)

### Bugs fixed
- Fix issue with slashing protection database failing once reaching max integer index value [#705](https://github.com/ConsenSys/web3signer/issues/705)
- Fix issue with Web3Signer startup when configuration file size is greater than 3 MB [#719](https://github.com/ConsenSys/web3signer/issues/719)

---
## 23.2.1
### Features Added
- Add support for Capella milestone in Sepolia
- Add Block signing support for Capella

### Bugs fixed
- Upgrade to Vertx 4.3.8 to address CVE-2023-24815
- Updated docker image with latest libssl3

---
## 23.2.0
### Features Added
- AWS Secrets Manager bulkload mode can now load multiple keys from same secret where keys are separated by line terminating 
character (such as `\n`). [#706](https://github.com/ConsenSys/web3signer/issues/706)

---
## 23.1.0
### Features Added
- Multiple Signing Key configurations can be specified in single YAML file using triple-dash `---` separator. 
[#689](https://github.com/ConsenSys/web3signer/issues/689)
- Reloading of signing key configuration file (via `/reload` endpoint) will process new or modified configuration files. [#689](https://github.com/ConsenSys/web3signer/issues/689)
- Updated Teku libraries version to 22.12.0

### Bugs Fixed
- Upgrade various dependencies including netty libraries to address CVE-2022-41881 and CVE-2022-41915

---
## 22.11.0
### Breaking Changes
- Slashing protection imports will now only fail for an individual validator instead for all validators allowing partial 
import if there is valid and invalid data.

### Features Added
- Introduced cli option to specify Hikari configuration for pruning database connection [#661](https://github.com/ConsenSys/web3signer/issues/661)
- Better database pruning default values: Pruning enabled by default with 
`slashing-protection-pruning-epochs-to-keep = 250`, `slashing-protection-pruning-at-boot-enabled = false` and 
`slashing-protection-pruning-interval = 12`.
- Improved performance for slashing protection import
- Introduced experimental cli option `--Xslashing-protection-db-connection-pool-enabled` to disable internal database 
connection pool (Hikari) to allow using external database connection pool such as pgBouncer. 
`--slashing-protection-db-pool-configuration-file` and `--slashing-protection-pruning-db-pool-configuration-file` can be
reused to specify PG Datasource properties. [#662](https://github.com/ConsenSys/web3signer/issues/662)
- Added new subcommand watermark-repair to update low watermarks

---

## 22.10.0

### Features Added
- Log eth2 network configuration on startup [#640](https://github.com/ConsenSys/web3signer/issues/640)
- Updated internal Teku libraries to 22.10.1
- Updated HikariCP to 5.0.1

### Bugs Fixed
- Upgrade jackson libraries to fix CVE-2022-25857, CVE-2022-38751, CVE-2022-38752 and CVE-2022-42003
- Upgrade protobufs to fix CVE-2022-3171

## 22.8.1
### Features Added
- Updated internal Teku libraries to 22.8.1. This update includes Bellatrix network upgrade and merge transition configuration for Mainnet.

## 22.8.0
### Features Added
- Added health check endpoint [#538](https://github.com/ConsenSys/web3signer/issues/538). 
- Introduced `--slashing-protection-db-health-check-timeout-milliseconds` to specify the timeout of the slashing db health check procedure.
- Introduced `--slashing-protection-db-health-check-interval-milliseconds` to specify the interval between slashing db health check procedures.
- Updated Teku libraries version (support for Prater/Görli merge).

### Bugs Fixed
- Updated to PostgreSQL JDBC driver to 42.4.1. Resolves a potential vulnerability CVE-2022-31197.

## 22.7.0
### Features Added
- Support register validator API endpoint [#577](https://github.com/ConsenSys/web3signer/issues/577)
- Version information available in metrics through `process_release` [#480](https://github.com/ConsenSys/web3signer/issues/480)
---

## 22.6.0
### Features Added
- Support for Sepolia network (updated Teku support libraries).
- Added new metric `eth2_slashingprotection_database_duration` to track time spent performing database queries during either block or attestation signing operations
- Private keys bulk loading from AWS Secrets Manager via cli options in eth2 mode [#499](https://github.com/ConsenSys/web3signer/issues/499)

### Bugs Fixed
- Fix issue where signing_signers_loaded_count metric didn't update after refresh endpoint was used to update loaded keys
---

## 22.5.1
### Breaking Changes
- Removed network definition for kintsugi testnet

### Features Added
- Eth2 keystore bulk loading allowing a directory of keystores to be loaded without config files
- Added support for ropsten testnet

### Bugs Fixed
- Fixes issue when using key manager delete API failed when there was no slashing protection data [#537](https://github.com/ConsenSys/web3signer/issues/537)

---
## 22.5.0
### Breaking Changes
- ETH2 Mode - block signing request (BLOCK_V2), starting from BELLATRIX fork, use block_header instead of block. [#547](https://github.com/ConsenSys/web3signer/pull/547)

### Features Added
- Added support for optimized block signing requests starting from Bellatrix fork. [#437](https://github.com/ConsenSys/web3signer/issues/437)
- Early access: Support for Gnosis network in Eth2 mode. `--network gnosis`

### Bugs Fixed
- Keys loaded using the AWS secrets manager with environment config didn't work when using web identity tokens due to missing sts library.

---
## 22.4.1

### Features Added
- Update various library dependencies

---
## 22.4.0

### Breaking Changes
- Because the web3signer docker image uses the latest LTS tag (ubuntu:latest), the container host may require an update to the latest container runtime. See [Ubuntu bug](https://bugs.launchpad.net/ubuntu/+source/libseccomp/+bug/1916485) for more details.

### Features Added
- Migrate from the deprecated `vertx-web-api-contract` module to `vertx-web-openapi` [#506](https://github.com/ConsenSys/web3signer/pull/506)
- Migrate jackson `ObjectMapper` instances to `JsonMapper` and `YamlMapper` builders to resolve deprecation warnings [#507](https://github.com/ConsenSys/web3signer/pull/507)
- Add `iputils-ping` and `net-tools` to docker image to support waiting for dependent services in tools such as docker-compose and Kubernetes [#525](https://github.com/ConsenSys/web3signer/pull/535)
- Updated Teku libraries to provide support for `kiln` network
- Support for BLS private keys in AWS Secrets Manager
- Early access support for [eth2 Key Manager API](https://ethereum.github.io/keymanager-APIs/)

### Bugs Fixed
- Upgrade Vertx to 4.x, signers to 2.0.0 and various other dependencies to latest versions [#503](https://github.com/ConsenSys/web3signer/pull/503)
- DB scripts executed in numeric order (instead of alphanumeric) when using docker instead of flyway to execute [#526](https://github.com/ConsenSys/web3signer/pull/526)

---
## 21.10.6
### Bugs Fixed
- Updated to PostgreSQL JDBC driver to 42.3.3. Resolves a potential vulnerability CVE-2022-21724.

---
## 21.10.5
### Bugs Fixed
- Updated to log4j 2.17.1. Resolves two potential vulnerabilities which are only exploitable when using custom log4j configurations that are either writable by untrusted users or log data from the `ThreadContext`.

---
## 21.10.4
### Bugs Fixed
- Updated log4j to 2.17.0 to mitigate potential DOS vulnerability when the logging configuration uses a non-default Pattern Layout with a Context Lookup.

---
## 21.10.3

### Bugs fixed
- Updated log4j to 2.16.0 to mitigate JNDI attack via thread context. 
---

## 21.10.2

### Bugs fixed
- Fix multi-arch JDK17 variant docker image to bundle Java 17 instead of Java 11

## 21.10.1

### Features Added
- Docker images are now published with multi-arch support including Linux/amd64 and Linux/arm64
- The default docker image now uses JDK 17 instead of 11. The JDK 11 image is still available with the version suffix `-jdk11`

### Breaking Changes
- The docker image now uses `web3signer` as user/group instead of `root` which may result in compatibility/permissions issues with existing directory mounts.  

### Bugs Fixed
- Updated log4j and explicitly disabled format message lookups.

---

## 21.10.0

### Features Added
- Upgrade to signers 1.0.19 allows empty password files to be read when creating a Signer [#432](https://github.com/ConsenSys/web3signer/pull/432)
- Upgrade Teku libraries version to 21.9.2 to provide support for Altair fork in mainnet [#435](https://github.com/ConsenSys/web3signer/pull/435)

### Breaking Changes
- Upgrade to signers 1.0.19 removes support for deprecated SECP256K1 curve in Azure remote signing [#432](https://github.com/ConsenSys/web3signer/pull/432)

## 21.8.1

### Features Added
- Added sign type BLOCK_V2 to support block signing for Phase0, Altair and future forks (Eth2 mode). BLOCK is not removed for 
backward compatibility with PHASE0 blocks.
- Upgraded Teku libraries to 21.8.2 which added support for Altair upgrade on Prater testnet at epoch 36660.

### Bugs fixed
- Unable to sign blocks on testnet Pyrmont after Altair fork. (Thanks to [Sephiroth](https://github.com/3eph1r0th) for reporting it.)

## 21.8.0

### Features Added
- Upgraded Teku libraries to 21.8.1. Added support for Altair upgrade on the Pyrmont testnet at epoch 61650.

### Bug fixed
- Spelling mistake fixed in Eth2 OpenApi spec

## 21.7.0

### Breaking changes
- `--network` flag for `eth2` subcommand is now mandatory and defaults to `mainnet`. Use appropriate network when running web3signer for a testnet.
- Database migration scripts (V8__*.sql and V9__*.sql) are required to be executed for this release if slashing protection is used.

### Features Added
- Introduced `--slashing-protection-db-pool-configuration-file` to specify Hikari connection pool configuration file.
- Upgraded gradle and various plugin versions. Switched to new dependency license reporting plugin. Project can now be compiled against JDK 16.
- Introduced --network cli option for Eth2 mode. Defaults to mainnet. Should match the option used by Teku at runtime.
- Upgraded Teku libraries.
- Eth2 slashing protection now has an additional safeguard that prevents multiple signed blocks or attestations being inserted using database constraints.
- Use adoptopenjdk/openjdk11:x86_64-ubuntu-jre-11.0.11_9 as docker base image.

### Bugs fixed
- Fixed transaction deadlock at start up during same validators registration (>10000) from multiple web3signer instances.

## 21.3.0

### Features Added
- Metrics on Vertx event loop and worker thread pools

### Bugs Fixed
- Eth2 slashing protection pruning on startup now runs on a separate thread, so it doesn't block application startup and http requests
- Eth2 slashing protection pruning was deleting the incorrect amount of data in some cases

## 21.2.0

### Features Added
- Reload API endpoint to load new keys
- Slashing protection database pruning for Eth2
- Publish binaries to Cloudsmith
- Resolve Signers from Cloudsmith

### Bugs Fixed
- Fixed build failure when checked out as a shallow clone. Shallow clones are still not recommended as the version number cannot be determined correctly.
- Change reference tests git submodule to https so a github account isn't required to build web3signer

## 21.1.0

### Features Added
- Azure secrets managed identity mode
- Check that database matches expected version on startup
- Added basic Eth2 grafana dashboard (https://grafana.com/grafana/dashboards/13687)
- Updated openjdk docker base image

### Bugs Fixed
- Improve query performance of attestations and blocks by adding indexes
- Azure configuration files could only be parallel-processed in a batch of 10 due to a bug in Azure libraries
- Incorrect options in the openapi spec for Eth2 signing API
- Fix the external link for documentation on OpenAPI documentation

## 20.11.0

### Features Added

- Interlock/Armory II HSM keystore support
- Eth2 slashing protection data able to be exported and imported from json file (Interchange format V5)
- Eth2 signing API returned body matches incoming request content-type in either plain-text or json
- Only able to sign for a single Genesis validators root (which is defined by the first received request after creation)
- Do not sign below watermark (regardless of if matching existing entry)

### Bugs Fixed
- Eth2 slashing protection metrics category was not working on CLI
- Update Filecoin RPC to be compatible with Lotus remote wallet API
- Eth2 slashing protection returns a 412 http status code for a slashing violation
- Signing with empty slashing database would sometimes fail due multiple genesis validator root values inserting concurrently 
- Resolved help text anomalies on command line

## 0.2.0

### Features Added
- Separate application into eth2, eth and Filecoin commands that can be run independently
- Eth2 slashing protection. Requires a PostgreSQL database to store eth2 signed blocks and attestations
- Ethereum secp256k1 signing of data
- Use yaml configuration of signing keys
- Support for Filecoin JSON RPCs
- Azure secret vault support for eth2 keys to load all secrets from a given vault
- Added a Prometheus metrics endpoint
- Use native BLS signing and verification
- Added helm charts

## 0.1.0

Initial release of Eth2Signer

### Features Added
- Signing of data using a BLS key. Supports BLS private keys in unencrypted files, BLS12-381 keystore files and keys in Hashicorp 
- TLS support for rest API
- OpenAPI documentation
