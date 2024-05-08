package net.anax.http;

import java.util.HashMap;

public class HTTPRequest {
    private HTTPMethod method;
    private String URI;
    private HashMap<HTTPHeaderType, String> headers = new HashMap<>();
    private String body;
    byte[] rawInput;

    public String getBody() {
        return body;
    }

    public byte[] getRawInput(){
        return rawInput;
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

    public void printSelf(){
        System.out.println("============== HttpRequest printSelf() Start =====================");
        System.out.println("Method: " + method.name());
        System.out.println("URI: " + URI);

        System.out.println("------- start of HttpRequest printSelf() Headers ---------");
        for(HTTPHeaderType type : headers.keySet()){
            System.out.println(type.name + ": " + headers.get(type));
        }
        System.out.println("------- end of HttpRequest printSelf() Headers ---------");

        System.out.println("body: " + body);


        System.out.println("============== HttpRequest printSelf() End =====================");

    }
}
