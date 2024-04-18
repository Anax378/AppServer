package net.anax.thread;

import net.anax.VirtualFileSystem.AuthorizationProfile;
import net.anax.VirtualFileSystem.UserAuthorizationProfile;
import net.anax.cryptography.KeyManager;
import net.anax.database.Authorization;
import net.anax.database.DatabaseAccessManager;
import net.anax.endpoint.EndpointFailedException;
import net.anax.http.*;
import net.anax.logging.Logger;
import net.anax.token.Claim;
import net.anax.token.Token;
import net.anax.util.StringUtilities;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;

public class WorkerThread extends Thread{
    private int timeOutTimeMillis = 10000;
    private HTTPParser parser = new HTTPParser();
    Socket socket;
    KeyManager keyManager;
    long traceId;

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

        DatabaseAccessManager.getInstance().setKeyManager(keyManager);

        try {
            socket.setSoTimeout(getTimeOutTimeMillis());
        } catch (SocketException e) {
            Logger.error("could not set socket timeout", traceId);
            throw new RuntimeException(e);
        }
        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();


            try {
                HTTPRequest request = parser.parseRequest(inputStream);
                Logger.info("request bytes: " + Arrays.toString(request.getRawInput()), traceId);
                Logger.info("request: " + new String(request.getRawInput()), traceId);
                HTTPResponse response = new HTTPResponse(HTTPVersion.HTTP_1_1, HTTPStatusCode.OK_200);

                String body = null;

                try {

                    AuthorizationProfile auth = new UserAuthorizationProfile(-1);
                    String tokenString = request.getHeader(HTTPHeaderType.Authorization).replace("Bearer", "");
                    Token token = Token.parseToken(tokenString.trim());

                    if(token != null){
                        if(token.validateSignature(keyManager.getHMACSHA256TokenKey())){
                            String expirationTime = token.getClaim(Claim.ExpirationTimestamp);
                            if(StringUtilities.isLong(expirationTime)){
                                if(Long.parseLong(expirationTime) > System.currentTimeMillis()){
                                    String id = token.getClaim(Claim.Subject);
                                    if(StringUtilities.isInteger(id)){
                                        auth = new UserAuthorizationProfile(Integer.parseInt(id));
                                        Logger.info("authorized token " + token, traceId);
                                    }
                                }else{Logger.log("expired token " + token, traceId);}
                            }else{Logger.log("invalid data in token  " + token, traceId);}
                        }else{Logger.log("could not verify token " + token, traceId);}
                    }else{Logger.log("token is null", traceId);}

                    body = DatabaseAccessManager.getInstance().handleRequest(request.getURI(), request.getBody(), auth, traceId);

                    if(body == null){
                        throw new EndpointFailedException("endpoint not found", EndpointFailedException.Reason.DataNotFound);
                    }

                    response.setBody(body);
                    response.setStatusCode(HTTPStatusCode.OK_200);

                }catch (EndpointFailedException e){
                        if(e.reason == EndpointFailedException.Reason.DataNotFound){
                            response.setStatusCode(HTTPStatusCode.CLIENT_ERROR_404_NOT_FOUND);
                            response.setBody("Data not found");
                            Logger.log("returning 404, data not found", traceId);
                        }
                        else if(e.reason == EndpointFailedException.Reason.NothingChanged){
                            response.setStatusCode(HTTPStatusCode.SERVER_ERROR_500_INTERNAL_SERVER_ERROR);
                            response.setBody("Nothing changed");
                            Logger.log("returning 500, nothing changed", traceId);
                        }
                        else if (e.reason == EndpointFailedException.Reason.AccessDenied){
                            response.setStatusCode(HTTPStatusCode.CLIENT_ERROR_403_FORBIDDEN);
                            response.setBody("Access Denied");
                            Logger.log("returning 403, forbidden", traceId);
                        }
                        else if (e.reason == EndpointFailedException.Reason.UnexpectedError){
                            response.setStatusCode(HTTPStatusCode.CLIENT_ERROR_414_URI_TOO_LONG);
                            response.setBody("Unexpected Error");
                            Logger.log("returning 414, unexpected error", traceId);
                        }
                        e.printStackTrace();
                }

               response.writeOnStream(outputStream, traceId);
            } catch (HTTPParsingException e) {
                HTTPResponse response = new HTTPResponse(HTTPVersion.HTTP_1_1, e.getStatusCode());
                response.writeOnStream(outputStream, traceId);
            }


        } catch (IOException e) {
            Logger.error("IO Exception", traceId);
            throw new RuntimeException(e);
        }finally {
            try {
                if(outputStream != null){
                    outputStream.close();
                }
                if(inputStream != null){
                    inputStream.close();
                }
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
