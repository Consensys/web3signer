# Fix #1012: Add EIP-712 structured data signing support

## 🎯 Purpose
This PR fixes issue #1012 by implementing proper EIP-712 structured data signing support in Web3Signer.

## 🐛 Problem
Web3Signer was incorrectly using the Ethereum message prefix (`\x19Ethereum Signed Message:\n`) instead of the EIP-712 prefix (`\x19\x01`) for structured data signing, causing incompatibility with EIP-712 standard.

## ✅ Solution
Added support for pre-hashed data signing and updated EIP-712 implementation to use the correct prefix:

### Changes Made:

#### 1. **CredentialSigner.java**
- Added `signHashed(byte[] hashedData)` method for signing pre-hashed data
- Validates input data is exactly 32 bytes
- Uses `Sign.signMessage()` with `needToHash=false` to prevent double hashing

#### 2. **EthSecpArtifactSigner.java**
- Added `signHashed(Bytes hashedMessage)` method
- Includes type checking for CredentialSigner compatibility
- Delegates to CredentialSigner.signHashed() when supported

#### 3. **SignerForIdentifier.java**
- Added `signHashed(String identifier, Bytes hashedData)` method for API layer
- Provides type-safe access to pre-hashed signing functionality

#### 4. **EthSignTypedDataResultProvider.java**
- **Key Fix**: Updated to use EIP-712 compliant signing process:
  - Uses `StructuredDataEncoder.getStructuredData()` to get EIP-712 formatted data
  - Applies SHA3 hash to structured data to get final 32-byte hash
  - Calls `signHashed()` instead of `sign()` to avoid double hashing
- Added proper error handling and validation

#### 5. **EthSignTypedDataResultProviderTest.java**
- Updated test to verify EIP-712 compliance
- Ensures signed hash matches expected EIP-712 hash
- Validates compatibility with Web3j EIP-712 implementation

## 🧪 Testing
- [x] Updated existing tests to verify EIP-712 compliance
- [x] Ensured backward compatibility with existing signing functionality
- [x] Verified signature matches Web3j EIP-712 implementation
- [x] Added proper input validation and error handling

## 🔒 Security Considerations
- ✅ **Input Validation**: Strict 32-byte validation for hashed data
- ✅ **Type Safety**: Proper type checking and casting
- ✅ **No Double Hashing**: Correct use of `needToHash=false` parameter
- ✅ **Backward Compatibility**: No changes to existing signing methods
- ✅ **Fail-Fast**: Clear error messages for unsupported operations

## 📋 Checklist
- [x] Code follows project coding conventions
- [x] Added appropriate documentation/comments
- [x] Updated/added tests for new functionality
- [x] Verified backward compatibility
- [x] No breaking changes to existing API
- [x] Security implications reviewed
- [x] Fixes the specific issue mentioned in #1012

## 🎯 Related Issues
Fixes #1012 - EIP-712: Support to sign typed data with prefix \x19\x01

## 📝 Additional Notes
This implementation maintains full backward compatibility while adding proper EIP-712 support. The solution follows the existing Web3Signer architecture patterns and uses the established Web3j library for cryptographic operations.

The fix specifically addresses the core issue where Web3Signer was not correctly implementing the EIP-712 standard for structured data signing, which requires the `\x19\x01` prefix instead of the Ethereum message prefix.
