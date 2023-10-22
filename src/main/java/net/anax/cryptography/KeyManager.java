package net.anax.cryptography;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
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
    private final String password;

    private byte[] HMACSHA256TokenKey = null;

    public KeyManager(String password) throws IOException, ParseException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, InvalidKeySpecException, BadPaddingException, InvalidKeyException {
        this.password = new String(password);
        this.HMACSHA256TokenKey = decryptBase64StringAES(password, getBase64EncryptedTokenKey());
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

    private String getBase64EncryptedTokenKey() throws IOException, ParseException {
        JSONObject privateConfig = (JSONObject) new JSONParser().parse(new FileReader("privateConfig.json"));
        return (String)privateConfig.get("encryptedTokenHMACSHA256Key");
    }

    public byte[] getHMACSHA256TokenKey() {
        return HMACSHA256TokenKey;
    }
}
