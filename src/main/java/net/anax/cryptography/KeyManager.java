package net.anax.cryptography;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

public class KeyManager {
    private static final int DerrivedKeyLengthBits = 256;
    private static final int ITERATIONS = 10000;

    private HMACSHA256Key hmacsha256Key;
    private RSAPrivateKey rsaPrivateKey;
    public KeyManager(String HMACSHA256KeyPassword, String RSAPrivateKeyPassword) throws IOException, ParseException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, InvalidKeySpecException, BadPaddingException, InvalidKeyException {
        this.hmacsha256Key = new HMACSHA256Key(decryptBase64StringAES(HMACSHA256KeyPassword, getBase64EncryptedTokenKey()));
        this.rsaPrivateKey = new RSAPrivateKey(decryptBase64StringAES(RSAPrivateKeyPassword, getBase64EncryptedTrafficPrivateKey()));
    }
    private byte[] decryptBase64StringAES(String password, String base64String) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] bytes = Base64.getDecoder().decode(base64String);

        SecretKey derivedKey = genKeyFromPassword(password);
        SecretKeySpec aesSecretKeySpec = new SecretKeySpec(derivedKey.getEncoded(), "AES");
        Cipher decryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        byte[] iv = new byte[decryptCipher.getBlockSize()];
        byte[] cipher = new byte[bytes.length- decryptCipher.getBlockSize()];
        System.arraycopy(bytes, 0, iv, 0, iv.length);
        System.arraycopy(bytes, decryptCipher.getBlockSize(), cipher, 0, cipher.length);

        IvParameterSpec ivParamsDecoded = new IvParameterSpec(iv);
        decryptCipher.init(Cipher.DECRYPT_MODE, aesSecretKeySpec, ivParamsDecoded);
        byte[] decryptedBytes = decryptCipher.doFinal(cipher);

        return decryptedBytes;
    }

    private SecretKey genKeyFromPassword(String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] salt = generateSalt(password);
        KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, DerrivedKeyLengthBits);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        SecretKey secretKey = keyFactory.generateSecret(keySpec);
        SecretKey derivedKey = new SecretKeySpec(secretKey.getEncoded(), "AES");
        return derivedKey;
    }

    private byte[] generateSalt(String password) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(password.getBytes(StandardCharsets.UTF_8));
    }

    private String getBase64EncryptedTrafficPrivateKey() throws IOException, ParseException {
        JSONObject privateConfig = (JSONObject) new JSONParser().parse(new FileReader("privateConfig.json"));
        return (String) privateConfig.get("encryptedTrafficRSAPrivateKey");
    }
    private String getBase64EncryptedTokenKey() throws IOException, ParseException {
        JSONObject privateConfig = (JSONObject) new JSONParser().parse(new FileReader("privateConfig.json"));
        return (String)privateConfig.get("encryptedTokenHMACSHA256Key");
    }

    public HMACSHA256Key getHMACSHA256TokenKey() {
        return hmacsha256Key;
    }
    public RSAPrivateKey getRSAPrivateTrafficKey(){
        return this.rsaPrivateKey;
    }
}
