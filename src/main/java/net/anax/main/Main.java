package net.anax.main;

import net.anax.thread.ListenerThread;

import java.io.IOException;
import java.net.ServerSocket;

public class Main {

    private static final int port = 8080;
    public static void main(String[] args) {
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