package net.anax.thread;

import net.anax.VirtualFileSystem.AuthorizationProfile;
import net.anax.VirtualFileSystem.UserAuthorizationProfile;
import net.anax.cryptography.KeyManager;
import net.anax.database.DatabaseAccessManager;
import net.anax.endpoint.EndpointFailedException;
import net.anax.http.*;
import net.anax.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

public class WorkerThread extends Thread{
    private int timeOutTimeMillis = 10000;
    private HTTPParser parser = new HTTPParser();
    Socket socket;
    KeyManager keyManager;

    public int getTimeOutTimeMillis() {
        return timeOutTimeMillis;
    }

    public void setTimeOutTimeMillis(int timeOutTimeMillis) {
        this.timeOutTimeMillis = timeOutTimeMillis;
    }

    public WorkerThread(Socket socket, KeyManager keyManager) throws SocketException {
        this.keyManager = keyManager;
        this.socket = socket;
    }

    @Override
    public void run() {
        InputStream inputStream = null;
        OutputStream outputStream = null;

        DatabaseAccessManager.getInstance().setKeyManager(keyManager);

        try {
            socket.setSoTimeout(timeOutTimeMillis);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();


            try {
                HTTPRequest request = parser.parseRequest(inputStream);
                HTTPResponse response = new HTTPResponse(HTTPVersion.HTTP_1_1, HTTPStatusCode.OK_200);

                //handle request

                String body = null;

                try {
                    body = DatabaseAccessManager.getInstance().handleRequest(request.getURI(), request.getBody(), new AuthorizationProfile() {
                        @Override
                        public boolean isAdmin() {
                            return true;
                        }

                        @Override
                        public int getId() {
                            return -1;
                        }
                    });

                    if(body == null){
                        throw new EndpointFailedException("endpoint not found", EndpointFailedException.Reason.DataNotFound);
                    }

                    response.setBody(body);
                    response.setStatusCode(HTTPStatusCode.OK_200);

                }catch (EndpointFailedException e){
                        if(e.reason == EndpointFailedException.Reason.DataNotFound){
                            response.setStatusCode(HTTPStatusCode.CLIENT_ERROR_404_NOT_FOUND);
                            response.setBody("Data not found");
                        }
                        else if(e.reason == EndpointFailedException.Reason.NothingChanged){
                            response.setStatusCode(HTTPStatusCode.SERVER_ERROR_500_INTERNAL_SERVER_ERROR);
                            response.setBody("Nothing changed");
                        }
                        else if (e.reason == EndpointFailedException.Reason.AccessDenied){
                            response.setStatusCode(HTTPStatusCode.CLIENT_ERROR_403_FORBIDDEN);
                            response.setBody("Access Denied");
                        }
                        else if (e.reason == EndpointFailedException.Reason.UnexpectedError){
                            response.setStatusCode(HTTPStatusCode.CLIENT_ERROR_414_URI_TOO_LONG);
                            response.setBody("Unexpected Error");
                        }
                        e.printStackTrace();
                }

               response.writeOnStream(outputStream);
            } catch (HTTPParsingException e) {
                HTTPResponse response = new HTTPResponse(HTTPVersion.HTTP_1_1, e.getStatusCode());
                response.writeOnStream(outputStream);
            }


        } catch (IOException e) {
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
