package net.anax.endpoint;

public class EndpointFailedException extends Exception {
    public EndpointFailedException(String userNotAuthorized, Reason reason) {

    }

    public static enum Reason {
        AccessDenied,
        DataNotFound,
        UnexpectedError,
        NothingChanged
        ;
    }
}
