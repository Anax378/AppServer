package net.anax.endpoint;

public class EndpointFailedException extends Exception {
    public Reason reason;
    public static boolean doPrintStacktrace = false;
    public EndpointFailedException(String message, Reason reason) {
        super(message);
        this.reason = reason;
        if(doPrintStacktrace){
            this.printStackTrace();
        }
    }

    public static enum Reason {
        AccessDenied,
        DataNotFound,
        UnexpectedError,
        NothingChanged
        ;
    }
}
