package net.anax.thread;

import net.anax.cryptography.KeyManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ListenerThread extends Thread{
    ServerSocket serverSocket;
    KeyManager keyManager;
    public ListenerThread(ServerSocket serverSocket, KeyManager keyManager){
        this.keyManager = keyManager;
        this.serverSocket = serverSocket;
    }

    @Override
    public void run() {
        while(!serverSocket.isClosed() && serverSocket.isBound()){
            try {
                Socket socket = serverSocket.accept();
                WorkerThread workerThread = new WorkerThread(socket, keyManager);
                workerThread.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
