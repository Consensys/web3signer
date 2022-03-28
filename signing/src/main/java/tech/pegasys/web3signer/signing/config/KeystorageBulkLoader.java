package tech.pegasys.web3signer.signing.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.web3signer.signing.BlsArtifactSigner;
import tech.pegasys.web3signer.signing.config.metadata.SignerOrigin;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;

public class KeystorageBulkLoader extends SignerLoader {

  private static final Logger LOG = LogManager.getLogger();;

  static Collection<BlsArtifactSigner> mapToSigner(final Stream<Optional<String>> privateKeys, final SignerOrigin signerOrigin) {
    ForkJoinPool forkJoinPool = null;
    try {
      return new ForkJoinPool(numberOfThreads())
        .submit(() -> privateKeys.filter(key -> key.isPresent())
          .map(String.class::cast)
          .map(key -> Bytes.fromHexString(key))
          .map(bytes -> new BLSKeyPair(BLSSecretKey.fromBytes(Bytes32.wrap(bytes))))
          .map(blsKeyPair -> new BlsArtifactSigner(blsKeyPair, signerOrigin))
          .collect(Collectors.toSet()))
        .get();
    } catch (final Exception e) {
      LOG.error("Unexpected error while bulk loading AWS secrets", e);
    } finally {
      if (forkJoinPool != null) {
        forkJoinPool.shutdown();
      }
    }
    return emptySet();
  }

}
