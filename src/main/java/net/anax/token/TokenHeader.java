package net.anax.token;

public enum TokenHeader {
    Algorithm("alg"),
    Type("typ"),
    ;

    String headerKey;
    TokenHeader(String headerKey){
        this.headerKey = headerKey;
    }

}
