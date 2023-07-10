package tech.pegasys.web3signer.signing.secp256k1.aws;

import com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.pegasys.web3signer.signing.config.metadata.AwsKMSMetadata;
import tech.pegasys.web3signer.signing.config.metadata.AwsKeySigningMetadata;
import tech.pegasys.web3signer.signing.secp256k1.Signer;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Create AwsKMSSigner and instantiate AWS' KmsClient library.
 */
public class AwsKMSSignerFactory {
    private static final Logger LOG = LogManager.getLogger();
    private final boolean needsToHash; // Apply Hash.sha3(data) before signing

    public AwsKMSSignerFactory() {
        this(true);
    }

    public AwsKMSSignerFactory(final boolean needsToHash) {
        this.needsToHash = needsToHash;
    }

    public Signer createSigner(final AwsKMSMetadata awsKMSMetadata) {
        checkArgument(awsKMSMetadata != null, "awsKMSMetadata must not be null");


    }
}
