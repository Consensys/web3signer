# Eth2Signer OpenAPI Spec Publish

This directory contains NodeJS project which publishes Eth2Signer OpenAPI specifications to 
[`gh-pages`](https://github.com/PegaSysEng/eth2signer/tree/gh-pages) branch via CI job after build and acceptanceTests. 
See `publishOpenApiSpec` job in `.circleci/config.yml`.

## Prerequisite 
The script assumes that the `gradle build` (from the root directory) has already been executed which prepares the 
eth2signer spec at `core/build/resources/main/openapi/eth2signer.yaml` (refered as `spec` in this document). 

## Procedure
The script performs following tasks:

* Create `dist` directory (this folder will be pushed to `gh-pages` branch)
* Read spec's version i.e. `info.version`.
* Copy the spec to `dist` as `eth2signer-latest.yaml`.

For release version, it performs following additional steps (the release version do not have `-dev-` in it)

* Copy the spec to `dist` as `eth2signer-<version>.yaml`
* Fetch `https://github.com/PegaSysEng/eth2signer/raw/gh-pages/versions.json`
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
 