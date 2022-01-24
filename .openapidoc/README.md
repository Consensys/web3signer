# Web3Signer OpenAPI Spec Publish

This directory contains NodeJS project which publishes Web3Signer OpenAPI specifications to 
[`gh-pages`](https://github.com/ConsenSys/web3signer/tree/gh-pages) branch via CI job after build and acceptanceTests. 
See `publishOpenApiSpec` job in `.circleci/config.yml`.

## Prerequisite 
The script assumes that the `gradle build` (from the root directory) has already been executed which prepares the 
Web3Signer specs under `../core/build/resources/main/openapi-specs`. 

## Procedure
~~~
npm ci
node publish.js
~~~
The script performs following tasks:

* Prepare config object (see `config.js`) which contains version details and gh-pages branch's [versions.json](https://github.com/ConsenSys/web3signer/raw/gh-pages/versions.json) details
  * spec version is read from `../core/build/resources/main/openapi-specs/eth2/web3signer.yaml`
  * Stable/Release version is considered when it doesn't contain a `+` sign. 
* Re-Create `dist` directory (this folder will be pushed to `gh-pages` branch)
* Copy directory `../core/build/resources/main/openapi-spcs` to `dist/latest`
* If stable/release version, 
  * copy `dist/latest` to `dist/$version`.
  * Fetch `versions.json` from gh-pages branch and update it with stable version entries. This file is used in gh-pages to render dropdowns.
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
* `OA_SPEC_DIR`           (default: `../core/build/resources/main/openapi-specs`)
 