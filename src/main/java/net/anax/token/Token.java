package net.anax.token;

import net.anax.cryptography.HMACSHA256Key;
import net.anax.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;

public class Token {
    HashMap<Claim, String> claims = new HashMap<>();
    HashMap<TokenHeader, String> headers = new HashMap<>();
    String data;
    String signature;
    public Token(){}

    public void addClaim(Claim claim, String data){
        claims.put(claim, data);
    }

    public void addHeader(TokenHeader header, String data){
        headers.put(header, data);
    }

    public String getHeader(TokenHeader header){
        return headers.get(header);
    }
    public String getClaim(Claim claim){
        if(!claims.containsKey(claim)){return null;}
        return claims.get(claim);
    }

    void generateData() throws UnsupportedEncodingException {
        JSONObject bodyData = new JSONObject();
        JSONObject headersData = new JSONObject();

        for(Claim claim : Claim.values()){
            if(claims.containsKey(claim)){
                bodyData.put(claim.key, claims.get(claim));
            }
        }

        for(TokenHeader header : TokenHeader.values()){
            if(headers.containsKey(header)){
                headersData.put(header.headerKey, headers.get(header));
            }
        }

        String bodyDataBase64 = Base64.getEncoder().encodeToString(bodyData.toJSONString().getBytes("UTF-8"));
        String headersDataBase64 = Base64.getEncoder().encodeToString(headersData.toJSONString().getBytes("UTF-8"));

        this.data = headersDataBase64 + "." + bodyDataBase64;

    }

    public static Token parseToken(String token){
        String[] parts = token.split("\\.");
        if(parts.length != 3){return null;}
        byte[] headerBytes = Base64.getDecoder().decode(parts[0]);
        byte[] bodyBytes = Base64.getDecoder().decode(parts[1]);
        String headerJson = new String(headerBytes, StandardCharsets.UTF_8);
        String bodyJson = new String(bodyBytes, StandardCharsets.UTF_8);

        JSONObject headers;
        JSONObject body;

        Token returnToken = new Token();
        try {
            headers = (JSONObject) new JSONParser().parse(headerJson);
            body = (JSONObject) new JSONParser().parse(bodyJson);

        } catch (ParseException e) {
            return null;
        }

        for(Claim claim : Claim.values()){
            if(body.containsKey(claim.key)){
                returnToken.addClaim(claim, (String)body.get(claim.key));
            }
        }

        returnToken.data = parts[0] + "." + parts[1];
        returnToken.signature = parts[2];


        for(TokenHeader header : TokenHeader.values()){
            if(headers.containsKey(header)){
                returnToken.addHeader(header, (String)headers.get(header.headerKey));
            }
        }

        return returnToken;
    }

    boolean validateSignature(HMACSHA256Key key){
        String data = this.data;
        try {
            String correct_signature_string = this.generateSignature(key);
            if(correct_signature_string.equals(this.signature)){
                return true;
            }
            return false;

        } catch (NoSuchAlgorithmException e) {
            Logger.log("invalid algorithm, invalid string literal. lid: 154619845358432");
            return false;
        } catch (InvalidKeyException e) {
            Logger.log("invalidKeyException. lid: 353521354231");
            if(true){return false;} //prevent unreachable code exception further down;
        }
        return false;
    }

    String generateSignature(HMACSHA256Key key) throws InvalidKeyException, NoSuchAlgorithmException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec spec = new SecretKeySpec(key.key, "HmacSHA256");
        mac.init(spec);
        byte[] signature = mac.doFinal(this.data.getBytes());
        return Base64.getEncoder().encodeToString(signature);
    }

    public Token sign(HMACSHA256Key key) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        this.generateData();
        this.signature = generateSignature(key);
        return this;
    }

    public String getTokenString() throws UnsupportedEncodingException {
        return data + "." + signature;
    }


}
