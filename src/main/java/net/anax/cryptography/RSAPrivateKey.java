package net.anax.cryptography;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class RSAPrivateKey {
    private byte[] keyData;
    PrivateKey key;
    public RSAPrivateKey(byte[] keyData) throws NoSuchAlgorithmException, InvalidKeySpecException {
        this.keyData = keyData;
        KeyFactory kf = KeyFactory.getInstance("RSA");
        this.key = kf.generatePrivate(new PKCS8EncodedKeySpec(keyData));
    }

    public byte[] getKeyData(){
        return keyData;
    }

    public PrivateKey getKey() {
        return key;
    }
}
