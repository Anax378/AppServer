package org.example.http;

import java.util.HashMap;

public class HTTPRequest {
    private HTTPMethod method;
    private String URI;
    private HashMap<HTTPHeaderType, String> headers = new HashMap<>();
    private String body;

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public HTTPMethod getMethod() {
        return method;
    }

    public void setMethod(HTTPMethod method) {
        this.method = method;
    }

    public String getURI() {
        return URI;
    }

    public void setURI(String URI) {
        this.URI = URI;
    }

    public void setHeader(HTTPHeaderType headerType, String header){
        this.headers.put(headerType, header);
    }

    public String getHeader(HTTPHeaderType headerType){
        if(headers.containsKey(headerType)){
            return headers.get(headerType);
        }
        return "";
    }
}
