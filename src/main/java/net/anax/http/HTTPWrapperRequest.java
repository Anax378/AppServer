package net.anax.http;

import net.anax.cryptography.AESKey;
import net.anax.cryptography.RSAPrivateKey;
import net.anax.endpoint.EndpointFailedException;
import net.anax.util.JsonUtilities;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

public class HTTPWrapperRequest{
    HTTPParser httpParser;
    private RSAPrivateKey rsaKey;
    private HTTPRequest underlyingRequest = null;
    private HTTPRequest wrapperRequest;

    private AESKey aesKey = null;
    public HTTPWrapperRequest(HTTPRequest wrapperRequest, HTTPParser parser, RSAPrivateKey key){
        this.httpParser = parser;
        this.rsaKey = key;
        this.wrapperRequest = wrapperRequest;
    }
    public HTTPRequest getUnderlyingRequest(long traceId) throws HTTPParsingException, EndpointFailedException {
        if(underlyingRequest != null){return underlyingRequest;}
        try {
            byte[] encryptedData = Base64.getDecoder().decode(wrapperRequest.getBody());

            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, rsaKey.getKey());
            byte[] data = cipher.doFinal(encryptedData);

            JSONParser parser = new JSONParser();

            JSONObject cJson = (JSONObject) parser.parse(new String(data, StandardCharsets.US_ASCII));

            EndpointFailedException ex = new EndpointFailedException("could not find necessary data in cJson", EndpointFailedException.Reason.InvalidRequest);

            byte[] underlyingRequestData = Base64.getDecoder().decode(JsonUtilities.extractString(cJson, "request", ex));
            byte[] aesKeyData = Base64.getDecoder().decode(JsonUtilities.extractString(cJson, "key", ex));
            this.aesKey = new AESKey(aesKeyData);

            underlyingRequest = httpParser.parseRequest(new ByteArrayInputStream(underlyingRequestData), traceId);
            return underlyingRequest;

        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e){
            throw new EndpointFailedException("could not decrypt the underlying request", EndpointFailedException.Reason.InvalidRequest, e);
        } catch (IOException e){
            throw new EndpointFailedException("connection failed", EndpointFailedException.Reason.UnexpectedError, e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
    public AESKey getAESKey(){
        return aesKey;
    }
}
