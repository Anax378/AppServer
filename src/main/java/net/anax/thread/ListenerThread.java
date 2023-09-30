package net.anax.thread;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ListenerThread extends Thread{
    ServerSocket serverSocket;
    public ListenerThread(ServerSocket serverSocket){
        this.serverSocket = serverSocket;
    }

    @Override
    public void run() {
        while(!serverSocket.isClosed() && serverSocket.isBound()){
            try {
                Socket socket = serverSocket.accept();
                WorkerThread workerThread = new WorkerThread(socket);
                workerThread.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
