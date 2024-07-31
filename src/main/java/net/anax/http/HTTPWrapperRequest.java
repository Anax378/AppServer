package net.anax.http;

import net.anax.cryptography.AESKey;
import net.anax.cryptography.RSAPrivateKey;
import net.anax.endpoint.EndpointFailedException;
import net.anax.util.CryptoUtilities;
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
import java.security.InvalidAlgorithmParameterException;
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
            JSONParser parser = new JSONParser();
            JSONObject data = (JSONObject) parser.parse(underlyingRequest.getBody());

            byte[] encryptedAesKey = Base64.getDecoder().decode(JsonUtilities.extractString(data, "encrypted_key", new EndpointFailedException("insufficient data in json", EndpointFailedException.Reason.InvalidRequest)));
            byte[] encryptedIv = Base64.getDecoder().decode(JsonUtilities.extractString(data, "encrypted_iv", new EndpointFailedException("insufficient data in json", EndpointFailedException.Reason.InvalidRequest)));

            byte[] aesKeyData = CryptoUtilities.decryptWithRSA(encryptedAesKey, rsaKey.getKey());
            byte[] aesIv = CryptoUtilities.decryptWithRSA(encryptedIv, rsaKey.getKey());

            this.aesKey = new AESKey(aesKeyData, aesIv);

            byte[] encryptedRequestData = Base64.getDecoder().decode(JsonUtilities.extractString(data, "encrypted_request", new EndpointFailedException("insufficient data in json", EndpointFailedException.Reason.InvalidRequest)));
            byte[] requestData = CryptoUtilities.decryptWithAES(encryptedRequestData, this.aesKey.getkey(), this.aesKey.getIv());

            underlyingRequest = httpParser.parseRequest(new ByteArrayInputStream(requestData), traceId);
            return underlyingRequest;

        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e){
            throw new EndpointFailedException("could not decrypt the underlying request", EndpointFailedException.Reason.InvalidRequest, e);
        } catch (IOException e){
            throw new EndpointFailedException("connection failed", EndpointFailedException.Reason.UnexpectedError, e);
        } catch (ParseException e) {
            throw new EndpointFailedException("could not parse json", EndpointFailedException.Reason.InvalidRequest, e);
        }
    }
    public AESKey getAESKey(){
        return aesKey;
    }
}
