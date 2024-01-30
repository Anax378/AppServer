package net.anax.endpoint;

public class EndpointFailedException extends Exception {
    public Reason reason;
    public EndpointFailedException(String message, Reason reason) {
        super(message);
        this.reason = reason;
    }

    public static enum Reason {
        AccessDenied,
        DataNotFound,
        UnexpectedError,
        NothingChanged
        ;
    }
}
