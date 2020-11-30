# Changelog

## 20.11.0

### Features Added

- Interlock/Armory II HSM keystore support
- Eth2 slashing protection data able to be exported and imported from json file (Interchange format V5)
- Eth2 signing API can now return response in json format
- Genesis validators root validation for eth2 signing requests. The GVR is set on first eth2 signing request.
- Use a low watermark in Eth2 slashing protection and import/export

### Bugs Fixed
- Eth2 slashing protections metrics category was not working on CLI
- Update Filecoin RPC to be compatible with Lotus remote wallet API
- Eth2 slashing protection returns a 412 http status code for a slashing violation
- Signing with empty slashing database would sometimes fail due multiple genesis validator root values inserting concurrently 

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
