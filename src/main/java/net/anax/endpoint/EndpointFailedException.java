package net.anax.endpoint;

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

    public static enum Reason {
        AccessDenied,
        DataNotFound,
        UnexpectedError,
        NothingChanged,
        InvalidRequest
        ;
    }
}
