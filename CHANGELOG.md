# Changelog

## Next Release

## 21.3.0

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
- Ethereuem secp256k1 signing of data
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
