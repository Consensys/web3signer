package tech.pegasys.web3signer.tests.signing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.junit.jupiter.MockServerExtension;
import tech.pegasys.web3signer.core.signing.KeyType;

import java.nio.file.Path;

@ExtendWith(MockServerExtension.class)
public class InterlockSigningAcceptanceTest extends SigningAcceptanceTestBase {

    @Test
    public void blsSingingUsingInterlock() {
        final Path configFile = testDirectory.resolve("yubihsm_1.yaml");
        metadataFileHelpers.createYubiHsmYamlFileAt(configFile, KeyType.BLS);

        signAndVerifySignature(yubiHsmShellEnvMap());
    }
}
