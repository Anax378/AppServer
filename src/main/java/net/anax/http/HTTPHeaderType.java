package net.anax.http;

public enum HTTPHeaderType {
    Host("Host"),
    User_Agent("User-Agent"),
    Accept("Accept"),
    Authorization("Authorization"),
    Cookie("Cookie"),
    Content_type("Content-Type"),
    Content_Length("Content-Length"),
    Accept_language("Accept-Language"),
    Accept_Encoding("Accept-Encoding"),
    Refer("Refer"),
    Origin("Origin");

    String name;
    HTTPHeaderType(String name){
        this.name = name;
    }

}
