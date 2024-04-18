package net.anax.http;

public class HTTPParsingException extends Exception{
    private HTTPStatusCode statusCode;
    public static boolean doPrintStacktrace = false;

    public HTTPParsingException(HTTPStatusCode statusCode, String message){
        super(message);
        this.statusCode = statusCode;
        if(doPrintStacktrace){
            this.printStackTrace();
        }
    }

    public HTTPStatusCode getStatusCode(){
        return statusCode;
    }

    public void setStatusCode(HTTPStatusCode statusCode){
        this.statusCode = statusCode;
    }
}
