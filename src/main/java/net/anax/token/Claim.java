package net.anax.token;

public enum Claim {
    Subject("sub"), //the subject of the token, typically the user the token was issued to.
    ExpirationTimestamp("exp"), //timestamp when the token expires
    Issuer("iss"), //who issued the token
    Audience("aud"), //who this token is meant for
    NotBefore("nbf"), //only valid after this time
    IssuedAt("iat"), //timestamp of creation of token
    Identifier("jti"), //a special identifier for the token
    ;

    String key;
    Claim(String name){
        this.key = name;
    }
}
