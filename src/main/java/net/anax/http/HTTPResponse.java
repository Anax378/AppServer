package net.anax.http;

import net.anax.logging.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;

public class HTTPResponse {
    private static final String CRLF = "\r\n";

    private HTTPVersion version;
    private HTTPStatusCode statusCode;
    private HashMap<HTTPHeaderType, String> headers = new HashMap<>();
    private String body = "";

    public HTTPResponse(HTTPVersion version, HTTPStatusCode statusCode) {
        this.version = version;
        this.statusCode = statusCode;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setHeader(HTTPHeaderType headerType, String content){
        if(content.equals("")){
            headers.remove(headerType);
            return;
        }
        headers.put(headerType, content);
    }

    String getHeader(HTTPHeaderType type){
        if(headers.containsKey(type)){
            return headers.get(type);
        }
        return "";
    }

    public HTTPVersion getVersion() {
        return version;
    }

    public void setVersion(HTTPVersion version) {
        this.version = version;
    }

    public HTTPStatusCode getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(HTTPStatusCode statusCode) {
        this.statusCode = statusCode;
    }

    public String getData(){
        StringBuilder data = new StringBuilder(HTTPVersion.HTTP_1_1.version + " " + statusCode.statusCode + " " + statusCode.message + CRLF);
        String headerContent;

        if(!body.isEmpty()){
            this.setHeader(HTTPHeaderType.Content_Length, ""+body.length());
        }

        for(HTTPHeaderType type : HTTPHeaderType.values()){
            if(!(headerContent = this.getHeader(type)).equals("")){
                data.append(type.name).append(": ").append(headerContent).append(CRLF);
            }
        }
        data.append(CRLF);
        data.append(body);
        return data.toString();
    }

    public void writeOnStream(OutputStream outputStream, long traceId) {
        try {
            Logger.info("writing on outputStream: " + Arrays.toString(this.getData().getBytes(StandardCharsets.US_ASCII)), traceId);
            outputStream.write(this.getData().getBytes(StandardCharsets.US_ASCII));
        } catch (IOException e) {
            Logger.error("IOException when writing on outputStream", traceId);
            Logger.logException(e, traceId);
        }
    }
}
