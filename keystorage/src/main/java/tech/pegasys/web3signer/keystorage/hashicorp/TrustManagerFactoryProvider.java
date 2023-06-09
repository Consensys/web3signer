/*
 * Copyright 2023 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.web3signer.keystorage.hashicorp;

import tech.pegasys.web3signer.keystorage.hashicorp.config.TlsOptions;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.TrustManagerFactory;

import org.apache.tuweni.net.tls.TrustManagerFactories;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;

/** Create TrustManagerFactory based on Trust store type. */
public class TrustManagerFactoryProvider {

  public static TrustManagerFactory getTrustManagerFactory(final TlsOptions tlsOptions)
      throws GeneralSecurityException, IOException {
    final TrustManagerFactory trustManagerFactory;
    final TrustStoreType trustStoreType =
        tlsOptions
            .getTrustStoreType()
            .orElseThrow(
                () ->
                    new HashicorpException(
                        "Cannot create TrustManagerFactory from empty truststore type."));
    switch (trustStoreType) {
      case JKS:
      case PKCS12:
        trustManagerFactory =
            buildFromKeystore(
                tlsOptions.getTrustStorePath(), tlsOptions.getTrustStorePassword(), trustStoreType);
        break;
      case PEM:
        trustManagerFactory =
            TrustManagerFactoryProvider.buildFromPemFile(tlsOptions.getTrustStorePath());
        break;
      default:
        // Tuweni throws an NPE if the trustStorePath has no directory prefix, thus requiring
        // the use of absolutePath.
        trustManagerFactory =
            TrustManagerFactories.allowlistServers(
                tlsOptions.getTrustStorePath().toAbsolutePath(), true);
        break;
    }
    return trustManagerFactory;
  }

  private static TrustManagerFactory buildFromKeystore(
      final Path keystorePath, final String keystorePassword, final TrustStoreType keystoreType)
      throws GeneralSecurityException, IOException {
    // Load the keystore
    final KeyStore keyStore = KeyStore.getInstance(keystoreType.name());
    try (final FileInputStream fis = new FileInputStream(keystorePath.toFile())) {
      keyStore.load(fis, keystorePassword.toCharArray());
    }

    // Initialize the TrustManagerFactory with the keystore
    final TrustManagerFactory trustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init(keyStore);

    return trustManagerFactory;
  }

  private static TrustManagerFactory buildFromPemFile(final Path caPemFile)
      throws GeneralSecurityException, IOException {
    final List<Certificate> certificates = loadCertificatesFromPEM(caPemFile);
    KeyStore caKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
    caKeystore.load(null, null);
    for (int i = 0; i < certificates.size(); i++) {
      caKeystore.setCertificateEntry("ca-" + i, certificates.get(i));
    }
    TrustManagerFactory trustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init(caKeystore);
    return trustManagerFactory;
  }

  private static List<Certificate> loadCertificatesFromPEM(Path caPemFile)
      throws IOException, CertificateException {
    try (FileReader fileReader = new FileReader(caPemFile.toFile(), StandardCharsets.UTF_8);
        PEMParser pemParser = new PEMParser(fileReader)) {

      JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();
      certConverter.setProvider(new BouncyCastleProvider());

      List<Certificate> certificates = new ArrayList<>();
      Object pemObject;
      while ((pemObject = pemParser.readObject()) != null) {
        certificates.add(certConverter.getCertificate((X509CertificateHolder) pemObject));
      }
      return certificates;
    }
  }
}
