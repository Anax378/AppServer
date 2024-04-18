package net.anax.main;

import net.anax.cryptography.KeyManager;
import net.anax.endpoint.EndpointFailedException;
import net.anax.http.HTTPParsingException;
import net.anax.logging.Logger;
import net.anax.thread.ListenerThread;
import net.anax.thread.WorkerThread;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Objects;
import java.util.Scanner;

public class Main {

    private static int port;
    public static KeyManager keyManager;
    public static void main(String[] args) throws IOException, ParseException {
        EndpointFailedException.doPrintStacktrace = true;
        HTTPParsingException.doPrintStacktrace = true;
        System.out.print("Hmac Token Key Decryption password: ");
        Scanner scanner = new Scanner(System.in);
        String password = scanner.nextLine();
        try {
            keyManager = new KeyManager(password);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        JSONObject config = (JSONObject)new JSONParser().parse(new FileReader("config.json"));
        port = ((Long)config.get("port")).intValue();
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
