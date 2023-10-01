package net.anax.thread;

import net.anax.database.DatabaseAccessManager;
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
                    response.setBody("404 not found<br>currently implemented file system schema:<br><img src=\"http://media.discordapp.net/attachments/1158003459073785896/1158075668958031882/IMPLEMENTED_FILE_STRUCUTRE.drawio.png?ex=651aed83&is=65199c03&hm=4597daf7ad29b28c2c93b3754df489f8ebca21d2fbe6ee5957ee387ef841922d&=&width=672&height=577\">");
                    response.setHeader(HTTPHeaderType.Content_type, "text/html");
                    response.setHeader(HTTPHeaderType.Access_Control_Allow_Origin, "*");
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
