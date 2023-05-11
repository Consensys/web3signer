package tech.pegasys.web3signer.core.jsonrpcproxy.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import tech.pegasys.web3signer.signing.KeyType;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.fail;

public class MetadataFileHelper {
    private static final ObjectMapper YAML_OBJECT_MAPPER = YAMLMapper.builder().build();

    public void createKeyStoreYamlFileAt(
            final Path metadataFilePath,
            final Path keystoreFile,
            final String password,
            final KeyType keyType) {
        final String filename = metadataFilePath.getFileName().toString();
        final String passwordFilename = filename + ".password";
        final Path passwordFile = metadataFilePath.getParent().resolve(passwordFilename);
        createPasswordFile(passwordFile, password);

        final Map<String, String> signingMetadata = new HashMap<>();
        signingMetadata.put("type", "file-keystore");
        signingMetadata.put("keystoreFile", keystoreFile.toString());
        signingMetadata.put("keystorePasswordFile", passwordFile.toString());
        signingMetadata.put("keyType", keyType.name());
        createYamlFile(metadataFilePath, signingMetadata);
    }

    private void createPasswordFile(final Path passwordFilePath, final String password) {
        try {
            Files.writeString(passwordFilePath, password);
        } catch (IOException e) {
            fail("Unable to create password file", e);
        }
    }

    private void createYamlFile(
            final Path filePath, final Map<String, ? extends Serializable> signingMetadata) {
        try {
            YAML_OBJECT_MAPPER.writeValue(filePath.toFile(), signingMetadata);
        } catch (final IOException e) {
            fail("Unable to create metadata file.", e);
        }
    }

}