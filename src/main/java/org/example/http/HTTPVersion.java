package org.example.http;

public enum HTTPVersion {
    HTTP_1_1("HTTP/1.1");

    String version;
    HTTPVersion(String version){
        this.version = version;
    }
}
