## Acceptance Tests

Following instructions are required to setup environment for running acceptance tests for external vault providers:

### Azure Key Vault
In order to run Azure Key Vault acceptance tests, you need to set the following environment variables:
```
export AZURE_CLIENT_ID=<YOUR AZURE CLIENT ID>
export AZURE_CLIENT_SECRET=<YOUR AZURE CLIENT SECRET>
export AZURE_KEY_VAULT_NAME=<YOUR AZURE KEY VAULT NAME>
export AZURE_TENANT_ID=<YOUR AZURE TENANT ID>
```

### Azure Key Vault BLS Test Keys
The "Secret" object contains at least two entries for BLS private keys. The first entry is a multi-line secret and the 
second one is a single line secret. For example, Web3Signer `BLSTestUtil.java` can generate following keys for 
non-production/test environment which can be imported into Azure Key Vault as secrets:

```
cat << EOF > bls-test-keys.txt
0x60b420bb3851d9d47acb933dbe70399bf6c92da33af01d4fb770e98c0325f41d
0x73d51abbd89cb8196f0efb6892f94d68fccc2c35f0b84609e5f12c55dd85aba8
0x39722cbbf8b91a4b9045c5e6175f1001eac32f7fcd5eccda5c6e62fc4e638508
0x4c9326bb9805fa8f85882c12eae724cef0c62e118427f5948aefa5c428c43c93
0x384a62688ee1d9a01c9d58e303f2b3c9bc1885e8131565386f75f7ae6ca8d147
0x4b6b5c682f2db7e510e0c00ed67ac896c21b847acadd8df29cf63a77470989d2
0x13086d684f4b1a1632178a8c5be08a2fb01287c4a78313c41373701eb8e66232
0x25296867ee96fa5b275af1b72f699efcb61586565d4c3c7e41f4b3e692471abd
0x10e1a313e573d96abe701d8848742cf88166dd2ded38ac22267a05d1d62baf71
0x0bdeebbad8f9b240192635c42f40f2d02ee524c5a3fe8cda53fb4897b08c66fe
EOF
```

Then import the above keys into Azure Key Vault as a secret (assuming Azure CLI is installed and logged in):

```
az keyvault secret set --vault-name "YOUR VAULT NAME" --name "BLS-TEST-KEYS" --file "./bls-test-keys.txt"
```

Finally, one more with single-line secret with a tag value ENV=TEST:

```
az keyvault secret set --vault-name "YOUR VAULT NAME" --name "BLS-TEST-TAGGED-KEY" --tags ENV=TEST --value 0x5e8d5667ce78982a07242739ab03dc63c91e830c80a5b6adca777e3f216a405d
```

### Azure Key Vault SECP Test Keys

The tests perform SECP remote signing operations using Key Vault "Key" objects. The tests assume that you have imported
following SECP256K1 private keys into Azure Key Vault (test keys are adapted from [ethpandaops](https://github.com/ethpandaops/ethereum-package/blob/main/src/prelaunch_data_generator/genesis_constants/genesis_constants.star)
where at least 1 key is tagged with `ENV=TEST`.
```
# m/44'/60'/0'/0/18
    new_prefunded_account(
        "0xD9211042f35968820A3407ac3d80C725f8F75c14",
        "a492823c3e193d6c595f37a18e3c06650cf4c74558cc818b16130b293716106f",
    ),
    # m/44'/60'/0'/0/19
    new_prefunded_account(
        "0xD8F3183DEF51A987222D845be228e0Bbb932C222",
        "c5114526e042343c6d1899cad05e1c00ba588314de9b96929914ee0df18d46b2",
    ),
    # m/44'/60'/0'/0/20
    new_prefunded_account(
        "0xafF0CA253b97e54440965855cec0A8a2E2399896",
        "04b9f63ecf84210c5366c66d68fa1f5da1fa4f634fad6dfc86178e4d79ff9e59",
    ),
```

In order to import the above keys, you need to use Azure CLI commands like below:
- Key 18
```sh
PRIVATE_KEY_HEX="a492823c3e193d6c595f37a18e3c06650cf4c74558cc818b16130b293716106f"

# Create DER format then convert to PEM
(echo "302e0201010420${PRIVATE_KEY_HEX}a00706052b8104000a" | xxd -r -p) | \
openssl ec -inform DER -outform PEM -out private-key.pem

# Verify it
openssl ec -in private-key.pem -text -noout

# Import into Azure (using your own values for vault-name)
az keyvault key import --curve P-256K --kty EC --vault-name "YOUR VAULT NAME" --name "SECP-18" --pem-file ./private-key.pem 
```

- Key 19

```sh
PRIVATE_KEY_HEX="c5114526e042343c6d1899cad05e1c00ba588314de9b96929914ee0df18d46b2"

# Create DER format then convert to PEM
(echo "302e0201010420${PRIVATE_KEY_HEX}a00706052b8104000a" | xxd -r -p) | \
openssl ec -inform DER -outform PEM -out private-key.pem

# Verify it
openssl ec -in private-key.pem -text -noout

# Import into Azure (using your own values for vault-name)
az keyvault key import --curve P-256K --kty EC --vault-name "YOUR VAULT NAME" --name "SECP-19" --pem-file ./private-key.pem 
```

- Key 20 (tagged with ENV=TEST)

```sh
PRIVATE_KEY_HEX="04b9f63ecf84210c5366c66d68fa1f5da1fa4f634fad6dfc86178e4d79ff9e59"

# Create DER format then convert to PEM
(echo "302e0201010420${PRIVATE_KEY_HEX}a00706052b8104000a" | xxd -r -p) | \
openssl ec -inform DER -outform PEM -out private-key.pem

# Verify it
openssl ec -in private-key.pem -text -noout

# Import into Azure (using your own values for vault-name)
az keyvault key import --curve P-256K --kty EC --vault-name "YOUR VAULT NAME" --name "SECP-20-TAGGED" --pem-file ./private-key.pem --tags ENV=TEST 

```