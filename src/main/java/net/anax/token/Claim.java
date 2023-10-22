package net.anax.token;

public enum Claim {
    Subject("sub"),
    ExpirationTimestamp("exp"),
    Issuer("iss"),
    Audience("aud"),
    NotBefore("nbf"),
    IssuedAt("iat"),
    Identifier("jti"),
    ;

    String key;
    Claim(String name){
        this.key = name;
    }
}
