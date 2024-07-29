package net.anax.cryptography;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

public class AESKey {
    byte[] keyData;
    byte[] iv;
    Key key;
    public AESKey(byte[] keyData,byte[] iv){
        this.keyData = keyData;
        this.iv = iv;
        key = new SecretKeySpec(keyData, "AES");
    }

    public Key getkey(){
        return key;
    }

    public byte[] getIv(){
        return iv;
    }
}
