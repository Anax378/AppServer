package net.anax.main;

import com.mysql.cj.util.Base64Decoder;
import com.mysql.cj.xdevapi.JsonParser;
import net.anax.cryptography.KeyManager;
import net.anax.endpoint.EndpointFailedException;
import net.anax.http.HTTPParsingException;
import net.anax.logging.Logger;
import net.anax.thread.ListenerThread;
import net.anax.thread.WorkerThread;
import net.anax.util.JsonUtilities;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.ServerSocket;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.Scanner;

public class Main {

    private static int port;
    public static KeyManager keyManager;
    public static boolean printExceptionsOnInstantiation = false;

    public static void main(String[] args) throws IOException, ParseException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        EndpointFailedException.doPrintStacktrace = printExceptionsOnInstantiation;
        HTTPParsingException.doPrintStacktrace = printExceptionsOnInstantiation;

        JSONObject config = (JSONObject)new JSONParser().parse(new FileReader("config.json"));
        port = ((Long)config.get("port")).intValue();
        String logPath = JsonUtilities.extractString(config, "log_path", new RuntimeException("could not find log_path in config"));

        Logger.printStreams = new PrintStream[]{System.out, new PrintStream(new FileOutputStream(logPath, true))};

        System.out.print("Hmac Token Key Decryption password: ");
        Scanner scanner = new Scanner(System.in);
        String password = scanner.nextLine();

        try {
            keyManager = new KeyManager(password, "password"); //TODO: ask user for RSA key password
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            ListenerThread listenerThread = new ListenerThread(serverSocket, keyManager);
            listenerThread.start();
            Logger.log("Server started at port " + port, 0);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println(Arrays.toString(keyManager.getRSAPrivateTrafficKey().getKeyData()));

        PublicKey pukey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(new byte[]{48, -126, 2, 34, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 3, -126, 2, 15, 0, 48, -126, 2, 10, 2, -126, 2, 1, 0, -9, -69, -61, -38, -124, 35, -52, 57, 126, 79, 124, -64, 85, 18, 121, -90, 3, 15, -53, -83, -114, 78, 23, -36, -99, -40, 62, -101, -14, -118, 17, 85, 26, -47, 58, 30, -98, -57, -34, 40, -115, 83, 117, -104, 17, -25, 31, 101, -124, 114, -17, -18, 43, -107, -39, 23, -44, -19, 88, -13, 88, -31, 56, 19, 2, 114, 123, 100, 19, -15, 50, -64, 32, 64, 105, -125, -52, -125, -116, -48, 126, -128, 8, -50, -38, 85, 115, 91, -86, 75, -32, -109, -59, -109, -106, -73, -28, -103, 18, -70, -121, -102, 43, -34, -18, -82, 121, 70, 127, -123, 13, -93, 1, -119, -115, 53, 104, 38, -120, 52, 100, 77, -19, 84, 111, -113, 22, 5, 43, 90, -68, -49, 19, 68, -61, -116, 126, -99, 5, 4, 85, -114, 92, -36, -28, -121, -126, -56, -85, 105, 95, -52, -112, 73, -16, 76, 87, 80, -9, 51, 83, 59, -47, -66, 83, 114, 114, 108, -4, -92, -113, -100, -127, 47, -56, 77, 8, 117, -75, 112, -48, 89, -108, 60, -104, 57, -3, -84, 38, -62, 98, -107, -44, 121, 51, -10, 16, 100, 122, 105, 125, 8, 35, 90, 78, -35, 0, -94, 126, -120, -62, -52, 11, 60, 4, 18, -70, -2, -125, 101, 58, 60, -24, -121, 26, -60, -33, 10, -54, 54, 102, 75, 40, 114, -60, -98, -13, -109, 8, -80, 109, -120, 90, -33, 48, 88, 76, 84, -49, -63, -6, 21, -120, -124, 52, 116, -100, -48, -33, 88, -91, -109, -94, 15, 73, -63, -85, 67, -88, 32, -69, 91, -123, -117, 4, -97, -96, 38, 88, -78, -126, -47, -41, -126, 55, -41, 123, 33, -41, -66, 82, 104, 14, 15, -46, 84, -11, -11, 103, 86, 69, 116, -24, 122, -124, 115, -84, -47, 51, 24, -75, 66, 30, -21, 17, 99, 56, -71, -124, 12, -3, -57, 45, 12, 17, -3, -12, -74, -82, -27, -10, -25, -40, 83, -101, 63, -111, -64, -24, 94, 15, -22, 7, -7, 58, -106, 104, 25, -98, 81, 35, 2, 85, 33, -82, 120, -94, -55, 58, -114, -66, 40, 12, -61, -23, -26, -3, -38, 0, 63, -11, -108, 71, 68, -121, -14, -109, -78, 49, -45, -10, -114, -38, 15, -36, -122, 3, -85, -80, -9, -57, 15, 16, 114, 91, -113, 117, 15, 108, -102, 26, 96, -4, -121, 15, -63, -69, -111, 97, -97, -93, -57, 73, -122, -11, 112, 68, -22, 5, -28, 32, -68, -56, 47, -18, -80, -49, 10, -108, 98, 0, 69, -20, -91, -46, -43, 85, 79, 27, -111, -114, 78, -50, -39, 14, -76, 31, -110, 75, -34, 94, 70, 13, -69, 25, -37, -117, 66, -39, -12, -99, -76, -112, 109, -91, 51, 13, 15, -9, -12, 51, 114, -46, -108, -71, 124, -3, 14, 103, -52, 77, 67, 78, 17, -108, 0, 42, 93, -71, -1, 115, 27, -116, 97, 17, 74, 121, 54, -56, -42, -61, -66, 31, 16, 100, -123, 10, -34, -99, -34, 79, -1, 2, 3, 1, 0, 1}));
        PrivateKey prkey = keyManager.getRSAPrivateTrafficKey().getKey();

        byte[] payload = new byte[]{123, 34, 114, 101, 113, 117, 101, 115, 116, 34, 58, 34, 82, 48, 86, 85, 73, 67, 56, 118, 100, 88, 78, 108, 99, 105, 57, 115, 98, 50, 100, 112, 98, 105, 66, 73, 86, 70, 82, 81, 76, 122, 69, 117, 77, 81, 48, 75, 81, 88, 86, 48, 97, 71, 57, 121, 97, 88, 112, 104, 100, 71, 108, 118, 98, 106, 111, 103, 98, 109, 57, 117, 90, 81, 48, 75, 81, 50, 57, 117, 100, 71, 86, 117, 100, 67, 49, 77, 92, 110, 90, 87, 53, 110, 100, 71, 103, 54, 73, 68, 81, 49, 68, 81, 111, 78, 67, 110, 115, 105, 99, 71, 70, 122, 99, 51, 100, 118, 99, 109, 81, 105, 79, 105, 74, 119, 89, 88, 78, 122, 100, 50, 57, 121, 90, 67, 73, 115, 73, 110, 86, 122, 90, 88, 74, 117, 89, 87, 49, 108, 73, 106, 111, 105, 100, 88, 78, 108, 99, 109, 53, 104, 98, 87, 85, 105, 92, 110, 102, 81, 61, 61, 92, 110, 34, 44, 34, 107, 101, 121, 34, 58, 34, 51, 67, 114, 71, 109, 85, 108, 55, 48, 106, 50, 116, 106, 78, 72, 65, 74, 75, 114, 52, 69, 119, 61, 61, 92, 110, 34, 125};

        Cipher ce = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        ce.init(Cipher.ENCRYPT_MODE, pukey);
        byte[] encryptedPayload = ce.doFinal(payload);

        System.out.println("encryptedPayload: " + Arrays.toString(encryptedPayload));

        Cipher cd = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cd.init(Cipher.DECRYPT_MODE, prkey);
        byte[] decryptedPayload = cd.doFinal(encryptedPayload);

        System.out.println("decryptedPayload: " + Arrays.toString(decryptedPayload));
        System.out.println("payload string: " + new String(decryptedPayload));




    }

}
