# EIP-712 Implementation Guide

## Overview
This document describes the EIP-712 structured data signing implementation added to Web3Signer.

## New Methods

### CredentialSigner.signHashed(byte[] hashedData)
- **Purpose**: Signs pre-hashed data without applying additional hashing
- **Input**: 32-byte hash data
- **Output**: ECDSA signature with recovery ID
- **Usage**: EIP-712 structured data signing where hash is pre-computed

### EthSecpArtifactSigner.signHashed(Bytes hashedMessage)
- **Purpose**: Wrapper for signHashed functionality at artifact level
- **Input**: 32-byte hash as Bytes
- **Output**: SecpArtifactSignature
- **Type Safety**: Only works with CredentialSigner instances

### SignerForIdentifier.signHashed(String identifier, Bytes hashedData)
- **Purpose**: API layer method for signing pre-hashed data
- **Input**: Identifier and 32-byte hash
- **Output**: Optional hex signature string
- **Error Handling**: Returns empty if signer not found

## EIP-712 Flow

1. **Input**: JSON structured data according to EIP-712 specification
2. **Encoding**: Use `StructuredDataEncoder.getStructuredData()` to get EIP-712 formatted data
3. **Hashing**: Apply SHA3 to get final 32-byte hash
4. **Signing**: Call `signHashed()` with the computed hash
5. **Output**: EIP-712 compliant signature

## Example Usage

```java
// EIP-712 structured data signing
StructuredDataEncoder encoder = new StructuredDataEncoder(jsonData);
byte[] structuredData = encoder.getStructuredData(); // 0x1901 || domainSeparator || hashStruct
byte[] finalHash = Hash.sha3(structuredData); // Final 32-byte hash
String signature = signerProvider.signHashed(identifier, Bytes.of(finalHash));
```

## Security Notes

- Always validate hash input is exactly 32 bytes
- Use `needToHash=false` when calling Web3j Sign.signMessage()
- Maintain type safety with instanceof checks
- Preserve backward compatibility with existing sign() methods

## Testing

Tests verify:
- Correct EIP-712 hash generation
- Signature compatibility with Web3j
- Proper error handling for invalid inputs
- Type safety and validation
