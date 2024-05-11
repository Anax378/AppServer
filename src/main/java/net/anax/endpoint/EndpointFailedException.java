package net.anax.endpoint;

import net.anax.http.HTTPStatusCode;

public class EndpointFailedException extends Exception {
    public Reason reason;
    public static boolean doPrintStacktrace = false;
    public EndpointFailedException(String message, Reason reason) {
        super(message);
        this.reason = reason;
        if(doPrintStacktrace){
            System.out.println("instantiated exception: " + message);
            this.printStackTrace();
        }
    }

    public EndpointFailedException(String message, Reason reason, Exception cause){
        super(message, cause);
        this.reason = reason;
        if(doPrintStacktrace){
            this.printStackTrace();
        }

    }

    public enum Reason {
        AccessDenied(HTTPStatusCode.CLIENT_ERROR_403_FORBIDDEN, "Access Denied"),
        DataNotFound(HTTPStatusCode.CLIENT_ERROR_404_NOT_FOUND, "Data not found"),
        UnexpectedError(HTTPStatusCode.SERVER_ERROR_500_INTERNAL_SERVER_ERROR, "Unexpected Error"),
        NothingChanged(HTTPStatusCode.SERVER_ERROR_500_INTERNAL_SERVER_ERROR, "Nothing Changed"),
        InvalidRequest(HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST, "Invalid Request")
        ;

        public final HTTPStatusCode statusCode;
        public final String message;

        Reason(HTTPStatusCode statusCode, String message ){
            this.statusCode = statusCode;
            this.message = message ;
        }
    }
}
