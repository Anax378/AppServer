package net.anax.http;

public enum HTTPStatusCode {

    CLIENT_ERROR_400_BAD_REQUEST(400, "Bad Request"),
    CLIENT_ERROR_401_METHOD_NOT_ALLOWED(401, "Method Not Allowed"),
    CLIENT_ERROR_414_URI_TOO_LONG(414, "URI Too Long"),
    CLIENT_ERROR_408_REQUEST_TIMEOUT(408, "Request Timeout"),
    CLIENT_ERROR_413_REQUEST_TOO_LARGE(413, "Request Too Large"),
    CLIENT_ERROR_404_NOT_FOUND(404, "Not Found"),
    CLIENT_ERROR_403_FORBIDDEN(403, "Forbidden"),
    SERVER_ERROR_500_INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    SERVER_ERROR_501_NOT_IMPLEMENTED(501, "Not Implemented"),
    OK_200(200, "OK");

    public final int statusCode;
    public final String message;

    HTTPStatusCode(int statusCode, String message){
        this.statusCode = statusCode;
        this.message = message;
    }
}
