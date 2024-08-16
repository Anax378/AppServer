package net.anax.thread;

import net.anax.VirtualFileSystem.AuthorizationProfile;
import net.anax.VirtualFileSystem.UserAuthorizationProfile;
import net.anax.cryptography.AESKey;
import net.anax.cryptography.KeyManager;
import net.anax.database.DatabaseAccessManager;
import net.anax.endpoint.EndpointFailedException;
import net.anax.http.*;
import net.anax.logging.Logger;
import net.anax.token.Claim;
import net.anax.token.Token;
import net.anax.util.ByteUtilities;
import net.anax.util.StringUtilities;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class WorkerThread extends Thread{
    int timeOutTimeMillis = 10_000;
    private HTTPParser parser = new HTTPParser();
    Socket socket;
    KeyManager keyManager;
    long traceId;
    final long MAX_REQUEST_SIZE = 5 * 1024 * 1024; //5 MiB

    public int getTimeOutTimeMillis() {
        return timeOutTimeMillis;
    }

    public void setTimeOutTimeMillis(int timeOutTimeMillis) {
        this.timeOutTimeMillis = timeOutTimeMillis;
    }

    public WorkerThread(Socket socket, KeyManager keyManager, long traceId) throws SocketException {
        this.keyManager = keyManager;
        this.socket = socket;
        this.traceId = traceId;
    }

    @Override
    public void run() {
        InputStream inputStream = null;
        OutputStream outputStream = null;

        boolean useRSARelay = false;
        AESKey aesKey = null;
        HTTPResponse workingResponse = new HTTPResponse(HTTPVersion.HTTP_1_1, HTTPStatusCode.OK_200);

        try {
            DatabaseAccessManager.getInstance().setKeyManager(keyManager);

            socket.setSoTimeout(getTimeOutTimeMillis());
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            HTTPRequest mainRequest = parser.parseRequest(inputStream, traceId);

            Logger.info("main request bytes: " + Arrays.toString(mainRequest.getRawInput()), traceId);
            Logger.info("main request string interpretation: \n" + new String(mainRequest.getRawInput(), StandardCharsets.UTF_8), traceId);

            if(mainRequest.getMethod() == HTTPMethod.UNKNOWN){
                throw new HTTPParsingException(HTTPStatusCode.SERVER_ERROR_501_NOT_IMPLEMENTED, "method not recognized");
            }

            HTTPRequest workingRequest;
            if(mainRequest.getURI().equals("/rsaRelay") || mainRequest.getURI().equals("/rsaRelay/")){
                Logger.info("routing request through rsaRelay", traceId);

                HTTPWrapperRequest wrapperRequest = new HTTPWrapperRequest(mainRequest, parser, keyManager.getRSAPrivateTrafficKey());
                workingRequest = wrapperRequest.getUnderlyingRequest(traceId);

                Logger.info("underlying request bytes: " + Arrays.toString(workingRequest.getRawInput()), traceId);
                Logger.info("underlying request string interpretation: \n" + new String(workingRequest.getRawInput(), StandardCharsets.UTF_8), traceId);

                useRSARelay = true;
                aesKey = wrapperRequest.getAESKey();
            }else{
                workingRequest = mainRequest;
            }

            AuthorizationProfile auth = new UserAuthorizationProfile(-1);
            String tokenString = workingRequest.getHeader(HTTPHeaderType.Authorization).replace("Bearer", "");
            Token token = Token.parseToken(tokenString);

            authorization: {
                if(token == null){Logger.log("token is null: " + tokenString, traceId); break authorization;}
                if(!token.validateSignature(keyManager.getHMACSHA256TokenKey())){Logger.log("could not authorize token: " + tokenString, traceId); break authorization;}

                String expirationTimeString = token.getClaim(Claim.ExpirationTimestamp);
                if(!StringUtilities.isLong(expirationTimeString)){Logger.log("invalid expiration time in token: " + tokenString, traceId); break authorization;}

                long expirationTime = Long.parseLong(expirationTimeString);
                if(expirationTime < System.currentTimeMillis()){Logger.log("expired token: " + tokenString, traceId); break authorization;}

                String idString = token.getClaim(Claim.Subject);
                if(!StringUtilities.isInteger(idString)){Logger.log("invalid id in token: " + tokenString, traceId); break authorization;}
                int id = Integer.parseInt(idString);

                Logger.info("successfully identified user with the id " + id, traceId);
                auth = new UserAuthorizationProfile(id);

            }

            String responseBody = DatabaseAccessManager.getInstance().handleRequest(workingRequest.getURI(), workingRequest.getBody(), auth, traceId);

            if(responseBody == null){
                throw new EndpointFailedException("endpoint not found", EndpointFailedException.Reason.DataNotFound);
            }
            workingResponse.setBody(responseBody);
        } catch (SocketException e) {
            Logger.error("could not set socket timeout", traceId);
            Logger.logException(e, traceId);

            closeStreams(inputStream, outputStream);
            throw new RuntimeException(e);
        } catch (IOException e) {
            Logger.error("IO Exception", traceId);
            Logger.logException(e, traceId);

            closeStreams(inputStream, outputStream);
            throw new RuntimeException(e);
        } catch (HTTPParsingException e) {
            workingResponse.setStatusCode(e.getStatusCode());
            Logger.error("parsing exception", traceId);
            Logger.logException(e, traceId);

        } catch (EndpointFailedException e) {
            workingResponse.setStatusCode(e.reason.statusCode);
            Logger.error("enpoint failed: " + e.reason.message, traceId);
            Logger.logException(e, traceId);

        } catch (Exception e){
            workingResponse.setStatusCode(HTTPStatusCode.SERVER_ERROR_500_INTERNAL_SERVER_ERROR);
            Logger.error("caught exception", traceId);
            Logger.logException(e, traceId);
        }

        try {
            if(useRSARelay){
                HTTPWrapperResponse wrapperResponse = new HTTPWrapperResponse(workingResponse, aesKey);

                wrapperResponse.writeOnSteam(outputStream, traceId);
            }else{
                workingResponse.writeOnStream(outputStream, traceId);
            }
        } catch (EndpointFailedException e) {
            HTTPResponse response = new HTTPResponse(HTTPVersion.HTTP_1_1, HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
            response.writeOnStream(outputStream, traceId);

        } finally {
            closeStreams(inputStream, outputStream);
        }

    }

    public void closeStreams(InputStream inputStream, OutputStream outputStream){
        try {
            if(outputStream != null){
                outputStream.close();
            }
            if(inputStream != null){
                inputStream.close();
            }
            socket.close();
        } catch (IOException e) {
            Logger.logException(e, traceId);
            throw new RuntimeException(e);
        }
    }

}
