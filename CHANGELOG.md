# Changelog

## 0.2.0

### Features Added
- Ethereuem secp256k1 signing of data
- Use yaml configuration of signing keys
- Filecoin support as an external signer
- Separate application into eth2, eth and filecoin commands that can be run independently
- Eth2 slashing protection. Requires a PostgreSQL database to store eth2 signed blocks and attestations
- Azure secret vault support to load all secrets from a given vault

## 0.1.0

Initial release of Eth2Signer

### Features Added
- Signing of data using a BLS key. Supports BLS private keys in unencrypted files, BLS12-381 keystore files and keys in Hashicorp 
- TLS support for rest API
- OpenAPI documentation
