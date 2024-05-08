package net.anax.cryptography;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

public class AESKey {
    byte[] keyData;
    Key key;
    public AESKey(byte[] keyData){
        this.keyData = keyData;
        key = new SecretKeySpec(keyData, "AES");
    }

    public Key getkey(){
        return key;
    }
}
