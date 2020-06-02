/*
 * Copyright 2020 ConsenSys AG.
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
package tech.pegasys.eth2signer.core.http;

import tech.pegasys.eth2signer.core.utils.ByteUtils;
import tech.pegasys.eth2signer.core.utils.JsonDecoder;
import tech.pegasys.signers.secp256k1.api.Signature;
import tech.pegasys.signers.secp256k1.api.TransactionSigner;
import tech.pegasys.signers.secp256k1.multikey.MultiKeyTransactionSignerProvider;

import java.util.Optional;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public class Secp256k1SigningHandler implements Handler<RoutingContext> {

  private static final Logger LOG = LogManager.getLogger();
  public static final String SECP256k1_API_PATH = "/secp256k1/sign/(?<address>.*)";

  final MultiKeyTransactionSignerProvider secp256k1Signer;
  private final JsonDecoder jsonDecoder;

  public Secp256k1SigningHandler(
      final MultiKeyTransactionSignerProvider secp256k1Signer, final JsonDecoder jsonDecoder) {
    this.secp256k1Signer = secp256k1Signer;
    this.jsonDecoder = jsonDecoder;
  }

  @Override
  public void handle(final RoutingContext context) {
    LOG.debug("Received a request for {}", context.normalisedPath());
    final String address = getAddress(context);
    final Optional<TransactionSigner> signer = secp256k1Signer.getSigner(address);
    if (signer.isEmpty()) {
      LOG.error("Unable to find an appropriate signer for request: {}", address);
      context.fail(404);
    } else {
      final Bytes dataToSign = getDataToSign(context);
      final Signature signature = signer.get().sign(dataToSign.toArrayUnsafe());

      // Copied from EthSigner (EthSignBodyProvider
      final Bytes outputSignature =
          Bytes.concatenate(
              Bytes32.leftPad(Bytes.wrap(ByteUtils.bigIntegerToBytes(signature.getR()))),
              Bytes32.leftPad(Bytes.wrap(ByteUtils.bigIntegerToBytes(signature.getS()))),
              Bytes.wrap(ByteUtils.bigIntegerToBytes(signature.getV())));

      context.response().end(outputSignature.toString());
    }
  }

  private String getAddress(final RoutingContext context) {
    return context.pathParam("address");
  }

  private Bytes getDataToSign(final RoutingContext context) {
    final Buffer body = context.getBody();
    final SigningRequestBody signingRequest =
        jsonDecoder.decodeValue(body, SigningRequestBody.class);
    return signingRequest.signingRoot();
  }
}
