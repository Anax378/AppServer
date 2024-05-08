package net.anax.http;

import com.sun.net.httpserver.Headers;
import net.anax.cryptography.AESKey;
import net.anax.endpoint.EndpointFailedException;
import org.json.simple.JSONObject;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class HTTPWrapperResponse {
    private HTTPResponse underlyingResponse;
    private AESKey key;

    public HTTPWrapperResponse(HTTPResponse underlyingResponse, AESKey key){
        this.underlyingResponse = underlyingResponse;
        this.key = key;
    }

    public void writeOnSteam(OutputStream ostream, long traceId) throws EndpointFailedException {
        try {

            ByteArrayOutputStream captureStream = new ByteArrayOutputStream();
            underlyingResponse.writeOnStream(captureStream, traceId);
            byte[] responseData = captureStream.toByteArray();

            String base64EncodedResponse = Base64.getEncoder().encodeToString(responseData);
            JSONObject responseJson = new JSONObject();
            responseJson.put("response", base64EncodedResponse);

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key.getkey());

            byte[] data = cipher.doFinal(responseJson.toJSONString().getBytes(StandardCharsets.US_ASCII));
            String encodedResponse = Base64.getEncoder().encodeToString(data);

            HTTPResponse wrapperResponse = new HTTPResponse(HTTPVersion.HTTP_1_1, HTTPStatusCode.OK_200);
            wrapperResponse.setBody(encodedResponse);
            wrapperResponse.writeOnStream(ostream, traceId);

        }catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException e) {
            throw new EndpointFailedException("failed to encrypt response, must likely, the provided AES key was not valid", EndpointFailedException.Reason.InvalidRequest);
        }
    }

}
