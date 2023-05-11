package tech.pegasys.web3signer.core.util;

import tech.pegasys.web3signer.core.service.jsonrpc.exceptions.JsonRpcException;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class ResponseCodeSelector {
    public static int jsonRPCErrorCode(final JsonRpcException e) {
        switch (e.getJsonRpcError()) {
            case INVALID_REQUEST:
            case INVALID_PARAMS:
            case PARSE_ERROR:
                return BAD_REQUEST.code();
            default:
                return OK.code();
        }
    }
}
