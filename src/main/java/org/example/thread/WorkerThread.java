package org.example.thread;

import org.example.database.DatabaseAccessManager;
import org.example.http.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

public class WorkerThread extends Thread{
    private int timeOutTimeMillis = 10000;
    private HTTPParser parser = new HTTPParser();
    Socket socket;

    public int getTimeOutTimeMillis() {
        return timeOutTimeMillis;
    }

    public void setTimeOutTimeMillis(int timeOutTimeMillis) {
        this.timeOutTimeMillis = timeOutTimeMillis;
    }

    public WorkerThread(Socket socket) throws SocketException {
        this.socket = socket;
    }

    @Override
    public void run() {
        InputStream inputStream = null;
        OutputStream outputStream = null;
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

                //TODO: handle request

                String body = DatabaseAccessManager.getInstance().getDataFromURI(request.getURI());
                if(body == null){
                    response.setStatusCode(HTTPStatusCode.CLIENT_ERROR_404_NOT_FOUND);
                    response.setBody("404 not found");
                }else{
                    response.setBody(body);
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
