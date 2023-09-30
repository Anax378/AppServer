package net.anax.main;

import net.anax.thread.ListenerThread;
import net.anax.thread.WorkerThread;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;

public class Main {

    private static int port;
    public static void main(String[] args) throws IOException, ParseException {

        JSONObject config = (JSONObject)new JSONParser().parse(new FileReader("config.json"));
        port = ((Long)config.get("port")).intValue();
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            ListenerThread listenerThread = new ListenerThread(serverSocket);
            listenerThread.start();
            System.out.println("Server started at port " + port);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}