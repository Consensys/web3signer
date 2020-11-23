# Web3Signer OpenAPI Spec Publish

This directory contains NodeJS project which publishes Web3Signer OpenAPI specifications to 
[`gh-pages`](https://github.com/ConsenSys/web3signer/tree/gh-pages) branch via CI job after build and acceptanceTests. 
See `publishOpenApiSpec` job in `.circleci/config.yml`.

## Prerequisite 
The script assumes that the `gradle build` (from the root directory) has already been executed which prepares the 
Web3Signer specs at `core/build/resources/main/openapi/web3signer-<mode>.yaml` (refered as `specs` in this document).
Mode is eth2 and eth1. 

## Procedure
The script performs following tasks:

* Create `dist` directory (this folder will be pushed to `gh-pages` branch)
* Read spec's version i.e. `info.version`.
* Copy the specs to `dist` as `web3signer-<mode>-latest.yaml`.

For release version, it performs following additional steps (the release version do not have `-dev-` in it)

* Copy the spec to `dist` as `web3signer-<mode>-<version>.yaml`
* Fetch `https://github.com/ConsenSys/web3signer/raw/gh-pages/versions.json`
* Update versions' json with release versions by updating `stable.spec` and `stable.source` to the release version and adding a new entry 
for it. For example after adding spec version `0.0.2`, the `versions.json` would look like:
~~~
{
 "latest": {
  "spec": "latest",
  "source": "master"
 },
 "stable": {
  "spec": "0.0.2",
  "source": "0.0.2"
 },
 "0.0.1": {
  "spec": "0.0.1",
  "source": "0.0.1"
 }
 "0.0.2": {
  "spec": "0.0.2",
  "source": "0.0.2"
 }
}
~~~ 
* Save updated `versions.json` to `dist` folder.
* Push the `dist` folder to `gh-pages` branch. The script is using [gh-pages](https://www.npmjs.com/package/gh-pages) 
npm module to automate this step.

## Environment variables
Following environment variables can be used to override defaults
* `OA_GIT_URL`            (default: `git@github.com:ConsenSys/web3signer.git`)
* `OA_GH_PAGES_BRANCH`    (default: `gh-pages`)
* `OA_GIT_USERNAME`       (default: `CircleCI Build`)
* `OA_GIT_EMAIL`          (default: `ci-build@consensys.net`)

Following should only be overridden if changing the project
* `OA_VERSIONS_FILE_NAME` (default: `versions.json`)
* `OA_DIST_DIR`           (default: `./dist`)
* `OA_SPEC_DIR`          (default: `../core/build/resources/main/openapi`)
 