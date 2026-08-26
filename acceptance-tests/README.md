## Acceptance Tests

Following instructions are required to setup environment for running acceptance tests for external vault providers:

### Azure Key Vault

All Azure Key Vault acceptance tests run automatically against a local
`ghcr.io/usmansaleem/azure-keyvault-emulator:v2.3.0` Testcontainers instance, requiring no manual
setup, credentials, or Azure subscription.
