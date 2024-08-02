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

    }

}
