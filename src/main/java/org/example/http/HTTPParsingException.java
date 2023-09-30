package org.example.http;

public class HTTPParsingException extends Exception{
    private HTTPStatusCode statusCode;

    public HTTPParsingException(HTTPStatusCode statusCode, String message){
        super(message);
        this.statusCode = statusCode;
    }

    public HTTPStatusCode getStatusCode(){
        return statusCode;
    }

    public void setStatusCode(HTTPStatusCode statusCode){
        this.statusCode = statusCode;
    }
}
